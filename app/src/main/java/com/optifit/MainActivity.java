package com.example.optifit;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.optifit.ui.SharedViewModel;

import android.graphics.Color;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.LayoutInflater;
import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
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

import java.io.IOException;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {
    private SharedViewModel model;
    private Button recordBtn;

    private Boolean isCollectingData = false;

    //Intializing all variables..
    private TextView startTV, stopTV, playTV, stopplayTV, statusTV;
    //creating a variable for medi recorder object class.
    private MediaRecorder mRecorder;
    // creating a variable for mediaplayer class
    private MediaPlayer mPlayer;
    //string variable is created for storing a file name
    private static String mFileName = null;


    // constant for storing audio permission
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;

    @SuppressLint({"RestrictedApi", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        while (!CheckPermissions()) RequestPermissions();

        setContentView(R.layout.fragment_practice);
        model = new ViewModelProvider(this).get(SharedViewModel.class);
        model.prepare(getApplicationContext());

        recordBtn = (Button) findViewById(R.id.recordbtn);
        recordBtn.setOnTouchListener(getButtonTouchListener());

        setRestRequestResponseListener();
        setRestRequestResponseErrorListener();
    }

    private void setRestRequestResponseListener() {
        model.restRequestHandler.setResponseListener(new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject result = new JSONObject(response);
                    if (result.getString("status").equals("OK") && isCollectingData) {
                        ((TextView) findViewById(R.id.error_on_classification)).setText("");
                        if (result.getString("exercise").equals("00000000-0000-0000-0000-000000000000")) {
                            TextView prediction = findViewById(R.id.exercise_prediction);
                            prediction.setTextColor(Color.BLACK);
                        } else {
                            boolean errorResult = result.getInt("mistakes") == 0;
                            String classificationResult = errorResult ? "Good" : "Bad";
                            classificationResult += " ";

                            TextView prediction = findViewById(R.id.exercise_prediction);
                            prediction.setText(classificationResult);

                            if (errorResult) {
                                prediction.setTextColor(getResources().getColor(R.color.connectedSensorColor));
                            } else {
                                prediction.setTextColor(Color.RED);
                            }
                        }

                    }
//                    If an error occurred (most often due to too few data points), the below prints an error message in the app.
//                    Removed after continuous classification implementation, as it should never happen and should just be ignored if it does.
//                    Succeeding classifications will replace the preceding classification after new data bhs been collected
//                    else {
//                        TextView error_on_classification = root.findViewById(R.id.error_on_classification);
//                        error_on_classification.setText(result.getString("message"));
//                        TextView prediction = root.findViewById(R.id.exercise_prediction);
//                        prediction.setText(R.string.exercise_prediction_failed);
//                        prediction.setTextColor(Color.RED);
//                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
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
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, REQUEST_AUDIO_PERMISSION_CODE);
    }
    private void startRecording() {
        recordBtn.setBackgroundColor(getResources().getColor(R.color.gray));

        // We are here initializing our filename variable with the path of the recorded audio file.
        mFileName = System.getProperty("user.dir");
        mFileName += "/AudioRecording.wav";

        mRecorder = new MediaRecorder();
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
        statusTV.setText("Recording Started");
    }

    public void playAudio() {
        stopTV.setBackgroundColor(getResources().getColor(R.color.gray));
        startTV.setBackgroundColor(getResources().getColor(R.color.purple_200));
        playTV.setBackgroundColor(getResources().getColor(R.color.gray));
        stopplayTV.setBackgroundColor(getResources().getColor(R.color.purple_200));
        //for playing our recorded audio we are using media player class.
        mPlayer = new MediaPlayer();
        try {
            //below method is used to set the data source which will be our file name
            mPlayer.setDataSource(mFileName);
            //below method will prepare our media player
            mPlayer.prepare();
            //below method will start our media player.
            mPlayer.start();
            statusTV.setText("Recording Started Playing");
        } catch (IOException e) {
            Log.e("TAG", "prepare() failed");
        }


    }

    public void stopRecording() {
        recordBtn.setBackgroundColor(getResources().getColor(R.color.purple_500));
        //below method will stop the audio recording.
        mRecorder.stop();
        //below method will release the media recorder class.
        mRecorder.release();
        mRecorder = null;
        statusTV.setText("Recording Stopped");

    }

    public void pausePlaying() {
        //this method will release the media player class and pause the playing of our recorded audio.
        mPlayer.release();
        mPlayer = null;
        stopTV.setBackgroundColor(getResources().getColor(R.color.gray));
        startTV.setBackgroundColor(getResources().getColor(R.color.purple_200));
        playTV.setBackgroundColor(getResources().getColor(R.color.purple_200));
        stopplayTV.setBackgroundColor(getResources().getColor(R.color.gray));
        statusTV.setText("Recording Play Stopped");

    }

    private View.OnTouchListener getButtonTouchListener() {
        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        // Turn pressed button red
                        startRecording();
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        // Reset button
                        stopRecording();
                        break;
                    }
                }
                return true;
            }
        };
    }
}