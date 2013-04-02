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
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

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
				view = inflater.inflate(R.layout.item_comment, parent, false);
			}
			
			Comment comment = mComments.get(position);
			TextView title = (TextView)view.findViewById(R.id.comment_title);
			title.setText(Html.fromHtml(comment.title) + " " + Html.fromHtml(comment.score));
			
			TextView content = (TextView)view.findViewById(R.id.comment_text);
			content.setText(Html.fromHtml(comment.content));
			Log.d("CommentsAdapter", "Content: " + comment.content);
			
			int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4 + 8 * comment.level, getResources().getDisplayMetrics());
			view.setPadding(px, 0, 0, 4);
			
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
		} else {
			Log.wtf("StoryDetailFragment", "No story found for " + getArguments().getLong(ARG_ITEM_ID));
		}
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (mStory != null) {
			
			if (this.getListAdapter() != null) {
				this.setListAdapter(null);
			}
			
			getListView().setDivider(null);
			getListView().setDividerHeight(0);
			
			View header = getActivity().getLayoutInflater().inflate(R.layout.story_header, getListView(), false);
			
			TextView title = (TextView) header.findViewById(R.id.story_title);
			title.setText(Html.fromHtml(mStory.title));
			
			final TextView summary = (TextView) header.findViewById(R.id.story_summary);
			summary.setText(Html.fromHtml(mStory.summary));
			getListView().addHeaderView(header);
			
			ToggleButton button = (ToggleButton) header.findViewById(R.id.toggle);
			button.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					summary.setVisibility(isChecked ? View.VISIBLE : View.GONE);
				}});
			button.setChecked(true);
			
			if (SlashdotContent.areCommentsLoaded(mStory.id)) {
				setListAdapter(new CommentsAdapter(SlashdotContent.getComments(mStory.id)));
			} else {
				setListAdapter(new CommentsAdapter(new ArrayList<Comment>()));
			}
				
			(new GetCommentsTask()).execute(mStory.id);
		}
	}
	
	
}
