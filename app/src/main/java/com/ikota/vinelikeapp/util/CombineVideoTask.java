package com.ikota.vinelikeapp.util;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


public class CombineVideoTask extends AsyncTask<String, Void, Boolean>{

    private final Context mContext;
    private final ArrayList<File> mSrcMovies;

    public CombineVideoTask(Context context, ArrayList<File> src_movies) {
        mContext = context;
        mSrcMovies = src_movies;
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        try {
            return VineHelper.combineVideo(mContext, mSrcMovies);
        } catch (IOException e) {
            Log.e("CombineVideoTask", e.getMessage());
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        String message;
        if(success) {
            message = "Combined your videos !!";
        } else {
            message = "Failed to combine...";
        }
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }
}
