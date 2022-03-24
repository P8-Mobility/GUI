package com.example.optifit.data;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;



import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RestRequestHandler {
    RequestQueue requestQueue;
    Response.Listener<String> responseListener;
    private Response.ErrorListener responseErrorListener;

    public RestRequestHandler(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public void setResponseListener(Response.Listener<String> newResponseListener) {
        this.responseListener = newResponseListener;
    }

    public void setResponseErrorListener(Response.ErrorListener newResponseErrorListener) {
        this.responseErrorListener = newResponseErrorListener;
    }

    public void addRequest(JSONArray data) {
        String postUrl = "https://fitrest.libdom.net/v1/classify";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, postUrl, this.responseListener, this.responseErrorListener) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer MmilZ8wvo6keiBr1CyuCd6ZnAfx4FS5s");
                return headers;
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                System.out.println("Num rows: "+data.length());
                System.out.println(data.toString());

                return data.toString().getBytes(StandardCharsets.UTF_8);
            }
        };

        requestQueue.add(stringRequest);
    }
}
