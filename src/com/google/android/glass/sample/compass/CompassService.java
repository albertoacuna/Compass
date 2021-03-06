/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.glass.sample.compass;

//import com.cubic.sensingmodule.*;

import com.google.android.glass.sample.compass.model.Landmarks;
import com.google.android.glass.sample.compass.model.Place;
import com.google.android.glass.sample.compass.util.MathUtils;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.widget.RemoteViews;

import java.util.List;

/**
 * The main application service that manages the lifetime of the compass live card and the objects
 * that help out with orientation tracking and landmarks.
 */
public class CompassService extends Service {

    private static final String LIVE_CARD_TAG = "compass";
    
    private OrientationManager mOrientationManager;
    //private SensorModule mSensorModule;
    private Landmarks mLandmarks;
    private TextToSpeech mSpeech;

    private LiveCard mLiveCard;
    private LiveCard mLiveCard2;
    private CompassRenderer mRenderer;
    private RemoteViews mRemoteViews;
    private boolean display_text = false;
    
   

    private final CompassBinder mBinder = new CompassBinder();

   

    @Override
    public void onCreate() {
        super.onCreate();

        // Even though the text-to-speech engine is only used in response to a menu action, we
        // initialize it when the application starts so that we avoid delays that could occur
        // if we waited until it was needed to start it up.
        mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // Do nothing.
            }
        });

        SensorManager sensorManager =
                (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mOrientationManager = new OrientationManager(sensorManager, locationManager);
       // mSensorModule = new SensorModule(sensorManager,locationManager);
        mLandmarks = new Landmarks(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mLiveCard == null) {
            mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
            
            mLiveCard2 = new LiveCard(this, "Info");
            mRemoteViews = new RemoteViews(getPackageName(), R.layout.info_card);
          
            Intent menuIntent2 = new Intent(this, CompassMenuActivity.class);
            menuIntent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard2.setAction(PendingIntent.getActivity(
                this, 0, menuIntent2, 0));
            
            
            
            
            mRenderer = new CompassRenderer(this, mOrientationManager, mLandmarks);

            mLiveCard.setDirectRenderingEnabled(true).getSurfaceHolder().addCallback(mRenderer);

            // Display the options menu when the live card is tapped.
            Intent menuIntent = new Intent(this, CompassMenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
            mLiveCard.attach(this);
            mLiveCard.publish(PublishMode.REVEAL);
        } else {
            mLiveCard.navigate();
        }

        return START_STICKY;
    }
    
    /**
     * A binder that gives other components access to the speech capabilities provided by the
     * service.
     */
    public class CompassBinder extends Binder {
        /**
         * Read the current heading aloud using the text-to-speech engine.
         */
        public void readHeadingAloud() {/*
            float heading = mOrientationManager.getHeading();

            Resources res = getResources();
            String[] spokenDirections = res.getStringArray(R.array.spoken_directions);
            String directionName = spokenDirections[MathUtils.getHalfWindIndex(heading)];

            int roundedHeading = Math.round(heading);
            int headingFormat;
            if (roundedHeading == 1) {
                headingFormat = R.string.spoken_heading_format_one;
            } else {
                headingFormat = R.string.spoken_heading_format;
            }

            String headingText = res.getString(headingFormat, roundedHeading, directionName);
            mSpeech.speak(headingText, TextToSpeech.QUEUE_FLUSH, null);*/
        }
        
        //Responsible for displaying text info for the location
        //Currently shown when "Info" activity is tapped
        public void displayText(){
        	//the text to be displayed and read on the screen
        	String infoText = "This location contains information displayed here";
        	
        	//you can use a different layout then compass
        	
        	
        	mLiveCard.unpublish();
        	//mLiveCard = null;
        	
        	mRemoteViews.setTextViewText(R.id.textView1, infoText);
        	mLiveCard2.setViews(mRemoteViews);
        	mLiveCard2.publish(PublishMode.REVEAL);
        	mSpeech.speak(infoText, TextToSpeech.QUEUE_FLUSH, null);
        	display_text = true;
        	
        }
        
        public void backToCompass(){
        	mLiveCard2.unpublish();
        	
        	
            mLiveCard.publish(PublishMode.REVEAL);
            display_text = false;
        }
        
        //only call the back to compass activity when displaying text livecard
        public boolean showingText(){
        	if(display_text){
        		return true;
        	}else{
        		return false;
        	}
        }
    }
    


    @Override
    public void onDestroy() {
        if (mLiveCard != null && mLiveCard.isPublished()) {
            mLiveCard.unpublish();
            mLiveCard = null;
        }

        mSpeech.shutdown();

        mSpeech = null;
        mOrientationManager = null;
        mLandmarks = null;

        super.onDestroy();
    }
}
