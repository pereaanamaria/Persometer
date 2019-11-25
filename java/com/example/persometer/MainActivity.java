package com.example.persometer;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.persometer.helpers.FirebaseHelper;
import com.example.persometer.steps.StepDetector;
import com.example.persometer.steps.StepListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener, StepListener {
    private static final String TAG = "MainActivity";

    private static final String TEXT_SAVED_STEPS = " saved";
    private static final String TEXT_EMPTY = "";
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final int GOAL = 1000;

    private boolean PersometerStarted = false;  //persometer is started flag (default: false)
    private int numSteps, saved, weight, height;

    private StepDetector simpleStepDetector;
    private FirebaseHelper FBHelper = new FirebaseHelper(); //creates a FirebaseHelper object used for Android app-Firebase communication

    private TextView TvSteps, TvSavedSteps, TvCal, TvKm, TvProgress, TvCongrats1, TvCongrats2;
    private EditText EtWeight, EtHeight;
    private Button BtnStart;
    private ProgressBar ProgBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //gets an instance of the SensorManager
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //creates a StepDetector for counting steps
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);

        TvSteps = (TextView) findViewById(R.id.tv_steps);
        TvSavedSteps = (TextView) findViewById(R.id.tv_stepsSaved);
        TvCal = (TextView) findViewById(R.id.tv_cal);
        TvKm = (TextView) findViewById(R.id.tv_km);
        TvCongrats1 = (TextView) findViewById(R.id.tv_congrats1);
        TvCongrats2 = (TextView) findViewById(R.id.tv_congrats2);
        TvProgress = (TextView) findViewById(R.id.tv_progress);
        EtWeight = (EditText) findViewById(R.id.edit_weight);
        EtHeight = (EditText) findViewById(R.id.edit_height);
        ProgBar = (ProgressBar) findViewById(R.id.progressBar);

        //registers the accelerometer's listener
        sensorManager.registerListener(MainActivity.this, accel, SensorManager.SENSOR_DELAY_FASTEST);

        //map is requested
        if (isServicesOK()) {
            initMap();   //map can be initialised through the map button
        }

        BtnStart = (Button) findViewById(R.id.btn_start);
        BtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                PersometerStarted = true;   //flag is set to true
                BtnStart.setVisibility(View.INVISIBLE);   //button becomes invisible from now on
                LinearLayout layout = (LinearLayout) findViewById(R.id.layout_all);
                layout.setVisibility(View.VISIBLE);      //the rest of the layout is visible from now on
                getInfoFromFB();     //data is being updated on the Android app
            }
        });

        Button BtnForceDBUpdate = (Button) findViewById(R.id.btn_forceDBUpdate);
        BtnForceDBUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSteps();   //steps that have been made so far are stored into Firebase
            }
        });

        Button BtnUpdate = (Button) findViewById(R.id.btn_updateData);
        BtnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    //weight input is stored into Firebase and hint is updated with the new value
                    FBHelper.inputWeight(Integer.parseInt(EtWeight.getText().toString()));
                    weight = FBHelper.getWeight();

                    //height input is stored into Firebase and hint is updated with the new value
                    FBHelper.inputHeight(Integer.parseInt(EtHeight.getText().toString()));
                    height = FBHelper.getHeight();

                    //new data and new formula results are displayed
                    displayEverything();

                    //EditTexts are cleared
                    EtWeight.getText().clear();
                    EtHeight.getText().clear();

                    Toast.makeText(MainActivity.this, "Saved settings.", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "No new weight or height settings.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //if accelerometer sensor's state changes, a possible new step is being detected
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
        }
    }

    //step counting
    @Override
    public void step(long timeNs) {
        //steps are not counted unless BtnStart was pressed
        if (PersometerStarted) {
            if (numSteps < saved) {
                numSteps = saved;   //greatest value is set as starting point in step counting
            }

            //if movement is detected, a step is being added
            numSteps++;
            changeProgress();    //ProgressBar is updated
            TvSteps.setText(TEXT_EMPTY + numSteps);    //new steps number is displayed

            //steps are stored into Firebase if numSteps is a multiple of 100
            if (numSteps % 100 == 0) {
                saveSteps();
                Log.d(TAG, "After saveSteps() from steps %50");
            }

            //steps are stored into Firebase if it is just before midnight
            String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            if (currentTime.equals("23:59:59")) {
                saveSteps();
                Log.d(TAG, "After saveSteps() from time 23:59:59");
                numSteps = 0;
            }
        }
    }

    //results are taken from Firebase and displayed
    private void getInfoFromFB() {
        if (numSteps < FBHelper.getSteps()) {
            numSteps = FBHelper.getSteps();
        }

        weight = FBHelper.getWeight();
        height = FBHelper.getHeight();

        displayEverything();
    }

    //steps are being saved
    private void saveSteps() {
        //greatest value is stored into Firebase
        if (numSteps > FBHelper.getSteps()) {
            FBHelper.inputSteps(numSteps);
            saved = numSteps;               //steps saved value is updated
        } else {
            numSteps = FBHelper.getSteps();
            saved = numSteps;               //steps saved value is updated
        }
        weight = FBHelper.getWeight();    //weight is read from Firebase
        height = FBHelper.getHeight();    //height is read from Firebase

        displayEverything();
    }

    //gets to MapsActivity when clicked
    private void initMap() {
        Button BtnGoToMap = (Button) findViewById(R.id.btn_goToMap);
        BtnGoToMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                moveTaskToBack(true);
                startActivityForResult(intent, numSteps);
            }
        });
    }

    //Google services are being checked in order to make map requests
    public boolean isServicesOK() {
        Log.d(TAG, "isServicesOK: checking google services version");
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if (available == ConnectionResult.SUCCESS) {
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            Log.d(TAG, "isServicesOK: an error occured but it can be fixed");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    //all data and calculated results are displayed
    public void displayEverything() {
        Log.d(TAG, "displayEverything() ==> weight: " + weight);
        Log.d(TAG, "displayEverything() ==> height: " + height);
        Log.d(TAG, "displayEverything() ==> steps: " + numSteps);
        Log.d(TAG, "displayEverything() ==> saved steps: " + saved);

        double cal = FBHelper.getCalories();    //gets the number of cals based on the user's saved steps, weight and height
        double km = FBHelper.getKm();           //gets the number of kms based on the user's saved steps

        TvCal.setText(String.format("%.2f", cal) + " cal");  //Displays text with the number of cals in TextView
        TvKm.setText(String.format("%.2f", km) + " km");     //Displays text with the number of kms in TextView
        TvSteps.setText(TEXT_EMPTY + numSteps);                           //Displays text of the currently made steps in TextView
        TvSavedSteps.setText(numSteps + TEXT_SAVED_STEPS);   //Displays text with the number of steps saved so far in TextView
        EtWeight.setHint(weight + " kg");                    //Sets hint in EditText of the user's current weight
        EtHeight.setHint(height + " cm");                    //Sets hint in EditText of the user's current height

        changeProgress();                   //Updates the progress in ProgressBar
    }

    private void changeProgress() {
        //Converts progress in percentage
        int percentage = (int) Math.round((numSteps * 100) / GOAL);

        Log.d(TAG, "Percentage = " + percentage);

        TvProgress.setText(percentage + "%");    //Displays the numeric percentage in TextView
        ProgBar.setProgress(percentage);         //Displays the visual percentage in ProgressBar
        if (percentage >= 100) {                 //When the goal is achieved, it displays a congratulation text
            TvCongrats1.setVisibility(View.VISIBLE);
            TvCongrats2.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        getInfoFromFB();   //updates from Firebase
    }

    protected void onRestart() {
        super.onRestart();
        getInfoFromFB();   //updates from Firebase
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSteps();   //saves to Firebase
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveSteps();   //saves to Firebase
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveSteps();   //saves to Firebase
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();    //moved to background
        moveTaskToBack(true);
    }
}
