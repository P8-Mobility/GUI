package com.example.optifit.ui.practice;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.optifit.R;
import com.example.optifit.ui.SharedViewModel;

import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;


import org.json.JSONException;
import org.json.JSONObject;


public class PracticeFragment extends Fragment {


    private SharedViewModel model;
    private ConstraintLayout root;
    private Button recordBtn;
    private Boolean isCollectingData = false;
    private MediaRecorder mRecorder;

    @SuppressLint("ClickableViewAccessibility")
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        model = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        root = (ConstraintLayout) inflater.inflate(R.layout.fragment_exercise, container, false);
        recordBtn = (Button) root.findViewById(R.id.recordbtn);
        recordBtn.setOnTouchListener(getButtonTouchListener(recordBtn));

        setRestRequestResponseListener();
        setRestRequestResponseErrorListener();

        return root;
    }



    private View.OnTouchListener getButtonTouchListener(Button recordBtn) {
        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        // Start collecting data
                        model.dataHandler.enableDataGathering();

                        // Visualize data collection
                        PracticeFragment.this.recordBtn.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                        PracticeFragment.this.recordBtn.setText(R.string.listen);
                        ((TextView) root.findViewById(R.id.exercise_prediction)).setTextColor(Color.BLACK);
                        ((TextView) root.findViewById(R.id.error_on_classification)).setText("");
                        ((TextView) root.findViewById(R.id.exercise_prediction)).setText(R.string.listen);

                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        // Stop collecting data
                        model.dataHandler.disableDataGathering();

                        PracticeFragment.this.recordBtn.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.purple_500)));
                        PracticeFragment.this.recordBtn.setText(R.string.record_repetition);
                        ((TextView) root.findViewById(R.id.exercise_prediction)).setText(R.string.pre_prediction_exercise_text);
                        ((TextView) root.findViewById(R.id.exercise_prediction)).setTextColor(Color.BLACK);


                        //String repId = Long.toString(model.dataHandler.stamp);


                        break;
                    }
                }
                return true;
            }
        };
    }


    private void setRestRequestResponseListener() {
        model.restRequestHandler.setResponseListener(new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject result = new JSONObject(response);
                    if (result.getString("status").equals("OK") && isCollectingData) {
                        ((TextView) root.findViewById(R.id.error_on_classification)).setText("");
                        if (result.getString("exercise").equals("00000000-0000-0000-0000-000000000000")) {
                            TextView prediction = root.findViewById(R.id.exercise_prediction);
                            //prediction.setText(ExerciseConverter.getExerciseFromUUID(result.getString("exercise")));
                            prediction.setTextColor(Color.BLACK);
                        } else {
                            boolean errorResult = result.getInt("mistakes") == 0;
                            String classificationResult = errorResult ? "Good" : "Bad";
                            classificationResult += " ";
                            //classificationResult += ExerciseConverter.getExerciseFromUUID(result.getString("exercise"));

                            TextView prediction = root.findViewById(R.id.exercise_prediction);
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

}

