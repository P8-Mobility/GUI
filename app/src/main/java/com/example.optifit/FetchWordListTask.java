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

public class FetchWordListTask extends AsyncTask<Void, Void, String> {

    @Override
    protected String doInBackground(Void... params) {
        String getUrl = "https://dataindsamling.libdom.net/api/words";
        String result = "";

        try {
            Connection.Response response = Jsoup.connect(getUrl)
                    .ignoreContentType(true)
                    .userAgent("Mozilla")
                    .header("Authorization", "Bearer 70f07f7b8b1211a7a25c7d0cb2ecb5c082abe80189119b2f0a1c0a0b72dd6d28")
                    .execute();

            result =  response.body();
        } catch (Exception e) {
            Log.e("request", "Failed to make request");
        }

        return result;
    }
}
