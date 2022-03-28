package com.example.optifit;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.optifit.ui.SharedViewModel;

import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import android.content.pm.PackageManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.INTERNET;


import org.jsoup.Connection;
import org.jsoup.Jsoup;

import uk.me.hardill.volley.multipart.MultipartRequest;

public class MainActivity extends AppCompatActivity {
    private SharedViewModel model;
    private Button recordBtn;

    // Sound recording
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;
    private AudioRecord mRecorder;
    private static final String mFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.wav" ;

    private MediaPlayer fluentPlayer; // for example sound
    private boolean recordingStarted = false;

    private byte[] recordedSound;
    private RequestQueue requestQueue;

    private final int sr = 44100;
    private final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private final int audioFormat = AudioFormat.ENCODING_PCM_FLOAT;
    private final int bufferSize = AudioRecord.getMinBufferSize(sr, channelConfig, audioFormat) * 2;
    private Thread recordingThread = null;
    private final AtomicBoolean recordingInProgress = new AtomicBoolean(false);

    @SuppressLint({"RestrictedApi", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_exercise);

        while (!CheckPermissions()) RequestPermissions();

        model = new ViewModelProvider(this).get(SharedViewModel.class);
        model.prepare(getApplicationContext());

        fluentPlayer = MediaPlayer.create(this, R.raw.paere);

        recordBtn = findViewById(R.id.recordbtn);
        recordBtn.setOnTouchListener(getButtonTouchListener());
        recordBtn.setOnClickListener(getButtonClickListener());

        Button listenBtn = findViewById(R.id.listenBtn);
        listenBtn.setOnClickListener(getListenButtonClickListener());

        setRestRequestResponseErrorListener();
        requestQueue = Volley.newRequestQueue(getApplicationContext());
    }

    private View.OnClickListener getButtonClickListener() {
        //Used to ensure the app doesn't crash when the user clicks button instead of holding it down
        return null;
    }

    private void setRestRequestResponseListener() {
        try{
            String result = new FileUploader().execute().get();
        }
        catch (Exception e){

        }
    }

    private void setRestRequestResponseErrorListener() {
        model.restRequestHandler.setResponseErrorListener(error -> Log.e("VOLLEY", error.toString()));
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
        if (CheckPermissions()) {
            mRecorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, sr, channelConfig, audioFormat, bufferSize);


            mRecorder.startRecording();
            recordingStarted = true;
            recordingInProgress.set(true);

            recordingThread = new Thread(new RecordingRunnable(), "Recording Thread");
            recordingThread.start();

        } else {
            RequestPermissions();
        }
    }

    private class RecordingRunnable implements Runnable {

        @Override
        public void run() {
            final File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "recording.wav");
            final ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);

            try (final FileOutputStream outStream = new FileOutputStream(file)) {
                while (recordingInProgress.get()) {
                    int result = mRecorder.read(buffer, bufferSize);
                    if (result < 0) {
                        throw new RuntimeException("Reading of audio buffer failed: " +
                                getBufferReadFailureReason(result));
                    }
                    outStream.write(buffer.array(), 0, bufferSize);
                    buffer.clear();
                }
            } catch (IOException e) {
                throw new RuntimeException("Writing of recorded audio failed", e);
            }
        }
    }

    private String getBufferReadFailureReason(int errorCode) {
        switch (errorCode) {
            case AudioRecord.ERROR_INVALID_OPERATION:
                return "ERROR_INVALID_OPERATION";
            case AudioRecord.ERROR_BAD_VALUE:
                return "ERROR_BAD_VALUE";
            case AudioRecord.ERROR_DEAD_OBJECT:
                return "ERROR_DEAD_OBJECT";
            case AudioRecord.ERROR:
                return "ERROR";
            default:
                return "Unknown (" + errorCode + ")";
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
        try {
            if (recordingInProgress.get()) {
                recordingInProgress.set(false);
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
                recordingThread = null;
            }
            recordingStarted = false;
        } catch (Exception e) {
            Log.e("TAG", "prepare() failed");
        }
        createByteArr();
        setRestRequestResponseListener();
    }

    private void createByteArr() {
        try {
            recordedSound = Files.readAllBytes(Paths.get(mFileName));
        } catch (Exception e) {
            Log.e("ByteArr", "Failed to create byte array");
        }
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
                    playAudio();
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