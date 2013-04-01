package com.noughmad.slashdotcomments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.ListFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.noughmad.slashdotcomments.SlashdotContent.Comment;
import com.noughmad.slashdotcomments.SlashdotContent.Story;

/**
 * A fragment representing a single Story detail screen. This fragment is either
 * contained in a {@link StoryListActivity} in two-pane mode (on tablets) or a
 * {@link StoryDetailActivity} on handsets.
 */
public class StoryDetailFragment extends ListFragment {
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_ITEM_ID = "item_id";

	/**
	 * The dummy content this fragment is presenting.
	 */
	private Story mStory;
	
	private class CommentsAdapter extends BaseAdapter {
		
		private List<Comment> mComments;
		
		CommentsAdapter(List<Comment> comments) {
			super();
			mComments = comments;
		}

		@Override
		public int getCount() {
			return mComments.size();
		}

		@Override
		public Object getItem(int position) {
			return mComments.get(position);
		}

		@Override
		public long getItemId(int position) {
			return mComments.get(position).id;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				view = inflater.inflate(android.R.layout.two_line_list_item, parent, false);
			}
			
			Comment comment = mComments.get(position);
			TextView title = (TextView)view.findViewById(android.R.id.text1);
			title.setText(Html.fromHtml(comment.title));
			
			TextView content = (TextView)view.findViewById(android.R.id.text2);
			content.setText(Html.fromHtml(comment.content));
			
			int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16 * comment.level, getResources().getDisplayMetrics());
			view.setPadding(px, 0, 0, 0);
			
			return view;
		}
		
	};

	private class GetCommentsTask extends AsyncTask<Long, Void, List<Comment> > {

		@Override
		protected List<Comment> doInBackground(Long... params) {
			try {
				SlashdotContent.refreshComments(params[0]);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				SlashdotContent.saveToCache(getActivity());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return SlashdotContent.getComments(params[0]);
		}

		@Override
		protected void onPostExecute(List<Comment> result) {
			Log.i("GetCommentsTask", String.format("Found %d comments", result.size()) );
			setListAdapter(new CommentsAdapter(result));
		}
	};

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public StoryDetailFragment() {
	}
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments().containsKey(ARG_ITEM_ID)) {
			// Load the dummy content specified by the fragment
			// arguments. In a real-world scenario, use a Loader
			// to load content from a content provider.

			
			mStory = SlashdotContent.findStoryById(getArguments().getLong(ARG_ITEM_ID));
		}
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (mStory != null) {
			
			if (this.getListAdapter() != null) {
				this.setListAdapter(null);
			}
			
			TextView title = new TextView(getActivity());
			title.setTextAppearance(getActivity(), android.R.style.TextAppearance_Large);
			title.setText(Html.fromHtml(mStory.title));
			getListView().addHeaderView(title);
			
			TextView summary = new TextView(getActivity());
			summary.setText(Html.fromHtml(mStory.summary));
			getListView().addHeaderView(summary);
			
			if (SlashdotContent.areCommentsLoaded(mStory.id)) {
				setListAdapter(new CommentsAdapter(SlashdotContent.getComments(mStory.id)));
			} else {
				setListAdapter(new CommentsAdapter(new ArrayList<Comment>()));
			}
				
			(new GetCommentsTask()).execute(mStory.id);
		}
	}
	
	
}
