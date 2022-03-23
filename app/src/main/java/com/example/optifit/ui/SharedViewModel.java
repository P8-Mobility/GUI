package com.example.optifit.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import androidx.lifecycle.ViewModel;

import com.example.optifit.data.DataHandler;
import com.example.optifit.data.RestRequestHandler;


public class SharedViewModel extends ViewModel {




    public DataHandler dataHandler;
    public RestRequestHandler restRequestHandler;


    public void prepare(Context context){
        dataHandler = new DataHandler(this);
        restRequestHandler = new RestRequestHandler(context);
    }


}
