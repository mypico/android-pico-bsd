/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.pairing;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.backup.BackupFactory;
import uk.ac.cam.cl.pico.android.backup.IBackupProvider;
import uk.ac.cam.cl.pico.android.backup.OnConfigureBackupListener;
import uk.ac.cam.cl.pico.android.data.SafeKeyPairing;
import uk.ac.cam.cl.pico.android.data.SafePairing;
import uk.ac.cam.cl.pico.android.data.SafeSession;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;

/**
 * UI for creating and naming a new Pico pairing.
 * Successfully created pairings are persisted to the Pico pairings database
 * using the NewKeyPairingIntentService.
 * 
 * @see KeyPairingIntentService
 * @author Max Spencer <ms955@cl.cam.ac.uk>
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
final public class NewKeyPairingActivity extends NewPairingActivity 
	implements OnConfigureBackupListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(NewKeyPairingActivity.class.getSimpleName());
    
    private final static int AUTHENTICATE_RESULT_CODE = 1;
    
    private ResponseReceiver mReceiver = new ResponseReceiver();    
    
    /**
     * Broadcast receiver for receiving status updates from the NewKeyPairingIntentService.
     * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
     */
    private class ResponseReceiver extends BroadcastReceiver {
    	
        @Override
        public void onReceive(final Context context, final Intent intent) {
    		if (!intent.hasExtra(KeyPairingIntentService.EXCEPTION)) {
	        	if (intent.getAction().equals(KeyPairingIntentService.PERSIST_PAIRING_ACTION)) {
					final SafeKeyPairing pairing =
							(SafeKeyPairing) intent.getParcelableExtra(KeyPairingIntentService.PAIRING);
	        		if (intent.getBooleanExtra(KeyPairingIntentService.PERSIST_PAIRING_ACTION, false)) {
	                    LOGGER.debug("Persisting new key pairing succeeded");
	
	                    final Intent authIntent = new Intent(
	                            NewKeyPairingActivity.this, AuthenticateActivity.class);
	                    authIntent.putExtra(SafeKeyPairing.class.getCanonicalName(), pairing);
	                    
	                    // Also include all of the extras from the received intent to forward the terminal
	                    // details if they are present
	                    authIntent.putExtras(getIntent());
	                    
	                    // Setting this flag means that the next activity will pass any
	                    // result back to the result target of this activity.
	                    startActivityForResult(authIntent, AUTHENTICATE_RESULT_CODE);
	        		} else {
	                    LOGGER.error("Persisting new key pairing failed");
	                    newPairingFailure();
	        		}
	        	} else {
	        		LOGGER.warn("Unrecognised action {}", intent.getAction());
	        	}
	    	} else {
	    		final Bundle extras = intent.getExtras();
	    		final Throwable exception =
	    				(Throwable) extras.getSerializable(KeyPairingIntentService.EXCEPTION);
	       		LOGGER.warn("Exception raise by IntentService {}", exception);
	       		newPairingFailure();
	    	}
        }
    	
        private void newPairingFailure() {
	        // Notify the user and finish the activity
	        Toast.makeText(
	        		NewKeyPairingActivity.this,
	        		getString(R.string.key_pairing_failed, service),
	        		Toast.LENGTH_SHORT).show();
	        NewKeyPairingActivity.this.setResult(RESULT_CANCELED);
	        finish();
        }
    }    
    
    /*
     * onClick event for the confirm button
     */
    @Override
    public void confirmNewPairing(final View view) {
    	LOGGER.debug("confirmNewPairing clicked");
    	
        // Get the user's desired human-readable pairing name
        final EditText nameEdit = (EditText) findViewById(
                R.id.new_pairing_activity__new_pairing_name);
        
        String newPairingName = nameEdit.getText().toString();
        if (newPairingName.isEmpty()) {
            newPairingName = getResources().getString(R.string.default_pairing_name);
        }
        
        LOGGER.debug("newPairingName={}", newPairingName);

        // Persist the new pairing using the NewKeyPairingIntentService
        final SafeKeyPairing emptyPairing = new SafeKeyPairing(newPairingName, service);        
        
        final Intent intent = new Intent(this, KeyPairingIntentService.class);
        intent.putExtra(KeyPairingIntentService.PAIRING, emptyPairing);
        intent.setAction(KeyPairingIntentService.PERSIST_PAIRING_ACTION);
        startService(intent);
    }        
    
    @Override
    protected void onActivityResult(
            final int requestCode, final int resultCode, final Intent data) {

        if (requestCode == AUTHENTICATE_RESULT_CODE) {                  
            // Extract the keyPairing from the Intent data
            if (data != null && data.hasExtra(SafeSession.class.getCanonicalName())) {  
                
                final SafeSession session = (SafeSession) data.getParcelableExtra(
                        SafeSession.class.getCanonicalName());                      
                final SafePairing safeNewPairing = (SafePairing) session.getSafePairing();
                
                if(resultCode == RESULT_OK) {               
                    LOGGER.info("Authentication successful");      
       
                    // Backup the pairing                                                  
                    LOGGER.info("Backing up the database with the new pairing {}", safeNewPairing);  
                    // Backup the data store containing the new Key pairing 
                	final Optional<IBackupProvider> backupProvider =
                			BackupFactory.newBackup(NewKeyPairingActivity.this); 
                	if (!backupProvider.isPresent()) {
                		LOGGER.warn("No backup provider is configured");
    	            	finish();
                	}    
                } else {    
                    // Delete the pairing
                    LOGGER.debug("Authentication failed deleting the pairing {}", safeNewPairing); 
                    
                    final Intent intent = new Intent(this, PairingsIntentService.class);
                    intent.putParcelableArrayListExtra(PairingsIntentService.PAIRING,
                    		(ArrayList<SafePairing>) Arrays.asList(safeNewPairing));
                    intent.setAction(PairingsIntentService.DELETE_PAIRINGS_ACTION);
                    startService(intent);
                }
            } else {           
               LOGGER.error("AuthenticationActivity did not return a valid session returned"); 
               setResult(Activity.RESULT_CANCELED);
               finish();
            }
        } else {
            LOGGER.error("Unrecognized requestCode");
        }
    }  
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGGER.trace("onCreate NewKeyPairingActivity");

        final IntentFilter mStatusIntentFilter = new IntentFilter();
        mStatusIntentFilter.addAction(KeyPairingIntentService.PERSIST_PAIRING_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, mStatusIntentFilter);
        
        // Update the TextView asking the user to confirm the new pairing with the name of the
        // service (service.getName())
        final TextView descTtextView =
        		(TextView) findViewById(R.id.new_pairing_activity__text1);
        final String textViewFmt = getString(R.string.activity_new_key_pairing__text1);
        final String textViewMsg = String.format(textViewFmt, service.getName());
        // Bold the Service's name
        final SpannableStringBuilder str = new SpannableStringBuilder(textViewMsg);
        str.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
        		textViewMsg.indexOf(service.getName()),
        		textViewMsg.indexOf(service.getName())+service.getName().length(),
        		Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);  
        descTtextView.setText(str);
    }      
    
    @Override
    public void onDestroy() {
    	LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    	super.onDestroy();
    }
    
    @Override
	public void onConfigureBackupSuccess(final IBackupProvider backupProvider) {
    	// Verify the method's preconditions
    	checkNotNull(backupProvider);
    	
		LOGGER.debug("Configuring backup successful");
		// Perform the backup
		backupProvider.backup();
		setResult(RESULT_OK);
		finish();
	}

	@Override
	public void onConfigureBackupCancelled() {
		LOGGER.error("Configuring backup cancelled");
		setResult(RESULT_CANCELED);
		finish();								
	}
	
	@Override
	public void onConfigureBackupFailure() {
		LOGGER.error("Configuring backup failed");
		setResult(RESULT_CANCELED);
		finish();								
	}	
}