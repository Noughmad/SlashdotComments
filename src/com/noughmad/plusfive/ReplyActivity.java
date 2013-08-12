package com.noughmad.plusfive;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by miha on 8/11/13.
 */
public class ReplyActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_reply);

        Log.d("ReplyActivity", "Story Id:" + getIntent().getLongExtra("sid", 0));


        if (SlashdotContent.isLoggedIn(this)) {
            onLoggedIn();
        } else {
            DialogFragment newFragment = new LoginFragment();
            newFragment.show(getFragmentManager(), "login");
        }
    }

    public void onLoggedIn() {
        if (SlashdotContent.isLoggedIn(this)) {
            ReplyFragment fragment = new ReplyFragment();
            fragment.setArguments(getIntent().getExtras());

            FragmentTransaction ft = getFragmentManager().beginTransaction();

            ft.add(R.id.container, fragment);
            ft.commit();
        } else {
            finish();
        }
    }
}
