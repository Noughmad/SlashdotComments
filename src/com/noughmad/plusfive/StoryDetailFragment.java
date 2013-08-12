package com.noughmad.plusfive;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
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
import android.widget.CursorAdapter;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import java.util.Calendar;

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
    private long mStorySid = 0;
    private View mHeader;

    private TextView mQuoteTextView;
	
	private static String[] COMMENT_PROJECTION = new String[] {
		SlashdotProvider.ID,
		SlashdotProvider.COMMENT_TITLE,
		SlashdotProvider.COMMENT_SCORE_TEXT,
		SlashdotProvider.COMMENT_CONTENT,
		SlashdotProvider.COMMENT_LEVEL
	};
	
	private static String[] STORY_PROJECTION = new String[] {
		SlashdotProvider.STORY_TITLE,
		SlashdotProvider.STORY_SUMMARY,
        SlashdotProvider.STORY_SID
	};

    private static String[] QUOTE_PROJECTION = new String[] {
        SlashdotProvider.QUOTE_CONTENT
    };

    private class CommentsAdapter extends CursorAdapter {
		

		public CommentsAdapter(Context context, Cursor c) {
			super(context, c, false);
		}

		@Override
		public void bindView(View view, Context context, final Cursor cursor) {
			TextView title = (TextView)view.findViewById(R.id.comment_title);
			title.setText(Html.fromHtml(cursor.getString(1)) + " " + Html.fromHtml(cursor.getString(2)));
			
			TextView content = (TextView)view.findViewById(R.id.comment_text);
			content.setText(Html.fromHtml(cursor.getString(3)));
			content.setMovementMethod(LinkMovementMethod.getInstance());
			
			int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4 + 8 * cursor.getInt(4), getResources().getDisplayMetrics());
			view.setPadding(px, 0, 0, 4);

            final long commentId = cursor.getLong(0);

            view.findViewById(R.id.replyButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mStorySid > 0)
                    {
                        Intent intent = new Intent(getActivity(), ReplyActivity.class);
                        intent.putExtra("story_id", mStoryId);
                        intent.putExtra("sid", mStorySid);
                        intent.putExtra("pid", commentId);
                        startActivity(intent);
                    }
                }
            });
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
		
		getListView().setDivider(null);
		getListView().setDividerHeight(0);
        getListView().setFastScrollEnabled(true);

        View quote = getActivity().getLayoutInflater().inflate(R.layout.quote, null, false);
        mQuoteTextView = (TextView)quote.findViewById(R.id.quote_text);
        getListView().addFooterView(quote);

        AdView ad = new AdView(getActivity(), AdSize.BANNER, "a151f3af95c37cd");
        getListView().addFooterView(ad);

        mHeader = getActivity().getLayoutInflater().inflate(R.layout.story_header, getListView(), false);
        final TextView summary = (TextView) mHeader.findViewById(R.id.story_summary);

        mHeader.findViewById(R.id.story_title).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (summary.getVisibility() == View.VISIBLE) {
                    summary.setVisibility(View.GONE);
                } else {
                    summary.setVisibility(View.VISIBLE);
                }
            }
        });

        mHeader.findViewById(R.id.postButton).setEnabled(false);

        SeekBar bar = (SeekBar)mHeader.findViewById(R.id.comment_score_seek);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ((TextView)mHeader.findViewById(R.id.comment_score_limit)).setText(getResources().getString(R.string.comment_score_limit, progress-1));
                getActivity().getPreferences(Context.MODE_PRIVATE).edit().putInt("CommentScoreLimit", progress-1).commit();
                Bundle args = new Bundle();
                args.putInt("Score", progress-1);
                getLoaderManager().restartLoader(0, args, StoryDetailFragment.this);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        int score = getActivity().getPreferences(Context.MODE_PRIVATE).getInt("CommentScoreLimit", 1);
        Bundle args = new Bundle();
        args.putInt("Score", score);
        ((TextView)mHeader.findViewById(R.id.comment_score_limit)).setText(getResources().getString(R.string.comment_score_limit, score));
        bar.setProgress(score+1);

        mHeader.findViewById(R.id.postButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ReplyActivity.class);
                intent.putExtra("story_id", mStoryId);
                intent.putExtra("sid", mStorySid);
                startActivity(intent);
            }
        });

        getListView().addHeaderView(mHeader);
        setListAdapter(new CommentsAdapter(getActivity(), null));
        (new GetCommentsTask()).execute(mStoryId);

        getLoaderManager().initLoader(0, args, this);
        getLoaderManager().initLoader(2, null, this);
        getLoaderManager().initLoader(1, null, this);

        AdRequest request = new AdRequest();
        request.addTestDevice(AdRequest.TEST_EMULATOR);
        ad.loadAd(request);
	}


	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id)
        {
            case 0:
                Uri commentsUri = Uri.withAppendedPath(ContentUris.withAppendedId(
                        Uri.withAppendedPath(SlashdotProvider.BASE_URI, SlashdotProvider.STORIES_TABLE_NAME), mStoryId), SlashdotProvider.COMMENTS_TABLE_NAME);
                return new CursorLoader(getActivity(), commentsUri, COMMENT_PROJECTION, SlashdotProvider.COMMENT_SCORE_NUM + " >= ?", new String[] {Integer.toString(args.getInt("Score", 1))}, null);
            case 1:
                Uri quoteUri = Uri.withAppendedPath(SlashdotProvider.BASE_URI, SlashdotProvider.QUOTES_TABLE_NAME);
                quoteUri = ContentUris.withAppendedId(quoteUri, Calendar.getInstance().getTimeInMillis());
                return new CursorLoader(getActivity(), quoteUri, QUOTE_PROJECTION, null, null, null);
            case 2:
                Uri storyUri = ContentUris.withAppendedId(Uri.withAppendedPath(SlashdotProvider.BASE_URI, SlashdotProvider.STORIES_TABLE_NAME), mStoryId);
                return new CursorLoader(getActivity(), storyUri, STORY_PROJECTION, null, null, null);
        }
        return null;
	}


	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId())
        {
            case 0:
                ((CursorAdapter)getListAdapter()).swapCursor(cursor);
                break;

            case 1:
                if (cursor.moveToFirst())
                {
                    mQuoteTextView.setText(Html.fromHtml(cursor.getString(0)));
                }
                break;
            
            case 2:
                if (cursor.moveToFirst()) {
        
                    TextView title = (TextView) mHeader.findViewById(R.id.story_title);
                    title.setText(Html.fromHtml(cursor.getString(0)));
    
                    TextView summary = (TextView) mHeader.findViewById(R.id.story_summary);
                    summary.setText(Html.fromHtml(cursor.getString(1)));
                    summary.setMovementMethod(LinkMovementMethod.getInstance());

                    if (cursor.getLong(2) != mStorySid) {
                        mStorySid = cursor.getLong(2);
                        mHeader.findViewById(R.id.postButton).setEnabled(mStoryId > 0);
                    }
                } else {
                    Log.wtf("StoryDetailFragment", "Story with id " + mStoryId + " not found");
                }
                break;
        }
	}


	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}
	
	public long getStoryId() {
		return mStoryId;
	}
}
