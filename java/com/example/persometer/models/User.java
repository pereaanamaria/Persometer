package com.example.persometer.models;

import android.util.Log;

public class User {
    private static final double AVERAGE_WALKING_SPEED = 5000/3600;  // 5km/h ~= 1.38 m/s
    private static final String TAG = "User class";  //used in logging
    private int bodyWeight; //stores the weight value measured in kg
    private int height;  //stores the height value measured in cm
    private int savedSteps;  //stores the steps saved so far into Firebase value

    public User() {
        //the constructor defines the default values of the class's attributes
        bodyWeight = 55;
        height = 165;
        savedSteps = 0;
    }

    //sets bodyWeight from the outside of the class
    public void setBodyWeight(int bodyWeight) {
        this.bodyWeight = bodyWeight;
    }

    //sets height from the outside of the class
    public void setHeight(int height) {
        this.height = height;
    }

    //sets steps saved into Firebase from the outside of the class
    public void setSavedSteps(int numSteps) { this.savedSteps = numSteps; }

    //gets bodyWeight from the outside of the class
    public int getBodyWeight() {
        return bodyWeight;
    }

    //gets height from the outside of the class
    public int getHeight() {
        return height;
    }

    //sets steps savedinto Firebase from the outside of the class
    public int getSavedSteps() { return savedSteps; }

    //calculates the number of calories the user burnt while walking
    //the formula is based on the user's bodyWeight, height and steps made so far
    public double getCalories() {
        double calPerMin = (0.035 * bodyWeight) + (0.029 * bodyWeight * ((AVERAGE_WALKING_SPEED * AVERAGE_WALKING_SPEED) / height));
        double stepsPerMin = 110;

        Log.d(TAG,"Calories: bodyWeight = " + bodyWeight);
        Log.d(TAG,"Calories: height = " + height);
        Log.d(TAG,"Calories: savedSteps = " + savedSteps);

        return (calPerMin * savedSteps) / stepsPerMin; //the value is measured in cal
    }

    //calculates the number of km a user made while walking
    //the formula is based on the user's steps made so far
    public double getKm() {
        // 1 step == 0.762 m
        // 1 step == 0.762/1000 km

        Log.d(TAG,"Km: bodyWeight = " + bodyWeight);
        Log.d(TAG,"Km: height = " + height);
        Log.d(TAG,"Km: savedSteps = " + savedSteps);

        return (savedSteps * 0.762) / 1000; //the value is measured in km
    }
}
