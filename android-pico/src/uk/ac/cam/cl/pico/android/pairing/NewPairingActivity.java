/**
 * Copyright Pico project, 2016
 */

// Copyright University of Cambridge, 2014
package uk.ac.cam.cl.pico.android.pairing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.data.SafeService;
import uk.ac.cam.cl.pico.data.pairing.Pairing;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

/**
 * Activity where a user can decide whether or not to create a new {@link Pairing} with a given
 * service. There is an {@link EditText} control to allow the user to provide a human-readable name
 * of their choice for the new Pairing.
 * 
 * @author Max Spencer <ms955@cl.cam.ac.uk>
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 * 
 */
public abstract class NewPairingActivity extends Activity {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(NewPairingActivity.class.getSimpleName());

    protected SafeService service;        
    
    @Override
    protected void onCreate(final Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);               
  
        if (getIntent().hasExtra(SafeService.class.getCanonicalName())) {
	        // Get the service info and pairing type from the intent.
	        // Note: The intent is preserved across re-creation, so it is not
	        // necessary to attempt to retrieve the service info from the saved
	        // bundle.
	        service = (SafeService) getIntent().getParcelableExtra(
	        		SafeService.class.getCanonicalName());
            LOGGER.debug("Got service {} info from intent", service);            
                  
            setContentView(R.layout.activity_new_pairing);
            
            // Add autocomplete for conventional pairing names
            final AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(
            		R.id.new_pairing_activity__new_pairing_name);
            final String[] pairingCategorySuggestions =
                    getResources().getStringArray(R.array.pairing_category_names);
            final ArrayAdapter<String> adapter =
                    new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                            pairingCategorySuggestions);
            textView.setAdapter(adapter);            
        } else {
            LOGGER.error("Failed to get service from intent");
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.new_pairing, menu);
        return true;
    }    
    
    /*
     * onClick event for the confirm button
     */
    public abstract void confirmNewPairing(final View view);

    /*
     * onClick event for the cancel button
     */
    public void cancelNewPairing(final View view) {
        finish();
    }   
}