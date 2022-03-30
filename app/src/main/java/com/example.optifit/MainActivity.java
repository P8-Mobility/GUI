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
    private static String mFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AudioRecording.mp4";

    private MediaPlayer fluentPlayer; // For example sound
    private boolean recordingStarted = false;

    @SuppressLint({"RestrictedApi", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_main);

        while (!CheckPermissions()) RequestPermissions();

        fluentPlayer = MediaPlayer.create(this, R.raw.paere);

        recordBtn = findViewById(R.id.recordbtn);
        recordBtn.setOnTouchListener(getButtonTouchListener());
        recordBtn.setOnClickListener(getButtonClickListener());

        responseTxt = findViewById(R.id.responseTxt);

        Button listenBtn = findViewById(R.id.listenBtn);
        listenBtn.setOnClickListener(getListenButtonClickListener());
    }

    private View.OnClickListener getButtonClickListener() {
        //Used to ensure the app doesn't crash when the user clicks button instead of holding it down
        return null;
    }

    /**
     * Requests the API with the recorded audio and updated the feedback when a response is received
     */
    private void uploadRecordingAndUpdateFeedbackOnResponse() {
        try {
            String result = new FileUploader().execute().get();
            Gson gson = new Gson();
            Map<String, String> asMap = gson.fromJson(result, Map.class);
            if (asMap.containsKey("status")) {
                responseTxt.setText("Vi hørte dig sige: " + asMap.get("result"));
            }
        } catch (Exception e) {
            responseTxt.setText(R.string.exceptionDuringUpload);
        }
    }

    /**
     * Requests user for permissions to access storage and record audio
     * (if requestCode is anything other than REQUEST_AUDIO_PERMISSION_CODE, this method does nothing)
     *
     * @param requestCode  int representation of the permissions request (anything other than REQUEST_AUDIO_PERMISSION_CODE will result in no action)
     * @param permissions  list permissions (not used)
     * @param grantResults list of permission statuses, used to check whether the permissions have been granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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

    /**
     * Check whether permissions for external storage and audio recording are granted
     *
     * @return whether the above permissions are granted
     */
    public boolean CheckPermissions() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void RequestPermissions() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET, WRITE_EXTERNAL_STORAGE}, REQUEST_AUDIO_PERMISSION_CODE);
    }

    private void startRecording() {
        if (CheckPermissions()) {
            // Initialization of filename to the path of the recorded audio file
            mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
            mFileName += "/AudioRecording.mp4";

            mRecorder = new MediaRecorder();

            // Set-up of the recorder to ensure correct format
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(AudioFormat.ENCODING_PCM_16BIT);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            mRecorder.setOutputFile(mFileName);
            try {
                mRecorder.prepare();
            } catch (IOException e) {
                Log.e("TAG", "prepare() failed");
            }

            mRecorder.start();
            recordingStarted = true;
        } else {
            RequestPermissions();
        }
    }

    /**
     * Plays the audio recorded by the user
     */
    public void playAudio() {
        MediaPlayer mPlayer = new MediaPlayer();
        try {
            // Prepare player to play recording and play it
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e("TAG", "prepare() failed");
        }
    }

    /**
     * Stop the recording and upload it to the API
     */
    public void stopRecording() {
        if (null == mRecorder) {
            return;
        }

        // Add delay to release event to ensure that the end of the word is also recorded
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
                    Log.e("TAG", "Exception thrown during release of recorder object");
                }
                playAudio();
                uploadRecordingAndUpdateFeedbackOnResponse();

                // Reset button
                recordBtn.setBackgroundColor(getResources().getColor(R.color.light_blue_400));
                MainActivity.this.recordBtn.setText(R.string.record);
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
                    MainActivity.this.recordBtn.setText(R.string.recording);
                    startRecording();
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    stopRecording();
                    break;
                }
            }
            return true;
        };
    }

    private View.OnClickListener getListenButtonClickListener() {
        return v -> fluentPlayer.start();
    }
}
