package com.example.optifit;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.FileInputStream;

public class FileUploader extends AsyncTask<Void, Void, String> {
    private static final String mFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AudioRecording.mp4" ;

    @Override
    protected String doInBackground(Void... params) {
        String postUrl = "http://srv01.libdom.net:8080/predict";
        File file = new File(mFileName);
        String result = "";
        try {
            FileInputStream fs = new FileInputStream(file);
            Connection.Response response = Jsoup.connect(postUrl)
                    .data("text", "value")
                    .data("file", file.getName(), fs)
                    .userAgent("Mozilla")
                    .method(Connection.Method.POST)
                    .ignoreContentType(true)
                    .execute();

            result =  response.body();
        } catch (Exception e) {
            Log.e("request", "Failed to make request");
        }
        return result;
    }
}
