package com.noughmad.slashdotcomments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ListFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.noughmad.slashdotcomments.SlashdotContent.Story;

/**
 * A list fragment representing a list of Stories. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon
 * selection. This helps indicate which item is currently being viewed in a
 * {@link StoryDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class StoryListFragment extends ListFragment {

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * activated item position. Only used on tablets.
	 */
	private static final String STATE_ACTIVATED_POSITION = "activated_position";

	/**
	 * The fragment's current callback object, which is notified of list item
	 * clicks.
	 */
	private Callbacks mCallbacks = sDummyCallbacks;

	/**
	 * The current activated item position. Only used on tablets.
	 */
	private int mActivatedPosition = ListView.INVALID_POSITION;

	/**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of item
	 * selections.
	 */
	public interface Callbacks {
		/**
		 * Callback for when an item has been selected.
		 */
		public void onItemSelected(long l);
		
		public boolean isTwoPane();
		
		public void onRefreshStateChanged(boolean refreshing);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onItemSelected(long id) {
		}
		public boolean isTwoPane() {
			return false;
		}
		public void onRefreshStateChanged(boolean refreshing) {
		}
	};
	
	private class StoriesAdapter extends BaseAdapter {
		
		private List<Story> mStories;
		
		StoriesAdapter(List<Story> stories) {
			super();
			mStories = stories;
		}

		@Override
		public int getCount() {
			return mStories.size();
		}

		@Override
		public Object getItem(int position) {
			return mStories.get(position);
		}

		@Override
		public long getItemId(int position) {
			return mStories.get(position).id;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				view = inflater.inflate(R.layout.item_story, parent, false);
			}
			
			Story story = mStories.get(position);
			TextView title = (TextView)view.findViewById(R.id.story_title);
			title.setText(Html.fromHtml(story.title));
			
			TextView comments = (TextView)view.findViewById(R.id.story_comments);
			comments.setText(String.format("Comments: %d", story.commentCount));
			
			return view;
		}
		
	};
		
	private class GetStoriesTask extends AsyncTask<String, Void, List<SlashdotContent.Story> > {

		@Override
		protected List<Story> doInBackground(String... params) {
			try {
				SlashdotContent.refreshStories();
			} catch (IOException e) {
				e.printStackTrace();
				this.cancel(true);
				return new ArrayList<Story>();
			}
			try {
				SlashdotContent.saveToCache(getActivity());
			} catch (IOException e) {
				e.printStackTrace();
			}
			return SlashdotContent.stories;
		}

		@Override
		protected void onPostExecute(List<Story> result) {
			setListAdapter(new StoriesAdapter(result));
			mCallbacks.onRefreshStateChanged(false);
		}
	};

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public StoryListFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// Restore the previously serialized activated item position.
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
			setActivatedPosition(savedInstanceState
					.getInt(STATE_ACTIVATED_POSITION));
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
		SlashdotContent.loadFromCache(activity);

		
		if (SlashdotContent.areStoriesLoaded()) {
			setListAdapter(new StoriesAdapter(SlashdotContent.stories));
		} else {
		}
		
		refreshStories();
	}

	@Override
	public void onDetach() {
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		super.onListItemClick(listView, view, position, id);

		// Notify the active callbacks interface (the activity, if the
		// fragment is attached to one) that an item has been selected.
		mCallbacks.onItemSelected(listView.getItemIdAtPosition(position));
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mActivatedPosition != ListView.INVALID_POSITION) {
			// Serialize and persist the activated item position.
			outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
		}
	}

	/**
	 * Turns on activate-on-click mode. When this mode is on, list items will be
	 * given the 'activated' state when touched.
	 */
	public void setActivateOnItemClick(boolean activateOnItemClick) {
		// When setting CHOICE_MODE_SINGLE, ListView will automatically
		// give items the 'activated' state when touched.
		getListView().setChoiceMode(
				activateOnItemClick ? ListView.CHOICE_MODE_SINGLE
						: ListView.CHOICE_MODE_NONE);
	}

	private void setActivatedPosition(int position) {
		if (position == ListView.INVALID_POSITION) {
			getListView().setItemChecked(mActivatedPosition, false);
		} else {
			getListView().setItemChecked(position, true);
		}

		mActivatedPosition = position;
	}
	
	public void refreshStories() {
		(new GetStoriesTask()).execute("http://slashdot.org");
		mCallbacks.onRefreshStateChanged(true);
	}
}
