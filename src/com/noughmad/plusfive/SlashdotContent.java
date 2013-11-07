package com.noughmad.plusfive;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlashdotContent {
	
	public static SimpleDateFormat sDateFormat = new SimpleDateFormat("EEEE MMM dd, yyyy @hh:mma", Locale.US);
	public static String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:20.0) Gecko/20100101 Firefox/20.0";

    public static boolean isLoggedIn(Context context) {
        return context.getSharedPreferences("cookie", Context.MODE_PRIVATE).contains("user");
    }
	
	public static boolean refreshStories(Context context, Calendar date) {
		if (date == null) {
			date = Calendar.getInstance();
		}
		
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
		format.setCalendar(date);
		String source = "http://classic.slashdot.org/?issue=" + format.format(date.getTime());

		return refreshStories(context, source);
	}
	
	public static boolean refreshStories(Context context, String source) {
		Log.i("RefreshStories", "Refreshing from " + source);

        if (context == null) {
            Log.w("RefreshStories", "Trying to refresh without a Context");
            return false;
        }

		Document doc;
		try {
			doc = Jsoup.connect(source)
                    .userAgent(USER_AGENT)
          //          .cookie("user", context.getSharedPreferences("cookie", Context.MODE_PRIVATE).getString("user", ""))
                    .get();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		Uri storiesUri = Uri.withAppendedPath(SlashdotProvider.BASE_URI, SlashdotProvider.STORIES_TABLE_NAME);
				
		Elements articles = doc.select("article[data-fhid]");
		Log.d("RefreshStories", "Found " + articles.size() + " articles");
		
		for (Element article : articles) {
			Elements titles = article.select("header h2.story");
			if (titles.isEmpty()) {
				continue;
			}
			
			long id = Long.parseLong(article.attr("data-fhid"));
			
			ContentValues values = new ContentValues();
			
			Element title = titles.first();
			Element link = title.select("a[href]").first();

			values.put(SlashdotProvider.STORY_TITLE, link.html());

			String storyUrl = link.attr("href");
			if (!storyUrl.startsWith("http")) {
				storyUrl = "http:" + storyUrl;
			}
			values.put(SlashdotProvider.STORY_URL, storyUrl);
			values.put(SlashdotProvider.STORY_SUMMARY, article.select("div#text-" + id).first().html());
			values.put(SlashdotProvider.STORY_COMMENT_COUNT, Integer.parseInt(article.select("span.commentcnt-" + id).first().html()));
			
			String date = article.select("time").html();
            if (date.startsWith("on") || date.startsWith("On")) {
                date = date.substring(3);
            }

            int timeIndex = 0;
            if (date.contains("@")) {
    			timeIndex = date.indexOf('@');
            } else {
                Pattern p = Pattern.compile("\\d{2}:\\d{2}");
                Matcher match = p.matcher(date);
                if (match.find()) {
                    timeIndex = match.start();
                } else {
                    timeIndex = -1;
                }
            }

            if (timeIndex < 0 || timeIndex >= date.length()) {
                Log.e("RefreshStories", "Unable to parse date " + date);
                continue;
            }

			values.put(SlashdotProvider.STORY_DATE, date.substring(0, timeIndex - 1));
			
			try {
				values.put(SlashdotProvider.STORY_TIME, sDateFormat.parse(date).getTime());
			} catch (ParseException e) {
				e.printStackTrace();
			}
						
			Uri uri = ContentUris.withAppendedId(storiesUri, id);
			Cursor existing = context.getContentResolver().query(uri, new String[] {SlashdotProvider.ID}, null, null, null);
			if (existing.moveToFirst()) {
				context.getContentResolver().update(uri, values, null, null);
			} else {
				context.getContentResolver().insert(uri, values);
			}
			existing.close();
		}

        Elements quote = doc.select("section.bq blockquote.msg p");
        if (!quote.isEmpty())
        {
            Uri uri = Uri.withAppendedPath(SlashdotProvider.BASE_URI, SlashdotProvider.QUOTES_TABLE_NAME);

            Cursor existing = context.getContentResolver().query(uri, new String[] {SlashdotProvider.ID}, SlashdotProvider.QUOTE_CONTENT + " = ?", new String[] {quote.first().html()}, null);
            if (!existing.moveToFirst())
            {
                ContentValues values = new ContentValues();
                values.put(SlashdotProvider.QUOTE_DATE, Calendar.getInstance().getTimeInMillis());
                values.put(SlashdotProvider.QUOTE_CONTENT, quote.first().html());
                context.getContentResolver().insert(uri, values);
            }
            existing.close();
        }
		
		return true;
	}
	
	private static void parseComment(Context context, Uri baseUri, Element tree, int level, String parentTitle) {
        long id = Long.parseLong(tree.id().substring(5));
        String title = "";
		if (!tree.hasClass("hidden")) {

            Element comment = tree.select("div.cw").first();

            ContentValues values = new ContentValues();
            values.put(SlashdotProvider.COMMENT_LEVEL, level);

            values.put(SlashdotProvider.ID, id);

            Element title_el = tree.select("a#comment_link_" + id).first();
            if (title_el == null) {
                title_el = tree.select("a[name=" + id + "]").first();
            }

            title = title_el.html();
            if (title.trim().equals("Re:") && parentTitle != null) {
                if (parentTitle.startsWith("Re:")) {
                    title = parentTitle;
                } else {
                    title = "Re:" + parentTitle;
                }
            }
            values.put(SlashdotProvider.COMMENT_TITLE, title);

            String author = null;
            Elements authorLinks = comment.select("span.by a");
            if (!authorLinks.isEmpty()) {
                author = authorLinks.first().html();
            } else {
                author = comment.select("span.by").first().html();
            }

            try {
                values.put(SlashdotProvider.COMMENT_AUTHOR, author);
                values.put(SlashdotProvider.COMMENT_CONTENT, comment.select("div#comment_body_" + id).first().html());
            } catch (NullPointerException e) {
                e.printStackTrace();
                return;
            }

            String scoreHtml = comment.select("span.score").first().html();
            values.put(SlashdotProvider.COMMENT_SCORE_TEXT, scoreHtml);
            int pos = scoreHtml.indexOf("Score:</span>");
            String score = scoreHtml.substring(pos + 13, pos + 15);
            if (!score.startsWith("-")) {
                score = score.substring(0, 1);
            }
            values.put(SlashdotProvider.COMMENT_SCORE_NUM, Integer.parseInt(score));

            Uri uri = ContentUris.withAppendedId(baseUri, id);
            Cursor existing = context.getContentResolver().query(uri, new String[] {SlashdotProvider.ID}, null, null, null);
            boolean exists = existing.getCount() > 0;
            existing.close();

            if (exists) {
                context.getContentResolver().update(uri, values, null, null);
            } else {
                context.getContentResolver().insert(uri, values);
            }
        }
		
		for (Element subTree : tree.select("ul#commtree_" + id + " > li.comment")) {
			parseComment(context, baseUri, subTree, level + 1, title);
		}
	}
	
	public static void refreshComments(Context context, long storyId, String source) {
        if (context == null) {
            Log.w("RefreshComments", "Trying to refresh comments without a Context");
            return;
        }


        Uri storyUri = ContentUris.withAppendedId(Uri.withAppendedPath(SlashdotProvider.BASE_URI, SlashdotProvider.STORIES_TABLE_NAME), storyId);
		
		if (source == null) {
			Cursor story = context.getContentResolver().query(storyUri, new String[] {SlashdotProvider.STORY_URL}, null, null, null);
			
			if (story.moveToFirst()) {
				source = story.getString(0);
				story.close();
			} else {
				story.close();
				return;
			}
		}
		
		Log.i("RefreshComments", "Refreshing from " + source);

		Document doc;
		try {
            Connection connection = Jsoup.connect(source).userAgent(USER_AGENT);
            if (context.getSharedPreferences("cookie", Context.MODE_PRIVATE).contains("user")) {
                connection.cookie("user", context.getSharedPreferences("cookie", Context.MODE_PRIVATE).getString("user", ""));
            }
            doc = connection.get();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

        Element postButton = doc.select("section#d2header a.btn").first();
        String replyUrl = postButton.attr("href");
        int sidStart = replyUrl.indexOf("sid=") + 4;
        int sidEnd = replyUrl.indexOf('&', sidStart);

        long sid = Long.parseLong(replyUrl.substring(sidStart, sidEnd));

        ContentValues values = new ContentValues();
        values.put(SlashdotProvider.STORY_SID, sid);
        context.getContentResolver().update(storyUri, values, null, null);

		Uri baseUri = Uri.withAppendedPath(storyUri, SlashdotProvider.COMMENTS_TABLE_NAME);
        // context.getContentResolver().delete(baseUri, null, null);

		for (Element tree : doc.select("ul#commentlisting > li.comment")) {
			parseComment(context, baseUri, tree, 0, null);			
		}
	};

    public static Bundle replyParameters(Context context, long storyId, long commentId) {
        String url = "";
        if (commentId > 0) {
            url = String.format("http://slashdot.org/comments.pl?sid=%d&op=Reply&threshold=1&commentsort=0&mode=thread&pid=%s", storyId, commentId);
        } else {
            url = String.format("http://slashdot.org/comments.pl?sid=%d&op=Reply&threshold=1&commentsort=0&mode=thread", storyId);
        }

        Log.d("ReplyParameters", "Requesting reply to " + url);

        Document doc;
        try {
            doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .cookie("user", context.getSharedPreferences("cookie", Context.MODE_PRIVATE).getString("user", ""))
                    .get();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        Element form = doc.select("div#comments form").first();

        String action = form.attr("action");
        if (!action.startsWith("http")) {
            action = "http:" + action;
        }

        Bundle args = new Bundle();
        args.putString("action", action);

        Log.i("ReplyParameters", action);

        Element key = form.select("input[name=formkey]").first();
        args.putString("formkey", key.val());

        Element subject = form.select("input[name=postersubj]").first();
        args.putString("postersubj", subject.val());

        return args;
    }

    public static boolean postReply(Context context, String url, Map<String, String> data) {
        try {
            data.put("op", "Submit");
            Log.d("PostReply", data.toString());

            Connection.Response response = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .cookie("user", context.getSharedPreferences("cookie", Context.MODE_PRIVATE).getString("user", ""))
                    .data(data)
                    .method(Connection.Method.POST)
                    .execute();

            Log.i("PostReply", "Return code: " + response.statusCode());

            Document doc = response.parse();
            Element error = doc.select("#comments p.error").first();

            if (error != null) {
                Log.e("PostReply", error.html());
                return false;
            } else {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
