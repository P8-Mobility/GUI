package com.example.optifit;


import com.google.gson.Gson;

import android.graphics.Color;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.pm.PackageManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.INTERNET;

public class MainActivity extends AppCompatActivity {
    private Button recordBtn;
    private TextView responseTxt;

    // Sound recording
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;
    private MediaRecorder mRecorder;
    private static String mFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AudioRecording.mp4" ;

    private MediaPlayer fluentPlayer; // for example sound
    private boolean recordingStarted = false;



    @SuppressLint({"RestrictedApi", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_exercise);

        while (!CheckPermissions()) RequestPermissions();

        fluentPlayer = MediaPlayer.create(this, R.raw.paere);

        recordBtn = findViewById(R.id.recordbtn);
        recordBtn.setOnTouchListener(getButtonTouchListener());
        recordBtn.setOnClickListener(getButtonClickListener());

        responseTxt =findViewById(R.id.responseTxt);

        Button listenBtn = findViewById(R.id.listenBtn);
        listenBtn.setOnClickListener(getListenButtonClickListener());

        setRestRequestResponseErrorListener();
    }

    private View.OnClickListener getButtonClickListener() {
        //Used to ensure the app doesn't crash when the user clicks button instead of holding it down
        return null;
    }


    private void setRestRequestResponseListener() {
        try{
            String result = new FileUploader().execute().get();
            Gson gson = new Gson();
            Map<String, String> asMap = gson.fromJson(result, Map.class);
            if(asMap.containsKey("status")){
                responseTxt.setText("Vi hørte dig sige: " + asMap.get("result"));
            }
        }
        catch (Exception e){

        }
    }


        private void setRestRequestResponseErrorListener() {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // this method is called when user will grant the permission for audio recording.
        if (requestCode == REQUEST_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0) {
                boolean permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean permissionToStore = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                if (permissionToRecord && permissionToStore) {
                    Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public boolean CheckPermissions() {
        //this method is used to check whether permissions have been granted
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void RequestPermissions() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET, WRITE_EXTERNAL_STORAGE}, REQUEST_AUDIO_PERMISSION_CODE);
    }

    private void startRecording() {
        // check permission method is used to check that the user has granted permission to record nd store the audio.
        if (CheckPermissions()) {
            //setbackgroundcolor method will change the background color of text view.
            //we are here initializing our filename variable with the path of the recorded audio file.
            mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
            mFileName += "/AudioRecording.mp4";
            //below method is used to initialize the media recorder clss
            mRecorder = new MediaRecorder();
            //below method is used to set the audio source which we are using a mic.
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
        // for recorded sound
        MediaPlayer mPlayer = new MediaPlayer();
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
        if (null == mRecorder){
            return;
        }
        Timer buttonTimer = new Timer();
        buttonTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (recordingStarted) {
                        mRecorder.stop();
                        mRecorder.release();
                        mRecorder = null;
                    }
                    recordingStarted = false;
                } catch (Exception e) {
                    Log.e("TAG", "prepare() failed");
                }
                playAudio();
                setRestRequestResponseListener();
            }
        }, 500);
    }


    private View.OnTouchListener getButtonTouchListener() {
        return (v, event) -> {
            v.performClick();
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
                    break;
                }
            }
            return true;
        };
    }

    private View.OnClickListener getListenButtonClickListener() {
        return v -> playFluentSound();
    }
}