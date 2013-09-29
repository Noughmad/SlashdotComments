package com.noughmad.plusfive;

import android.app.Activity;
import android.os.Bundle;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;


/**
 * Created by miha on 9/29/13.
 */
public abstract class RefreshActivity extends Activity
        implements PullToRefreshAttacher.OnRefreshListener
{

    private PullToRefreshAttacher mAttacher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAttacher = PullToRefreshAttacher.get(this);
    }

    public PullToRefreshAttacher getAttacher() {
        return mAttacher;
    }
}
