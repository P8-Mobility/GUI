package com.example.optifit;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.NetworkResponse;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;
import com.example.optifit.ui.SharedViewModel;
import com.example.optifit.R;
import com.example.optifit.ui.practice.PracticeFragment;

import android.graphics.Color;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import android.content.pm.PackageManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.INTERNET;


import uk.me.hardill.volley.multipart.MultipartRequest;

public class MainActivity extends AppCompatActivity {
    private SharedViewModel model;
    private Button recordBtn;
    private Button listenBtn;

    private Boolean isCollectingData = false;

    //creating a variable for medi recorder object class.
    private MediaRecorder mRecorder;
    // creating a variable for mediaplayer class
    private MediaPlayer mPlayer;
    private MediaPlayer fluentPlayer;
    //string variable is created for storing a file name
    private static String mFileName = null;

    private boolean recordingStarted = false;
    private String postUrl = "http://srv01.libdom.net:8080/predict";
    private byte[] recordedSound;

    private RequestQueue requestQueue;

    // constant for storing audio permission
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;

    @SuppressLint({"RestrictedApi", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_exercise);
        fluentPlayer = MediaPlayer.create(this, R.raw.paere);
        while (!CheckPermissions()) RequestPermissions();

        model = new ViewModelProvider(this).get(SharedViewModel.class);
        model.prepare(getApplicationContext());

        recordBtn = (Button) findViewById(R.id.recordbtn);
        recordBtn.setOnTouchListener(getButtonTouchListener());
        recordBtn.setOnClickListener(getButtonClickListener());

        listenBtn = (Button) findViewById(R.id.listenBtn);
        listenBtn.setOnClickListener(getListenButtonClickListener());
        //listenBtn.setOnClickListener();

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        setRestRequestResponseListener();
        setRestRequestResponseErrorListener();

    }

    private View.OnClickListener getButtonClickListener() {
        return null; //Used to ensure the app dosen't crash when the user clicks instead of holding
    }

    private void setRestRequestResponseListener(){
        MultipartRequest request = new MultipartRequest(postUrl, null,
                new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                        System.out.println("det virker!!!!!!!!!!!!!!!!");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println("SE HER!!!!!!!" + error.getMessage());
                    }
                });

        //request.addPart(new MultipartRequest.FormPart(fieldName,value));
        request.addPart(new MultipartRequest.FilePart("file", "audio/wav", mFileName, recordedSound));

        requestQueue.add(request);
    }

    private void setRestRequestResponseErrorListener() {
        model.restRequestHandler.setResponseErrorListener(new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("VOLLEY", error.toString());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // this method is called when user will grant the permission for audio recording.
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSION_CODE:
                if (grantResults.length > 0) {
                    boolean permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean permissionToStore = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (permissionToRecord && permissionToStore) {
                        Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    public boolean CheckPermissions() {
        //this method is used to check permission
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void RequestPermissions() {
        // this method is used to request the permission for audio recording and storage.
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET, WRITE_EXTERNAL_STORAGE}, REQUEST_AUDIO_PERMISSION_CODE);
    }
    private void startRecording() {
        // check permission method is used to check that the user has granted permission to record nd store the audio.
        if (CheckPermissions()) {
            //setbackgroundcolor method will change the background color of text view.
            //we are here initializing our filename variable with the path of the recorded audio file.
            mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
            mFileName += "/AudioRecording.wav";
            //below method is used to initialize the media recorder clss
            mRecorder = new MediaRecorder();
            //below method is used to set the audio source which we are using a mic.
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(AudioFormat.ENCODING_PCM_16BIT);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            //below method is used to set the output file location for our recorded audio
            mRecorder.setOutputFile(mFileName);
            try {
                //below mwthod will prepare our audio recorder class
                mRecorder.prepare();
            } catch (IOException e) {
                Log.e("TAG", "prepare() failed");
            }
            // start method will start the audio recording.
            mRecorder.start();
            recordingStarted = true;
        } else {
            //if audio recording permissions are not granted by user below method will ask for runtime permission for mic and storage.
            RequestPermissions();
        }
    }
    public void playAudio() {
        //for playing our recorded audio we are using media player class.
        mPlayer = new MediaPlayer();
        try {
            //below method is used to set the data source which will be our file name
            mPlayer.setDataSource(mFileName);
            //below method will prepare our media player
            mPlayer.prepare();
            //below method will start our media player.
            mPlayer.start();
        } catch (IOException e) {
            Log.e("TAG", "prepare() failed");
        }


    }

    public void playFluentSound() {
        //for playing our recorded audio we are using media player class.
        fluentPlayer.start();
    }


    public void stopRecording() {
        try{
            if(recordingStarted) {
                //below method will stop the audio recording.
                mRecorder.stop();
                //below method will release the media recorder class.
                mRecorder.release();
                mRecorder = null;
            }
            recordingStarted = false;
        }
        catch (Exception e){
            Log.e("TAG", "prepare() failed");
        }
        createByteArr();
        setRestRequestResponseListener();
    }

    private void createByteArr() {
        try {
            File file = new File(mFileName);
            InputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer, 0, buffer.length);
            fis.close();
            recordedSound = buffer;
        } catch (Exception e) {

        }
    }


    private View.OnTouchListener getButtonTouchListener() {
        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN: {
                        // Turn pressed button gray
                        MainActivity.this.recordBtn.setBackgroundColor(Color.GRAY);
                        MainActivity.this.recordBtn.setText(R.string.press_down);
                        startRecording();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        // Reset button
                        recordBtn.setBackgroundColor(getResources().getColor(R.color.light_blue_400));
                        MainActivity.this.recordBtn.setText(R.string.record_repetition);
                        stopRecording();
                        playAudio();
                        break;
                    }
                }
                return true;
            }
        };
    }
        private View.OnClickListener getListenButtonClickListener() {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playFluentSound();
                }
            };

        }

}