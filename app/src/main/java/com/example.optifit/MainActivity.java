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
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.INTERNET;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class MainActivity extends AppCompatActivity {
    private TextView wordTxt;
    private Button listenBtn;
    private TextView responseTxt;
    private ImageView earImage;
    private Button recordBtn;

    // Sound recording
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;
    private MediaRecorder mRecorder;
    private static String mFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AudioRecording.mp4";

    private MediaPlayer fluentPlayer; // For example sound
    private boolean recordingStarted = false;

    Map<String, String> wordList = new Hashtable<>();
    String currentWord;

    @SuppressLint({"RestrictedApi", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_main);

        while (!CheckPermissions()) RequestPermissions();

        fluentPlayer = MediaPlayer.create(this, R.raw.paere);

        wordTxt = findViewById(R.id.wordTxt);

        parseWordList();
        currentWord = wordList.keySet().stream()
                .filter((w) -> w.equals("Pære")) // Ensures that "Pære" is displayed, should be removed in final product
                .findFirst().orElse("Ord mangler");
        wordTxt.setText(currentWord.contains(" ") ? currentWord : '"' + currentWord + '"');
        wordTxt.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);

        recordBtn = findViewById(R.id.recordbtn);
        recordBtn.setOnTouchListener(getButtonTouchListener());
        recordBtn.setOnClickListener(getButtonClickListener());
        recordBtn.setBackgroundResource(R.drawable.rounded_corners_primary);

        listenBtn = findViewById(R.id.listenBtn);
        listenBtn.setOnClickListener(getListenButtonClickListener());
        listenBtn.setBackgroundResource(R.drawable.rounded_corners_primary);

        responseTxt = findViewById(R.id.responseTxt);
        earImage = findViewById(R.id.earImage);
    }

    private void parseWordList() {
        Resources res = getResources();
        TypedArray wordArray = res.obtainTypedArray(R.array.word_list);
        int n = wordArray.length();

        for (int i = 0; i < n; ++i) {
            int id = wordArray.getResourceId(i, 0);
            if (id > 0) {
                wordList.put(res.getStringArray(id)[0], res.getStringArray(id)[1]);
            } else {
                // ToDo: Handle something wrong with the XML
            }
        }

        wordArray.recycle(); // Important!
    }

    private View.OnClickListener getButtonClickListener() {
        // Used to ensure the app doesn't crash when the user clicks button instead of holding it down
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
            if (result.equals(wordList.get(currentWord))) { // Gets the phonemes
                this.runOnUiThread(() -> responseTxt.setText(getResources().getString(R.string.correctPronunciation, currentWord)));
            } else {
                this.runOnUiThread(() -> responseTxt.setText(getResources().getString(R.string.incorrectPronunciation, currentWord)));
            }
        }
    }

    /**
     * Calls special feedback case if relevant
     */
    private boolean specialFeedback(String result) {
        String[] phonemes = result.split(" ");
        if (currentWord.equals("pære")) {
            if (phonemes[0].equals("p")) {
                this.runOnUiThread(() -> responseTxt.setText(R.string.incorrectPronunciationPtoB));
                return true;
            }
            if (!phonemes[0].equals("pʰ")) {
                this.runOnUiThread(() -> responseTxt.setText(R.string.incorrectPronunciationP));
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
            // Initialization of filename to the path of the recorded audio file
            mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
            mFileName += "/AudioRecording.mp4";

            mRecorder = new MediaRecorder();

            // Set-up of the recorder to ensure correct format
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(AudioFormat.ENCODING_PCM_16BIT);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            mRecorder.setOutputFile(mFileName);

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

    /**
     * Plays the audio recorded by the user
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
}
