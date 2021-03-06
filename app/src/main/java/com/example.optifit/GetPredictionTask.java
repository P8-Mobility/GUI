package com.example.optifit;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GetPredictionTask extends AsyncTask<Void, Void, String> {
    private static final String mFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AudioRecording.mp4" ;

    /**
     * Uploads recorded audio to the API in the background to avoid halting the interface
     *
     * @return the result from passing the recorded audio through the model with the API
     */
    @Override
    protected String doInBackground(Void... params) {
        String postUrl = "https://dataindsamling.libdom.net/api/predict";
        File file = new File(mFileName);
        String result = "";

        try {
            FileInputStream fs = new FileInputStream(file);
            Connection.Response response = Jsoup.connect(postUrl)
                    .data("word", "pære")
                    .data("mediafile", file.getName(), fs)
                    .userAgent("Mozilla")
                    .header("Authorization", "Bearer 70f07f7b8b1211a7a25c7d0cb2ecb5c082abe80189119b2f0a1c0a0b72dd6d28")
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
