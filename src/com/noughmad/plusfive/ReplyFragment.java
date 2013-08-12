package com.noughmad.plusfive;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by miha on 8/11/13.
 */
public class ReplyFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private final static String[] COMMENT_PROJECTION = {
            SlashdotProvider.ID,
            SlashdotProvider.COMMENT_TITLE,
            SlashdotProvider.COMMENT_SCORE_TEXT,
            SlashdotProvider.COMMENT_CONTENT
    };

    private final static String TAG = "ReplyFragment";

    private Bundle mReplyArgs = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.reply_fragment, container, false);

        view.findViewById(R.id.replyButton).setVisibility(View.GONE);
        view.findViewById(R.id.reply_submit).setEnabled(false);
        view.findViewById(R.id.reply_submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mReplyArgs == null) {
                    return;
                }

                final Map<String, String> data = new HashMap<String, String>();
                data.put("formkey", mReplyArgs.getString("formkey"));
                data.put("sid", Long.toString(getArguments().getLong("sid")));
                if (getArguments().containsKey("pid"))
                {
                    data.put("pid", Long.toString(getArguments().getLong("pid")));
                }

                data.put("postersubj", encode(((EditText) view.findViewById(R.id.reply_subject)).getText().toString()));
                data.put("postercomment", encode(((EditText)view.findViewById(R.id.reply_body)).getText().toString()));
                if (((CheckBox)view.findViewById(R.id.post_anon)).isChecked()) {
                    data.put("postanon",  "1");
                }

                AsyncTask<Map<String, String>, Void, Boolean> task = new AsyncTask<Map<String, String>, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(Map<String, String>... params) {
                        return SlashdotContent.postReply(getActivity(), mReplyArgs.getString("action"), params[0]);
                    }

                    @Override
                    protected void onPostExecute(Boolean success) {
                        if (success) {
                            Toast.makeText(getActivity(), R.string.comment_posted, Toast.LENGTH_LONG).show();
                            getActivity().finish();
                        } else {
                            Toast.makeText(getActivity(), R.string.comment_post_error, Toast.LENGTH_LONG).show();
                        }
                    }
                };

                task.execute(data);
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        long sid = getArguments().getLong("sid", 0);
        long pid = getArguments().getLong("pid", 0);

        AsyncTask<Long, Void, Bundle> task = new AsyncTask<Long, Void, Bundle>() {
            @Override
            protected Bundle doInBackground(Long... params) {
                return SlashdotContent.replyParameters(getActivity(), params[0], params[1]);
            }

            @Override
            protected void onPostExecute(Bundle args) {
                ((EditText)getView().findViewById(R.id.reply_subject)).setText(args.getString("postersubj"));
                mReplyArgs = args;
                getView().findViewById(R.id.reply_submit).setEnabled(true);
            }
        };

        Bundle args = new Bundle();
        args.putLong("story_id", getArguments().getLong("story_id"));
        args.putLong("sid", sid);
        if (pid > 0) {
            args.putLong("pid", pid);
        }

        task.execute(sid, pid);
        getLoaderManager().initLoader(0, args, this);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = Uri.withAppendedPath(SlashdotProvider.BASE_URI, SlashdotProvider.STORIES_TABLE_NAME);
        uri = ContentUris.withAppendedId(uri, args.getLong("story_id"));

        if (args.containsKey("pid")) {
            uri = Uri.withAppendedPath(uri, SlashdotProvider.COMMENTS_TABLE_NAME);
            uri = ContentUris.withAppendedId(uri, args.getLong("pid"));
        }

        Log.d(TAG, "Comment URI:" + uri);

        return new CursorLoader(getActivity(), uri, COMMENT_PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor.moveToFirst()) {
            View view = getView();

            TextView title = (TextView)view.findViewById(R.id.comment_title);
            title.setText(Html.fromHtml(cursor.getString(1)) + " " + Html.fromHtml(cursor.getString(2)));

            TextView content = (TextView)view.findViewById(R.id.comment_text);
            content.setText(Html.fromHtml(cursor.getString(3)));
            content.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    private String encode(String input) {
        /*
        String output;
        try {
            output = URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            output = URLEncoder.encode(input);
        }

        return output.replace("+", "%20");
        */
        return input;
    }
}
