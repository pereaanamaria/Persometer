package com.example.persometer.helpers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.persometer.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

//creates a connection between Firebase and the Android app
public class FirebaseHelper{
    private static final String TAG = "FirebaseHelper"; //for logging

    private DatabaseReference myRef, myWeightRef, myHeightRef;  //refrences to Firebase
    private FirebaseDatabase database;  //Firebase variable

    private int weight, height, steps; //variables storing Firebase data
    private User user;  //instance to a User variable

    private SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");  //formatting the current date into a dd-MM-yyyy String type

    public FirebaseHelper() {
        database = FirebaseDatabase.getInstance();  //stores Firebase instance into database variable

        //gets current date which is later formatted into dd-MM-yyyy type
        Date currentDate = Calendar.getInstance().getTime();
        String currentFormattedDate = df.format(currentDate);

        //stores a Firebase reference of the steps made during the current date, the weight and the height of the user
        myRef = database.getReferenceFromUrl("https://persometer.firebaseio.com/" + currentFormattedDate);
        myHeightRef = database.getReferenceFromUrl("https://persometer.firebaseio.com/0Height");
        myWeightRef = database.getReferenceFromUrl("https://persometer.firebaseio.com/0Weight");

        //creates a new user and reads the steps made dring the current date, the weight and the height of the user
        user = new User();
        readSteps();
        readHeight();
        readWeight();
    }

    private void readSteps() {
        //listener to the "steps" child
        myRef.child("steps").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String key = myRef.getKey();  //gets the reference key

                Log.d(TAG,"Currently reading steps that were previously saved");

                if (dataSnapshot.getValue() != null) {
                    int stepsRead = dataSnapshot.getValue(Integer.class); //gets steps from Firebase
                    user.setSavedSteps(stepsRead);  //stores the value into the user object
                    Log.d(TAG,"Saved steps dataSnapshot = " + stepsRead);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

        steps = user.getSavedSteps();  //gets the steps stored into the user object and adds them into the class attribute
        Log.d(TAG,"Saved steps = " + steps);

    }

    private void readWeight() {
        //listener to the weight reference
        myWeightRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String key = myWeightRef.getKey();  //gets the reference key

                Log.d(TAG,"Currently reading weight");

                if(dataSnapshot.getValue() != null) {
                    int weightRead = dataSnapshot.getValue(Integer.class);  //gets weight from Firebase
                    user.setBodyWeight(weightRead);  //stores weight into the user object
                    Log.d(TAG,"Weight dataSnapshot = " + weightRead);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

        weight = user.getBodyWeight();  //gets the weight stored into the user object and adds them into the class attribute
        Log.d(TAG,"Weight = " + weight);

    }

    private void readHeight() {
        //listener to the height reference
        myHeightRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String key = myHeightRef.getKey();  //gets the reference key

                Log.d(TAG,"Currently reading height");

                if(dataSnapshot.getValue() != null) {
                    int heightRead = dataSnapshot.getValue(Integer.class);  //gets height from Firebase
                    user.setHeight(heightRead);  //stores height into the user object
                    Log.d(TAG,"Height dataSnapshot = " + heightRead);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

        height = user.getHeight();  //gets the height stored into the user object and adds them into the class attribute
        Log.d(TAG,"Height = " + height);
    }

    //returns the calories calculated by the user object
    //based on the steps made, user's weight and height
    public double getCalories() { return user.getCalories(); }

    //returns the distance in km calculated by the user object
    //based on the steps made
    public double getKm() {
        return user.getKm();
    }

    public void inputSteps(int numSteps) {
        String key = myRef.push().getKey(); //gets the reference key
        steps = numSteps;  //updates the steps attribute
        myRef.child("steps").setValue(steps);  //stores the steps into Firebase
        user.setSavedSteps(steps);  //updates the savedSteps attribute from user object
    }

    public void inputWeight(int newWeight) {
        String key = myWeightRef.push().getKey(); //gets the reference key
        weight = newWeight; //updates the weight attribute
        myWeightRef.setValue(newWeight); //stores the weight into Firebase
        user.setBodyWeight(weight);  //updates the bodyWeight attribute from user object
    }

    public void inputHeight(int newHeight) {
        String key = myHeightRef.push().getKey();  //gets the reference key
        height = newHeight;  //updates the height attribute
        myHeightRef.setValue(newHeight);  //stores the height into Firebase
        user.setHeight(height);  //updates the height attribute from user object
    }

    //reads steps from Firebase and passes the value to another object type
    public int getSteps() {
        readSteps();
        Log.d(TAG, "saved steps = " + steps);
        return steps;
    }

    //reads weight from Firebase and passes the value to another object type
    public int getWeight() {
        readWeight();
        Log.d(TAG, "weight = " + weight);
        return weight;
    }

    //reads height from Firebase and passes the value to another object type
    public int getHeight() {
        readHeight();
        Log.d(TAG, "height = " + height);
        return height;
    }
}
