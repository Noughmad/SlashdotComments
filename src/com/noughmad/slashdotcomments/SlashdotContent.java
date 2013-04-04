package com.noughmad.slashdotcomments;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		
		Uri storiesUrl = Uri.withAppendedPath(SlashdotProvider.BASE_URI, SlashdotProvider.STORIES_TABLE_NAME);
		
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
			
			Uri uri = ContentUris.withAppendedId(storiesUrl, id);
			Cursor existing = context.getContentResolver().query(uri, new String[] {SlashdotProvider.ID}, null, null, null);
			if (existing.moveToFirst()) {
				context.getContentResolver().update(uri, values, null, null);
			} else {
				context.getContentResolver().insert(uri, values);
			}
		}
	}
	
	public static Story findStoryById(long id) {
		for (Story story : stories) {
			if (story.id == id) {
				return story;
			}
		}
		return null;
	}
	
	public static boolean areStoriesLoaded() {
		return stories != null && !stories.isEmpty();
	}
	
	public static Map<Long, List<Comment>> comments = new HashMap<Long, List<Comment>>();
	
	private static void parseComment(List<Comment> list, Element tree, int level, Comment parent) {
		if (tree.hasClass("hidden")) {
			return;
		}
		
		Element comment = tree.select("div.cw").first();
		
		Comment c = new Comment();
		c.level = level;
		c.id = Long.parseLong(comment.id().substring(8));
				
		c.title = tree.select("a#comment_link_" + c.id).first().html();
		if (c.title.trim().equals("Re:") && parent != null) {
			if (parent.title.startsWith("Re:")) {
				c.title = parent.title;
			} else {
				c.title = "Re:" + parent.title; 
			}
		}
		
		Elements authorLinks = comment.select("span.by a");
		if (!authorLinks.isEmpty()) {
			c.author = authorLinks.first().html();
		} else {
			c.author = comment.select("span.by").first().html();
		}
		c.content = comment.select("div#comment_body_" + c.id).first().html();
		c.score = comment.select("span.score").first().html();
		
		list.add(c);
		
		for (Element subTree : tree.select("ul#commtree_" + c.id + " > li.comment")) {
			parseComment(list, subTree, level + 1, c);
		}
	}
	
	public static List<Comment> refreshComments(long storyId) throws IOException {
		Story story = findStoryById(storyId);
		
		List<Comment> list = new ArrayList<Comment>();
		if (story == null) {
			return list;
		}
		
		URL url = new URL(story.url);
		Document doc = Jsoup.parse(url, 30 * 1000);
		
		
		for (Element tree : doc.select("ul#commentlisting > li.comment")) {
			parseComment(list, tree, 0, null);			
		}
		
		return list;
	}
	
	public static List<Comment> getComments(long storyId) {
		if (areCommentsLoaded(storyId)) {
			return comments.get(storyId);
		} else {
			return null;
		}
	}
	
	public static boolean areCommentsLoaded(long storyId) {
		return comments != null && comments.containsKey(storyId);
	}
	
	public static void saveToCache(Context context) throws IOException {
		
		if (context == null) {
			return;
		}
		
		FileOutputStream stream = context.openFileOutput("stories", Context.MODE_PRIVATE);
		ObjectOutputStream objectStream = new ObjectOutputStream(stream);

		objectStream.writeObject(stories);
		objectStream.writeObject(comments);
		
		objectStream.close();
		stream.close();
	}
	
	public static void loadFromCache(Context context) {
		if (areStoriesLoaded()) {
			return;
		}
		
		try {
			FileInputStream stream = context.openFileInput("stories");
			ObjectInputStream objectStream = new ObjectInputStream(stream);
			
			stories = (List<Story>) objectStream.readObject();
			comments = (Map<Long, List<Comment>>) objectStream.readObject();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (ClassCastException e) {
			e.printStackTrace();
			return;
		}
	}
}
