package com.example.optifit.data;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.optifit.ui.SharedViewModel;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DataHandler {
    private boolean collectData = false;
    private final ConcurrentLinkedQueue<String> queue;
    private boolean txQueueProcessing = false;
    private SharedViewModel model;
    private static final int DATA_POINTS_NEEDED_FOR_CLASSIFICATION = 200;

    public DataHandler(SharedViewModel sharedViewModel){
        queue = new ConcurrentLinkedQueue<>();
        model = sharedViewModel;
    }

    public boolean enableDataGathering(){
        if(this.queue.isEmpty()) {
            this.collectData = true;
            return true;
        }

        return false;
    }

    public void disableDataGathering(){
        this.collectData = false;
        this.queue.clear();
    }

    public JSONArray getRecordedData(){
        JSONArray jsonArray = new JSONArray();

        while (!this.queue.isEmpty()) {
            String data = this.queue.poll();


                if(data != null){
                    //JSONObject obj = data.getJsonObject();
                    //if(obj != null)
                      //  jsonArray.put(obj);
                }else{
                    Log.e("TagData", "Tagdata is empty (null)");
                }

        }

        return jsonArray;
    }

    public void addData(String data){
        if(this.collectData){
            this.queue.add(data);

            if(this.queue.size() >= DATA_POINTS_NEEDED_FOR_CLASSIFICATION) {
                int counter = 0;
                JSONArray jsonArray = new JSONArray();

                while (!this.queue.isEmpty() && counter <= DATA_POINTS_NEEDED_FOR_CLASSIFICATION) {
                    String element = this.queue.poll();


                        if(element != null){
                            //JSONObject obj = element.getJsonObject();
                            //if(obj != null)
                              //  jsonArray.put(obj);
                        }

                    counter++;
                }

                model.restRequestHandler.addRequest(jsonArray);
            }
        }
    }

}
