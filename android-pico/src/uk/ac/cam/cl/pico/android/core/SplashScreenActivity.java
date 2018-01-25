/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.core;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.setup.SetupActivity;

/**
 * Splash screen, displayed on first running the Pico application.
 * 
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
final public class SplashScreenActivity extends Activity {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            SplashScreenActivity.class.getSimpleName());
    
    // Splash screen timer in ms
    private static int SPLASH_TIME_OUT = 2000;
    // Key indicating first run of the Pico app
    private static final String PICO_UUID_KEY = "PICO_UUID";   
    // "What" field of empty Message, presence of this message indicates that the splash screen
    // is showing
    private static final int SPLASH_SCREEN_SHOWING = 0;

    private Handler handler;
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
    	
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String picoUuid = preferences.getString(PICO_UUID_KEY, "");
        handler = new Handler();
        
        if(isNullOrEmpty(picoUuid)) {
         	            
        	if(!handler.hasMessages(SPLASH_SCREEN_SHOWING)) {	        		  
               	// The Pico application has not been configured
        		
	            LOGGER.info("Configuring Pico...");                        
	            handler.postDelayed(new Runnable() {
	                
	                @Override
	                public void run() {
	                   
	                	handler.removeMessages(SPLASH_SCREEN_SHOWING);
	                	
	                    // Launch the setup Activity
	                    LOGGER.info("Launching setup activity..."); 

	                	final Intent intent = new Intent(SplashScreenActivity.this, SetupActivity.class);
	                    startActivityForResult(intent, SetupActivity.SETUP_RESULT_CODE);
	                }
	            }, SPLASH_TIME_OUT);
	            handler.sendEmptyMessage(SPLASH_SCREEN_SHOWING);
        	}
        } else {
        	// Pico is already configured
        	LOGGER.debug("Pico is already configured");
            whenConfigured();
        }
    }   
    
    @Override
    protected void onActivityResult(
    		final int requestCode, final int resultCode, final Intent result) {
        super.onActivityResult(requestCode, resultCode, result);
        
        if (requestCode == SetupActivity.SETUP_RESULT_CODE) {       
            if (resultCode == RESULT_OK) {
            	// The backup has been configured, therefore, start using Pico                

                // Generate a UUID for the application at first run
                final UUID newPicoUuid = UUID.randomUUID();

                // Store the UUID indicating the Pico is configured in the SharedPreferences  
                final SharedPreferences preferences =
                        PreferenceManager.getDefaultSharedPreferences(this);                
                final SharedPreferences.Editor editor = preferences.edit();
                editor.putString(PICO_UUID_KEY, newPicoUuid.toString());
                editor.commit();                  

                LOGGER.debug("Configuration complete");
                whenConfigured();
            } else if (resultCode == RESULT_CANCELED) {                
                // Close this activity
                finish();
            }
        }
    }

    @Override
    public void onStop() {
    	super.onStop();
    	handler.removeCallbacksAndMessages(null);
    }
       
    private void whenConfigured() {
    	LOGGER.debug("Pico is configured, starting AcquireCodeActivity...");
    	final Intent intent = new Intent(getIntent());
    	intent.setClass(this, AcquireCodeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}