package com.example.optifit;

import com.google.gson.Gson;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Paint;
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
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.INTERNET;

import kotlin.Triple;

public class MainActivity extends AppCompatActivity {
    private TextView wordTxt;
    private Button listenBtn;
    private TextView responseTxt;
    private ImageView earImage;
    private Button recordBtn;

    // Sound recording
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;
    private MediaRecorder mRecorder;
    private static final String mFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AudioRecording.mp4";

    private MediaPlayer fluentPlayer; // For example sound
    private boolean recordingStarted = false;

    private ResourceLoader resourceLoader;
    ArrayList<Pair<String, String>> wordList = new ArrayList<>();
    ArrayList<Triple<String, String, String>> specialFeedbackCases = new ArrayList<>();
    Pair<String, String> currentWord;

    @SuppressLint({"RestrictedApi"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_main);
        resourceLoader = new ResourceLoader(getResources());

        while (!CheckPermissions()) RequestPermissions();

        fluentPlayer = MediaPlayer.create(this, R.raw.paere);

        responseTxt = findViewById(R.id.responseTxt);
        earImage = findViewById(R.id.earImage);

        initializeWord();
        initializeButtons();
    }

    private void initializeWord() {
        wordTxt = findViewById(R.id.wordTxt);
        wordList = resourceLoader.parseWordList();
        specialFeedbackCases = resourceLoader.parseSpecialCasesList();
        currentWord = wordList.stream()
                .filter((w) -> w.first.equals("Pære")) // Ensures that "Pære" is displayed, should be removed in final product
                .findFirst().orElse(null);
        wordTxt.setText(currentWord == null ? "Ord mangler" : '"' + currentWord.first + '"');
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initializeButtons() {
        recordBtn = findViewById(R.id.recordbtn);
        recordBtn.setOnTouchListener(getButtonTouchListener());
        recordBtn.setOnClickListener(getButtonClickListener());
        recordBtn.setBackgroundResource(R.drawable.rounded_corners_primary);

        listenBtn = findViewById(R.id.listenBtn);
        listenBtn.setOnClickListener(getListenButtonClickListener());
        listenBtn.setBackgroundResource(R.drawable.rounded_corners_primary);
    }

    /**
     * Requests the API with the recorded audio and updated the feedback when a response is received
     */
    private void uploadRecordingAndUpdateFeedbackOnResponse() {
        try {
            String result = new FileUploader().execute().get();
            Gson gson = new Gson();
            Map<String, String> asMap = gson.fromJson(result, Map.class);
            if (asMap.containsKey("status") && Objects.equals(asMap.get("status"), "OK")) {
                // We need to run setText on UI thread to avoid exception
                showFeedback(asMap.get("result"));
            }
        } catch (Exception e) {
            // We need to run setText on UI thread to avoid exception
            this.runOnUiThread(() -> responseTxt.setText(R.string.exceptionDuringUpload));
        }
    }

    /**
     * Shows feedback to the user depending on the predicted phonemes
     */
    private void showFeedback(String result) {
        boolean specialCasePresent = specialFeedback(result);
        if (!specialCasePresent) {
            if (currentWord.second.equals(result)) {
                this.runOnUiThread(() -> responseTxt.setText(getString(R.string.correctPronunciation, currentWord.first.toLowerCase())));
            } else {
                this.runOnUiThread(() -> responseTxt.setText(getString(R.string.incorrectPronunciation, currentWord.first.toLowerCase())));
            }
        }
    }

    /**
     * Calls special feedback case any case applies to the current word and the pronunciation result
     */
    private boolean specialFeedback(String result) {
        for (Triple<String, String, String> specialCase : specialFeedbackCases) {
            String[] wordPhonemes = currentWord.second.split(" ");
            String[] resultPhonemes = result.split(" ");
            if (Arrays.stream(wordPhonemes).anyMatch((s) -> s.equals(specialCase.getFirst()))
                    && Arrays.stream(resultPhonemes).anyMatch((s) -> s.equals(specialCase.getSecond()))
                    && currentWord.second.indexOf(specialCase.getFirst()) == result.indexOf(specialCase.getSecond())) {
                this.runOnUiThread(() -> responseTxt.setText(getString(R.string.incorrectPronunciationSpecialCase, specialCase.getThird())));
                return true;
            }
        }
        return false;
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
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{RECORD_AUDIO, INTERNET, WRITE_EXTERNAL_STORAGE},
                REQUEST_AUDIO_PERMISSION_CODE);
    }

    private void startRecording() {
        if (CheckPermissions()) {
            prepareRecorder();

            responseTxt.setVisibility(View.GONE);
            earImage.setVisibility(View.VISIBLE);
            earImage.setAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.pulse));

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

    private void prepareRecorder() {
        mRecorder = new MediaRecorder();

        // Set-up of the recorder to ensure correct format
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(AudioFormat.ENCODING_PCM_16BIT);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        mRecorder.setOutputFile(mFileName);
    }

    /**
     * Plays the audio recorded by the user.
     * ToDo: Currently commented out, as it might be ideal for the user to listen to their own pronunciation.
     */
/*    public void playAudio() {
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
*/

    /**
     * Stop the recording and upload it to the API
     */
    public void stopRecording() {
        if (null == mRecorder) {
            return;
        }

        earImage.clearAnimation();
        earImage.setVisibility(View.GONE);
        responseTxt.setText(R.string.gettingResource);
        responseTxt.setVisibility(View.VISIBLE);

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
                } finally {
                    setButtonStylingAndText(recordBtn, R.drawable.rounded_corners_primary, R.string.record);
                }
                uploadRecordingAndUpdateFeedbackOnResponse();
            }
        }, 500);
    }

    private void setButtonStylingAndText(Button btn, int style, int textResource) {
        btn.setBackgroundResource(style);
        btn.setText(textResource);
    }

    private View.OnTouchListener getButtonTouchListener() {
        return (v, event) -> {
            v.performClick();
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    // Fade button on press
                    setButtonStylingAndText(recordBtn, R.drawable.rounded_corners_faded, R.string.recording);
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
        return v -> {
            fluentPlayer.start();
            listenBtn.setBackgroundResource(R.drawable.rounded_corners_faded);
            fluentPlayer.setOnCompletionListener(mediaPlayer -> listenBtn.setBackgroundResource(R.drawable.rounded_corners_primary));
        };
    }


    private View.OnClickListener getButtonClickListener() {
        // Used to ensure the app doesn't crash when the user clicks button instead of holding it down
        return null;
    }
}
