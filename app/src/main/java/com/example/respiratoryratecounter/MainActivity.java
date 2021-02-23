package com.example.respiratoryratecounter;

import android.app.Dialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private ConstraintLayout circle;
    private Button resetButton, manualInput;
    private TextView elapsedTime;
    private ArrayList<Long> durations;
    private long lastBreath;
    private double value;
    private int numBreaths, margin, score, birthday;
    private String ifFastBreathing,ifNormalBreathing;
    private boolean fastBreathing;
    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        numBreaths = 5;
        margin=13;
        score = 0;
        durations = new ArrayList<>();
        newTimer();
        lastBreath=-1;

        circle = findViewById(R.id.Circle);
        manualInput = findViewById(R.id.ManualInput);
        resetButton = findViewById(R.id.ResetButton);
        elapsedTime = findViewById((R.id.ElapsedTime));

        circle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.circle_animation));
                ((TextView) view.findViewById(R.id.TapOnInhale)).setText(R.string.tap_on_inhale);
                breathTaken();
                if (validateDataCollection()) {
                    value = getBreathRate(numBreaths);
                    completeMeasuring();
                }
            }
        });

        manualInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Dialog dialog = new Dialog(MainActivity.this);
                dialog.requestWindowFeature(MainActivity.this.getWindow().FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.manual_input_resp_rate);

                final EditText editText = dialog.findViewById(R.id.EditText);
                Button submitButton = dialog.findViewById(R.id.SubmitButton);

                submitButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (editText.getText().toString().isEmpty()) {
                            return;
                        }
                        String typedText = editText.getText().toString();
                        value = Double.parseDouble(typedText);
                        dialog.dismiss();
                        evalFastBreathing();
                        showDialog();
                    }
                });
                dialog.show();
            }
        });

    }

    /**
     * shows popup with alert explaining RR and if child is experiencing fast breathing
     */
    public void showDialog(){
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.requestWindowFeature(MainActivity.this.getWindow().FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.respiratory_rate_result_dialog_layout);

        TextView respRateNum = dialog.findViewById(R.id.RespRateNum);
        TextView condition = dialog.findViewById(R.id.FastBreathing);
        TextView breathInfo = dialog.findViewById(R.id.breathingInfo);

        NumberFormat formatter = new DecimalFormat("#0");
        respRateNum.setText(formatter.format(value));

        if(fastBreathing){

            condition.setText(R.string.fast_breathing);
            condition.setTextColor(MainActivity.this.getResources().getColor(R.color.red));
        } else {
            condition.setText(R.string.normal_breathing);
            condition.setTextColor(MainActivity.this.getResources().getColor(R.color.primaryButtonColor));
        }
        if (birthday<2) {
            breathInfo.setText(R.string.breathing_info_under1);
        } else {
            breathInfo.setText(R.string.breathing_info_over1);
        }

        Button dialogButtonReset = dialog.findViewById(R.id.ResetButton);
        // if button is clicked, close the custom dialog
        dialogButtonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                resetRespRate();
            }
        });

        Button dialogButtonContinue = dialog.findViewById(R.id.ContinueButton);
        // if button is clicked, close the custom dialog
        dialogButtonContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
