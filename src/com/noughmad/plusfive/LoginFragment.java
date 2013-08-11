package com.noughmad.plusfive;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;

/**
 * Created by miha on 8/11/13.
 */
public class LoginFragment extends DialogFragment {
    private View mView;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        mView = inflater.inflate(R.layout.login_dialog, null, false);
        builder.setView(mView)
                .setPositiveButton(R.string.login, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, int id) {

                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                })
                .setTitle(R.string.login);
        return builder.create();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        final AlertDialog d = (AlertDialog)getDialog();
        if(d != null)
        {
            Button positiveButton = (Button) d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    final String nickname = ((EditText) mView.findViewById(R.id.editNickname)).getText().toString();
                    String password = ((EditText) mView.findViewById(R.id.editPassword)).getText().toString();

                    AsyncTask<String, Void, Connection.Response> task = new AsyncTask<String, Void, Connection.Response>() {

                        @Override
                        protected Connection.Response doInBackground(String... params) {
                            Log.d("LoginTask", String.valueOf(params));
                            try {
                                return Jsoup.connect("http://slashdot.org/my/login")
                                        .data("unickname", params[0])
                                        .data("upasswd", params[1])
                                        .method(Connection.Method.POST).execute();
                            } catch (IOException e) {
                                return null;
                            }
                        }

                        @Override
                        protected void onPostExecute(Connection.Response response) {
                            if (response != null && response.hasCookie("user")) {
                                getActivity().getSharedPreferences("cookie", Context.MODE_PRIVATE).edit().putString("user", response.cookie("user")).commit();
                                Toast.makeText(getActivity(), getResources().getString(R.string.login_success, nickname), Toast.LENGTH_SHORT).show();
                                d.dismiss();
                            } else {
                                Toast.makeText(getActivity(), R.string.login_failed, Toast.LENGTH_LONG).show();
                            }
                        }
                    };

                    task.execute(nickname, password);
                }
            });
        }
    }
}
