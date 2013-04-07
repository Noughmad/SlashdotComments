package com.noughmad.slashdotcomments;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ShareActionProvider;

/**
 * An activity representing a single Story detail screen. This activity is only
 * used on handset devices. On tablet-size devices, item details are presented
 * side-by-side with a list of items in a {@link StoryListActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link StoryDetailFragment}.
 */
public class StoryDetailActivity extends FragmentActivity {
	
	private ShareActionProvider mShareProvider;
	private long mStoryId;
	private ViewPager mViewPager;
	
	private class StoriesPagerAdapter extends FragmentStatePagerAdapter {
		
		private Cursor mCursor;

		public StoriesPagerAdapter(FragmentManager fm) {
			super(fm);
			Uri uri = Uri.withAppendedPath(SlashdotProvider.BASE_URI, SlashdotProvider.STORIES_TABLE_NAME);
			String[] projection = new String[] {SlashdotProvider.ID, SlashdotProvider.STORY_TITLE};
			mCursor = getContentResolver().query(uri, projection, null, null, SlashdotProvider.ID + " DESC");
		}

		@Override
		public Fragment getItem(int position) {
			if (mCursor.moveToPosition(position)) {
				Bundle args = new Bundle();
				args.putLong(StoryDetailFragment.ARG_ITEM_ID, mCursor.getLong(0));
				StoryDetailFragment fragment = new StoryDetailFragment();
				fragment.setArguments(args);
				return fragment;
			}
			return null;
		}

		@Override
		public int getCount() {
			return mCursor.getCount();
		}
		
		@Override
		public CharSequence getPageTitle(int position) {
			mCursor.moveToPosition(position);
			return mCursor.getString(1);
		}

		public int findItem(long id) {
			mCursor.moveToPosition(-1);
			while (mCursor.moveToNext()) {
				if (mCursor.getLong(0) == id) {
					return mCursor.getPosition();
				}
			}
			return -1;
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_story_detail);

		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		mViewPager = (ViewPager) findViewById(R.id.pager);
		StoriesPagerAdapter adapter = new StoriesPagerAdapter(getSupportFragmentManager());
		mViewPager.setAdapter(adapter);

		// savedInstanceState is non-null when there is fragment state
		// saved from previous configurations of this activity
		// (e.g. when rotating the screen from portrait to landscape).
		// In this case, the fragment will automatically be re-added
		// to its container so we don't need to manually add it.
		// For more information, see the Fragments API guide at:
		//
		// http://developer.android.com/guide/components/fragments.html
		//
		if (savedInstanceState == null) {
			int position = adapter.findItem(getIntent().getLongExtra(StoryDetailFragment.ARG_ITEM_ID, 0));
			if (position > -1) {
				mViewPager.setCurrentItem(position);
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			Intent upIntent = new Intent(this, StoryListActivity.class);
			upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(upIntent);
            finish();
			return true;
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		Uri uri = ContentUris.withAppendedId(Uri.withAppendedPath(SlashdotProvider.BASE_URI, SlashdotProvider.STORIES_TABLE_NAME), mStoryId);
		Cursor cursor = getContentResolver().query(uri, new String[] {SlashdotProvider.STORY_TITLE, SlashdotProvider.STORY_URL}, null, null, null);
		
		if (cursor.moveToFirst()) {
			getMenuInflater().inflate(R.menu.story_detail_menu, menu);
			mShareProvider = (ShareActionProvider) menu.findItem(R.id.share).getActionProvider();
	
			Intent i = new Intent(Intent.ACTION_SEND);
			i.setType("text/plain");
			i.putExtra(Intent.EXTRA_SUBJECT, cursor.getString(0));
			i.putExtra(Intent.EXTRA_TEXT, cursor.getString(1));
			mShareProvider.setShareIntent(i);
		}
		
		cursor.close();
		
		return true;
	}
}