//                afterClick();
            }
        });

        dialog.show();
    }


    public String millisecondsToString(long millis){
        String  ms = (TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)))+
                ":"+ (TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
        return ms;
    }

    public void updateElapsedTimeView(long millis){
        String b = MainActivity.this.getResources().getString(R.string.elapsed_time);
        elapsedTime.setText(b+" "+millisecondsToString(millis));
    }
    /**
     * Creates new timer to break off after one minute in worst case where taps are too inconsistent.
     */
    public void newTimer(){
        timer = new CountDownTimer(60*1000,1000) {
            @Override
            public void onTick(long l) {
                updateElapsedTimeView((60*1000)-l);
            }

            @Override
            public void onFinish() {
                if(durations.size()<5){
                    resetRespRate();
                } else {
                    value = getBreathRate(durations.size());
                    completeMeasuring();
                }
            }
        };
    }

    public void completeMeasuring(){
        resetTimer();
        evalFastBreathing();
        showDialog();
    }

    /**
     * method decides whether child is experiencing fast breathing based on RR and age.
     */
    public void evalFastBreathing(){

            if (birthday>-1) {
                if (birthday <2) {
                    if(value>=50 && value<60){
                        fastBreathing = true;
                        score = 2;
                        return;
                    }

                    if (value >=60) {
                        fastBreathing = true;
                        score = 3;
                        return;
                    }
                }
                else {
                    if (value >= 40 && value <45) {
                        fastBreathing = true;
                        score = 2;
                        return;
                    }
                    if (value >= 45) {
                        fastBreathing = true;
                        score = 3;
                        return;
                    }
                }

                fastBreathing = false;
            } else {
                //TODO
            }

    }

    public void resetRespRate(){
        ((TextView) circle.findViewById(R.id.TapOnInhale)).setText(R.string.start_text);
        durations.clear();
        lastBreath = -1;
        resetTimer();
        updateElapsedTimeView(0);
    }
    public void resetTimer(){
        timer.cancel();
        newTimer();
    }


    public void reset() {
        resetRespRate();
    }

    public double getBreathRate(int num) {
        return 60 / (getMedian(num) / 1000.0);
    }


    /**
     * determines if value is in bounds
     * @param val value
     * @param up upper bound
     * @param low lower bound
     * @return is in bounds
     */
    public boolean inBounds(long val, long up, long low) {
        return (val < up) && (val > low);
    }


    public long getMedian(int length) {
        if (length > durations.size()) {
            return -1;
        }
        if (length == 0) {
            return -1;
        }

        ArrayList<Long> sub = new ArrayList<Long>(durations.subList(durations.size() - (length), durations.size()));
        Collections.sort(sub);
        int half = (length / 2);
        if (length % 2 == 0) {
            return (sub.get(half - 1) + sub.get(half)) / 2;
        }
        return sub.get(half);
    }

    /**
     * Calculates the lower bound of what is considered a consistent breath given the margin
     * @param med median
     * @return lower bound value
     */
    public long lowerBound(long med) {
        return (long) (med * (1.0 - (margin / 100.0)));
    }

    /**
     * Calculates the upper bound of what is considered a consistent breath given the margin
     * @param med median
     * @return upper bound value
     */
    public long upperBound(long med) {
        return (long) (med * (1.0 + (margin / 100.0)));
    }

    /**
     * calculates number of consistent breaths in trailing window.
     *
     * @return number of consistent breaths
     */
    public int getValidProgress() {
        for (int ii = 1; ii <= numBreaths; ii++) {
            if (ii > durations.size()) {
                return ii - 1;
            }
            long median = getMedian(ii);
            if (median == -1) {
                return ii - 1;
            }
            long up = upperBound(median);
            long low = lowerBound(median);

            for (int jj = durations.size() - 1; jj > ((durations.size() - 1) - ii); jj--) {
                if (!inBounds(durations.get(jj), up, low)) {
                    return ii - 1;
                }
            }
        }
        return numBreaths;
    }
    public void startTimer(){
        timer.start();
    }
    /**
     * handles logic for when a breath is measured
     */
    public void breathTaken() {
        long currTime = System.currentTimeMillis();
        if (lastBreath != -1) {
            long dur = currTime - lastBreath;
            durations.add(dur);
        } else{
            startTimer();
        }

        lastBreath = currTime;
    }

    public boolean validateDataCollection() {
        return getValidProgress() >= numBreaths;
    }

    public double getValue() {
        return value;
    }

    public String getIfFastBreathing() {
        return ifFastBreathing;
    }

    public void setIfFastBreathing(String ifFastBreathing) {
        this.ifFastBreathing = ifFastBreathing;
    }

    public String getIfNormalBreathing() {
        return ifNormalBreathing;
    }

    public void setIfNormalBreathing(String ifNormalBreathing) {
        this.ifNormalBreathing = ifNormalBreathing;
    }
    public int getScore() {
        return score;
    }

}