package com.noughmad.slashdotcomments;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Context;
import android.util.Log;

public class SlashdotContent {
	
	public static class Story implements Serializable {
		private static final long serialVersionUID = 1L;
		
		public String title;
		public String summary;
		public long id;
		public String url;
		public int commentCount;
	}
	
	public static class Comment implements Serializable {
		private static final long serialVersionUID = 1L;
		
		
		public String author;
		public String title;
		public String content;
		public String score;
		public long id;
		int level;
	}
	
	public static List<Story> stories = new ArrayList<Story>();
	
	public static void refreshStories() throws IOException {
		URL url;
		
		stories = new ArrayList<Story>();
		
		url = new URL("http://slashdot.org");
		Document doc = Jsoup.parse(url, 30000);

		Elements articles = doc.select("article[data-fhid]");
		for (Element article : articles) {
			Elements titles = article.select("header h2.story");
			if (titles.isEmpty()) {
				continue;
			}
			
			SlashdotContent.Story story = new SlashdotContent.Story();
			story.id = Long.parseLong(article.attr("data-fhid"));
			
			Element title = titles.first();
			Element link = title.select("a[href]").first(); 
			story.title = link.html();

			story.url = link.attr("href");
			if (!story.url.startsWith("http")) {
				story.url = "http:" + story.url;
			}
			
			story.summary = article.select("div#text-" + story.id).first().html();

			Log.i("GetStoriesTask", "Parsed story " + story.id);
			Log.i("GetStoriesTask", story.title);
			
			story.commentCount = Integer.parseInt(article.select("span.commentcnt-" + story.id).first().html());
			
			stories.add(story);
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
	
	public static void refreshComments(long storyId) throws IOException {
		Story story = findStoryById(storyId);
		
		URL url = new URL(story.url);
		Document doc = Jsoup.parse(url, 30 * 1000);
		
		List<Comment> list = new ArrayList<Comment>();
		
		for (Element tree : doc.select("ul#commentlisting > li.comment")) {
			parseComment(list, tree, 0, null);			
		}
		
		SlashdotContent.comments.put(storyId, list);
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
