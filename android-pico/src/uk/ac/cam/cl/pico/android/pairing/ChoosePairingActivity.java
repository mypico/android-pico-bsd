/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.pairing;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.core.AcquireCodeActivity;
import uk.ac.cam.cl.pico.android.data.SafeService;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * TODO
 * @author Max Spencer <ms955@cl.cam.ac.uk>
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 */
public abstract class ChoosePairingActivity extends Activity {
 
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.choose_pairing, menu);
        return true;
    }

    /**
     * TODO
     */
    protected void hideSpinner() {
        final ProgressBar spinner = (ProgressBar) findViewById(
                R.id.choose_pairing_activity__spinner);
        spinner.setVisibility(View.GONE);
    }

    /**
     * Display Toast to user when Pico doesn't have pairings with the service.
     */
    protected void showNoPairingsToast() {
    	final SafeService service = getIntent().getParcelableExtra(AcquireCodeActivity.SERVICE);
    	
    	// What should we show as the service name?
    	String serviceName = service.getName();
    	// Don't rely on service name being set
    	if (serviceName == null || serviceName.equals("")) {
    		serviceName = getString(R.string.this_service);
    	}
    	
    	// Show the toast
    	Toast.makeText(
        		getApplicationContext(),
        		getString(R.string.no_pairings_with, serviceName),
        		Toast.LENGTH_LONG).show();
        finish();
    }
}