package com.noughmad.slashdotcomments;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class SlashdotContent {
	
	public static class Comment implements Serializable {
		private static final long serialVersionUID = 1L;
		
		
		public String author;
		public String title;
		public String content;
		public String score;
		public long id;
		int level;
	}
	
	public static void refreshStories(Context context, int page) {
		if (page == 0) {
			refreshStories(context, "http://slashdot.org");
		} else {
			refreshStories(context, "http://slashdot.org/?page=" + page);
		}
	}
	
	public static void refreshStories(Context context, String source) {
		URL url;
		try {
			url = new URL(source);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return;
		}
		
		
		Document doc;
		try {
			doc = Jsoup.parse(url, 30000);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		Uri storiesUri = Uri.withAppendedPath(SlashdotProvider.BASE_URI, SlashdotProvider.STORIES_TABLE_NAME);
		
		Elements articles = doc.select("article[data-fhid]");
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

			Log.i("GetStoriesTask", "Parsed story " + id);
			Log.v("GetStoriesTask", values.getAsString(SlashdotProvider.STORY_TITLE));
			
			values.put(SlashdotProvider.STORY_COMMENT_COUNT, Integer.parseInt(article.select("span.commentcnt-" + id).first().html()));
			
			Uri uri = ContentUris.withAppendedId(storiesUri, id);
			Cursor existing = context.getContentResolver().query(uri, new String[] {SlashdotProvider.ID}, null, null, null);
			if (existing.moveToFirst()) {
				context.getContentResolver().update(uri, values, null, null);
			} else {
				context.getContentResolver().insert(uri, values);
			}
		}
	}
	
	private static void parseComment(Context context, Uri baseUri, Element tree, int level, String parentTitle) {
		if (tree.hasClass("hidden")) {
			return;
		}
		
		Element comment = tree.select("div.cw").first();
		
		ContentValues values = new ContentValues();
		values.put(SlashdotProvider.COMMENT_LEVEL, level);
		
		long id = Long.parseLong(comment.id().substring(8));
		values.put(SlashdotProvider.ID, id);
				
		String title = tree.select("a#comment_link_" + id).first().html();
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
		
		values.put(SlashdotProvider.COMMENT_AUTHOR, author);
		
		values.put(SlashdotProvider.COMMENT_CONTENT, comment.select("div#comment_body_" + id).first().html());
		values.put(SlashdotProvider.COMMENT_SCORE, comment.select("span.score").first().html());

		Uri uri = ContentUris.withAppendedId(baseUri, id);
		Cursor existing = context.getContentResolver().query(uri, new String[] {SlashdotProvider.ID}, null, null, null);
		if (existing.moveToFirst()) {
			context.getContentResolver().update(uri, values, null, null);
		} else {
			context.getContentResolver().insert(uri, values);
		}
		
		for (Element subTree : tree.select("ul#commtree_" + id + " > li.comment")) {
			parseComment(context, baseUri, subTree, level + 1, title);
		}
	}
	
	public static void refreshComments(Context context, long storyId, String source) {
		Uri storyUri = ContentUris.withAppendedId(Uri.withAppendedPath(SlashdotProvider.BASE_URI, SlashdotProvider.STORIES_TABLE_NAME), storyId);

		if (source == null) {
			Cursor story = context.getContentResolver().query(storyUri, new String[] {SlashdotProvider.STORY_URL}, null, null, null);
			
			if (story.moveToFirst()) {
				source = story.getString(0);
			} else {
				return;
			}
		}
		
		URL url;
		try {
			url = new URL(source);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return;
		}
		
		Document doc;
		try {
			doc = Jsoup.parse(url, 30 * 1000);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		Uri baseUri = Uri.withAppendedPath(storyUri, SlashdotProvider.COMMENTS_TABLE_NAME);
		
		for (Element tree : doc.select("ul#commentlisting > li.comment")) {
			parseComment(context, baseUri, tree, 0, null);			
		}
	};
}
