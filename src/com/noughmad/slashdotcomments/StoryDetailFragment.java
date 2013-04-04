package com.noughmad.slashdotcomments;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * A fragment representing a single Story detail screen. This fragment is either
 * contained in a {@link StoryListActivity} in two-pane mode (on tablets) or a
 * {@link StoryDetailActivity} on handsets.
 */
public class StoryDetailFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_ITEM_ID = "item_id";

	/**
	 * The dummy content this fragment is presenting.
	 */
	private long mStoryId;
	
	private static String[] COMMENT_PROJECTION = new String[] {
		SlashdotProvider.COMMENT_TITLE,
		SlashdotProvider.COMMENT_SCORE,
		SlashdotProvider.COMMENT_CONTENT,
		SlashdotProvider.COMMENT_LEVEL
	};
	
	private static String[] STORY_PROJECTION = new String[] {
		SlashdotProvider.STORY_TITLE,
		SlashdotProvider.STORY_SUMMARY
	};
	
	private class CommentsAdapter extends CursorAdapter {
		

		public CommentsAdapter(Context context, Cursor c) {
			super(context, c, false);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView title = (TextView)view.findViewById(R.id.comment_title);
			title.setText(Html.fromHtml(cursor.getString(0)) + " " + Html.fromHtml(cursor.getString(1)));
			
			TextView content = (TextView)view.findViewById(R.id.comment_text);
			content.setText(Html.fromHtml(cursor.getString(2)));
			content.setMovementMethod(LinkMovementMethod.getInstance());
			
			int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4 + 8 * cursor.getInt(3), getResources().getDisplayMetrics());
			view.setPadding(px, 0, 0, 4);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater inflater = getActivity().getLayoutInflater();
			return inflater.inflate(R.layout.item_comment, parent, false);
		}
		
	};

	private class GetCommentsTask extends AsyncTask<Long, Void, Void> {
		
		@Override
		protected Void doInBackground(Long... params) {
			SlashdotContent.refreshComments(getActivity(), params[0], null);
			return null;
		}

		@Override
		protected void onPostExecute(Void v) {
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

			mStoryId = getArguments().getLong(ARG_ITEM_ID);
		} else {
			Log.wtf("StoryDetailFragment", "No story found for " + getArguments().getLong(ARG_ITEM_ID));
		}
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		Uri uri = ContentUris.withAppendedId(Uri.withAppendedPath(SlashdotProvider.BASE_URI, SlashdotProvider.STORIES_TABLE_NAME), mStoryId);
		Cursor cursor = getActivity().getContentResolver().query(uri, STORY_PROJECTION, null, null, null);
		
		if (cursor.moveToFirst()) {
			
			this.setListAdapter(new CommentsAdapter(getActivity(), null));
			
			if (this.getListAdapter() != null) {
				this.setListAdapter(null);
			}
			
			getListView().setDivider(null);
			getListView().setDividerHeight(0);
			
			View header = getActivity().getLayoutInflater().inflate(R.layout.story_header, getListView(), false);
			
			TextView title = (TextView) header.findViewById(R.id.story_title);
			title.setText(Html.fromHtml(cursor.getString(0)));
			
			final TextView summary = (TextView) header.findViewById(R.id.story_summary);
			summary.setText(Html.fromHtml(cursor.getString(0)));
			summary.setMovementMethod(LinkMovementMethod.getInstance());
			getListView().addHeaderView(header);
			
			ToggleButton button = (ToggleButton) header.findViewById(R.id.toggle);
			button.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					summary.setVisibility(isChecked ? View.VISIBLE : View.GONE);
				}});
			button.setChecked(true);
			
			(new GetCommentsTask()).execute(mStoryId);
		}
	}


	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Uri commentsUri = Uri.withAppendedPath(ContentUris.withAppendedId(
				Uri.withAppendedPath(SlashdotProvider.BASE_URI, SlashdotProvider.STORIES_TABLE_NAME), id), SlashdotProvider.COMMENTS_TABLE_NAME);

		return new CursorLoader(getActivity(), commentsUri, COMMENT_PROJECTION, null, null, null);
	}


	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		((CursorAdapter)getListAdapter()).swapCursor(cursor);
	}


	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}
	
	
}
