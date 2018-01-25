/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.pairing;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.core.AcquireCodeActivity;
import uk.ac.cam.cl.pico.android.data.ParcelableAuthToken;
import uk.ac.cam.cl.pico.android.data.SafeKeyPairing;
import uk.ac.cam.cl.pico.android.data.SafeLensPairing;
import uk.ac.cam.cl.pico.android.data.SafePairing;
import uk.ac.cam.cl.pico.android.data.SafeService;
import uk.ac.cam.cl.pico.android.data.SafeSession;
import uk.ac.cam.cl.pico.android.db.DbHelper;
import uk.ac.cam.cl.pico.android.pairing.AuthFailedDialog.AuthFailedSource;
import uk.ac.cam.cl.pico.comms.org.apache.commons.codec.binary.Base64;
import uk.ac.cam.cl.pico.crypto.AuthToken;
import uk.ac.cam.cl.pico.crypto.AuthTokenFactory;
import uk.ac.cam.cl.pico.db.DbDataAccessor;
import uk.ac.cam.cl.pico.db.DbDataFactory;

/**
 * TODO
 * 
 * @author Max Spencer <mss955@cl.cam.ac.uk>
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
final public class AuthenticateActivity extends Activity
	implements LensPairingListFragment.Listener, DialogInterface.OnDismissListener{

    static private final Logger LOGGER = LoggerFactory.getLogger(
            AuthenticateActivity.class.getSimpleName());
    static private final String SERVICE = "SERVICE";
    static private final String PAIRING = "PAIRING";
    static private final String TERMINAL_SHARED_KEY = "TERMINAL_SHARED_KEY";
    static private final String PAIRING_LIST_FRAGMENT = "PAIRING_LIST_FRAGMENT";
    
    private SafePairing pairing;    
    private SafeService service;
    private byte[] terminalSharedKey;
    private String loginForm;
    private String cookieString;
    private AuthToken token;
    
    private final IntentFilter intentFilter = new IntentFilter();
    private final ResponseReceiver responseReceiver = new ResponseReceiver();   
	
    {
        intentFilter.addAction(AuthenticateIntentService.AUTHENTICATE_TERMINAL_ACTION);
        intentFilter.addAction(AuthenticateIntentService.AUTHENTICATE_PAIRING_ACTION);
        intentFilter.addAction(AuthenticateIntentService.AUTHENTICATE_DELEGATED);
        intentFilter.addAction(LensPairingIntentService.GET_LENS_PAIRINGS_ACTION);
    }       
    
    /**
     * Broadcast receiver for receiving status updates from the KeypairingIntentService and LensPairingIntentService.
     * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
     */
    private class ResponseReceiver extends BroadcastReceiver {
    	
        @Override
        public void onReceive(final Context context, final Intent intent) {
       		if (!intent.hasExtra(AuthenticateIntentService.EXCEPTION)) {
       			if (intent.getAction().equals(AuthenticateIntentService.AUTHENTICATE_TERMINAL_ACTION)) {   
       				if (intent.hasExtra(AuthenticateIntentService.EXTRA_DATA) &&
       						intent.hasExtra(AuthenticateIntentService.TERMINAL_SHARED_KEY)) {
	        			final byte[] extraData = intent.getByteArrayExtra(AuthenticateIntentService.EXTRA_DATA);
	        			terminalSharedKey = intent.getByteArrayExtra(AuthenticateIntentService.TERMINAL_SHARED_KEY);
	
	 					try {
	 						LOGGER.debug("extraData = {}", new String(extraData));
	 						// TODO: tidy this up with a set of extra data classes
							final JSONObject obj = new JSONObject(new String(extraData));
							final String serviceAddress = obj.getString("sa");
							LOGGER.debug("sa = {}", serviceAddress);
							final String serviceCommitment= obj.getString("sc");
							LOGGER.debug("sc = {}", serviceCommitment);
							loginForm = obj.getString("lf");
							LOGGER.debug("lf = {}", loginForm);
							cookieString = obj.getString("cs");
							LOGGER.debug("cs = {}", cookieString);
							
		 					// Return the Service to authenticate to
		 					service = new SafeService(null,
		 							Base64.decodeBase64(serviceCommitment),
		 			                Uri.parse(serviceAddress),
		 			                null);
		 					
		 					// Query whether Pico is already paired with this service
		        			LOGGER.debug("Querying Pico's pairings with service {}", serviceAddress);
		        			
		        	        final Intent requestIntent;
		        	        requestIntent = new Intent(AuthenticateActivity.this, LensPairingIntentService.class);
		        	        requestIntent.putExtra(LensPairingIntentService.SERVICE, service);
		        	        requestIntent.setAction(LensPairingIntentService.GET_LENS_PAIRINGS_ACTION);
		        	        startService(requestIntent); 
						} catch (JSONException e) {
							LOGGER.error("The received extraData does not parse as JSON", e);							
					 		authenticateFailed();
						} 	
       				} else {
       					LOGGER.error("Intent {} doesn't contain required extras", intent);
				 		authenticateFailed();       					
       				}
	        	} else if (intent.getAction().equals(AuthenticateIntentService.AUTHENTICATE_TERMINAL_UNTRUSTED)) {  
	        		authenticateTerminalUntrusted();
	        	} else if (intent.getAction().equals(LensPairingIntentService.GET_LENS_PAIRINGS_ACTION)) {   
	        		if (intent.hasExtra(LensPairingIntentService.PAIRINGS)) {
	        			final ArrayList<SafeLensPairing> pairings =
	        					intent.getParcelableArrayListExtra(LensPairingIntentService.PAIRINGS);        		
	        			if (pairings.isEmpty()) {
	        				// No pairings with the service
	        				noLensPairings();
	        			} else {
	        				if (pairings.size() == 1) {
	        					// Single pairing with the service - authenticate using this pairing
	        					SafeLensPairing pairing = pairings.get(0);
	        					// TODO: Figure out a better way to manage the pairing and service class variables
	        					service = pairing.getSafeService();
	        					
	        			        OrmLiteSqliteOpenHelper helper = OpenHelperManager.getHelper(context, DbHelper.class);

	        			        try {
	        			        	// TODO: Figure out a sensible place to store the AuthToken pairings
	        			            DbDataAccessor dbDataAccessor = new DbDataAccessor(helper.getConnectionSource());
		        					Map<String, String> credentials = pairing.getLensPairing(dbDataAccessor).getCredentials();
	        			            if (credentials.size() == 1 && credentials.containsKey("AuthToken")) {
	        			            	String tokenString = credentials.get("AuthToken");
	        			            	final byte[] tokenStringBytes = Base64.decodeBase64(tokenString);
	        			            	AuthToken token = AuthTokenFactory.fromByteArray(tokenStringBytes);
	        			            	authenticateToService(token, pairing);
	        			            }
	        			            else {
	    	        					authenticateToService(pairing);
	        			            }
	        			        } catch (SQLException e) {
	        			            LOGGER.warn("Failed to connect to database");
	        			        } catch (IOException e) {
	        			            LOGGER.warn("IOException searching for pairing in database");
								}
	        				} else {
	        					// Multiple pairings with this service
	        					multipleLensPairings(pairings);
	        				}
	        			}
       				} else {
       					LOGGER.error("Intent {} doesn't contain required extras", intent);
				 		authenticateFailed(service);
       				}
	         	} else if (intent.getAction().equals(AuthenticateIntentService.AUTHENTICATE_PAIRING_ACTION)) {
	         		if (intent.hasExtra(AuthenticateIntentService.SESSION)) {
	        			final SafeSession session = intent.getParcelableExtra(AuthenticateIntentService.SESSION);
	        			LOGGER.debug("Authentication successful");  
	        			authenticateSuccess(session);
       				} else {
       					LOGGER.error("Intent {} doesn't contain required extras", intent);
				 		authenticateFailed(pairing);
       				}        			
	         	} else if (intent.getAction().equals(AuthenticateIntentService.AUTHENTICATE_PAIRING_DELEGATION_FAILED)) {  
	         		if (intent.hasExtra(AuthenticateIntentService.SESSION)) {
	        			final SafeSession session = intent.getParcelableExtra(AuthenticateIntentService.SESSION);
	        			authenticatPairingDelegationFailed(session);
       				} else {
       					LOGGER.error("Intent {} doesn't contain required extras", intent);
				 		authenticateFailed(pairing);
       				} 
	         	} else if (intent.getAction().equals(AuthenticateIntentService.AUTHENTICATE_DELEGATED)) {
	         		if (intent.hasExtra(AuthenticateIntentService.SERVICE)) {
	        			final SafeService service = intent.getParcelableExtra(AuthenticateIntentService.SERVICE);
	        			LOGGER.debug("Authentication successful");  
	        			authenticateSuccess(service);
       				} else {
       					LOGGER.error("Intent {} doesn't contain required extras", intent);
				 		authenticateFailed(service);
       				}

	         	
	         	
	         	
	         	
	         	} else {	         	
	        		LOGGER.error("Unrecognised action {}", intent.getAction());
	        		authenticateFailed(service);
	        	}
    		} else {
    	 		final Bundle extras = intent.getExtras();
        		final Throwable exception =
        				(Throwable) extras.getSerializable(PairingsIntentService.EXCEPTION);
           		LOGGER.error("Exception raise by IntentService", exception);
    			if (intent.hasExtra(AuthenticateIntentService.SESSION)) {
    				final SafeSession session =
    						(SafeSession) intent.getParcelableExtra(AuthenticateIntentService.SESSION);
    				delegateFailed(session);
    			} else {    				
    				if (service != null) {
    					authenticateFailed(service);
    				} else {
    					authenticateFailed();
    				}
    			}
    		}
        }        
    } 
    
    private void authenticatPairingDelegationFailed(final SafeSession session) {
    	LOGGER.error("Authenticating with {} failed", service);

    	// Display a dialog sourced from the service
    	DelegationFailedHere.getInstance(pairing, new ParcelableAuthToken(session.getAuthToken()))
        	.show(getFragmentManager(), "authFailedDialog");   	
    }
    
    private void authenticateToService(final SafePairing pairing) {
    	LOGGER.debug("Authenticating with service {}: pairing = {}",
					service, pairing);      
    	this.pairing = pairing;
		showAutenticatingTo(service);
		
        final Intent requestIntent =
        		new Intent(getIntent());
        		requestIntent.setClass(AuthenticateActivity.this, AuthenticateIntentService.class);
        requestIntent.putExtra(AuthenticateIntentService.PAIRING, pairing);
        requestIntent.putExtra(AuthenticateIntentService.SERVICE, service);
        requestIntent.putExtra(AuthenticateIntentService.TERMINAL_ADDRESS,
        		getIntent().getParcelableExtra(AcquireCodeActivity.TERMINAL_ADDRESS));
        requestIntent.putExtra(AuthenticateIntentService.TERMINAL_SHARED_KEY, terminalSharedKey);
        requestIntent.putExtra(AuthenticateIntentService.LOGIN_FORM, loginForm);
        requestIntent.putExtra(AuthenticateIntentService.COOKIE_STRING, cookieString);
        requestIntent.setAction(AuthenticateIntentService.AUTHENTICATE_PAIRING_ACTION);        
        startService(requestIntent);
    }
    
    private void authenticateToService(final AuthToken token, final SafePairing pairing) {
    	LOGGER.debug("Authenticating with service {}: token = {}",
					service, token.getFull());
    	this.token = token;
		showAutenticatingTo(service);
		
        final Intent requestIntent =
        		new Intent(AuthenticateActivity.this, AuthenticateIntentService.class);
        final ParcelableAuthToken authtoken = new ParcelableAuthToken(token);
        requestIntent.putExtra(AuthenticateIntentService.AUTHTOKEN, authtoken);
        requestIntent.putExtra(AuthenticateIntentService.TERMINAL_ADDRESS,
        		getIntent().getParcelableExtra(AcquireCodeActivity.TERMINAL_ADDRESS));
        requestIntent.putExtra(AuthenticateIntentService.TERMINAL_SHARED_KEY, terminalSharedKey);
        requestIntent.setAction(AuthenticateIntentService.AUTHENTICATE_DELEGATED);        
        requestIntent.putExtra(AuthenticateIntentService.PAIRING, pairing);
        requestIntent.putExtra(AuthenticateIntentService.SERVICE, service);
        startService(requestIntent);
    }
    
    private void noLensPairings() {
		LOGGER.debug("Pico is not paired with the service {}", service);
		
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
    
    private void multipleLensPairings(final ArrayList<SafeLensPairing> pairings) {
		LOGGER.debug("Pairings with {}={}", service, pairings);

		// Hid the progress spinner whilst selecting from a list of pairings
		hideSpinner();
		
		final FragmentManager fragmentManager = getFragmentManager();
		final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

		final LensPairingListFragment fragment = LensPairingListFragment.newInstance(pairings, service);
		fragmentTransaction.add(R.id.lens_pairings_fragment, fragment, PAIRING_LIST_FRAGMENT);
		fragmentTransaction.commit();
		findViewById(R.id.lens_pairings_fragment).setVisibility(View.VISIBLE);
    }
    
    private void authenticateTerminalUntrusted() {
    	LOGGER.error("Authenticating with {} failed", service);
    	
    	// Display a dialog sourced from the service
        AuthFailedHere.getInstance(service)
        	.show(getFragmentManager(), "authFailedDialog");   	
    }

    private void authenticateFailed() {
    	LOGGER.error("Authenticating failed");
    	
    	// Display a dialog sourced from the service
        AuthFailedHere.getInstance(
        	new SafeService(getString(R.string.service_unknown), null, null, null))
        	.show(getFragmentManager(), "authFailedDialog");
    }
    
    private void authenticateFailed(final AuthFailedSource source) {
    	LOGGER.error("Authenticating with {} failed", service);
    	
    	// Display a dialog sourced from the service
        AuthFailedHere.getInstance(source)
        	.show(getFragmentManager(), "authFailedDialog");
    }

    private void delegateFailed(final SafeSession session) {
    	LOGGER.error("Authenticating with {} failed", service);
    	
    	// Display a dialog sourced from the service
    	DelegationFailedHere.getInstance(pairing, new ParcelableAuthToken(session.getAuthToken()))
        	.show(getFragmentManager(), "authFailedDialog");
    }
    
    private void authenticateSuccess(final SafeSession session) {    	       
	   	// In either case, this is considered a successful authentication
		successToast(pairing.getSafeService());
		
		// Create result intent and add session
	    final Intent resultIntent = new Intent();
	    resultIntent.putExtra(SafeSession.class.getCanonicalName(), session);
	    setResult(Activity.RESULT_OK, resultIntent);
	    
	    // Finish this activity
		finish();
	}

    private void authenticateSuccess(final SafeService service) {    	       
	   	// In either case, this is considered a successful authentication
		successToast(service);
		
		// Create result intent and add service
		// TODO: This used to return a session, now it's a service. Does this matter?
	    final Intent resultIntent = new Intent();
	    resultIntent.putExtra(SafeService.class.getCanonicalName(), service);
	    setResult(Activity.RESULT_OK, resultIntent);
	    
	    // Finish this activity
		finish();
	}

    private void hideSpinner() {
        final ProgressBar spinner = (ProgressBar) findViewById(
                R.id.activity_authenticate__spinner);
        spinner.setVisibility(View.GONE);
    }
    
    private void showSpinner() {
        final ProgressBar spinner = (ProgressBar) findViewById(
                R.id.activity_authenticate__spinner);
        spinner.setVisibility(View.VISIBLE);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_authenticate);    
        
        // Register the BroadcastReceiver that receives responses from
        // the TermianlIntentService (unregistered in the onDestroy lifecycle method)
        LocalBroadcastManager.getInstance(this).registerReceiver(responseReceiver, intentFilter);          
        
        if (savedInstanceState == null) {
	        final Intent intent = getIntent();
	        if (intent.hasExtra(SafeKeyPairing.class.getCanonicalName())) {
	        	// Old style Key authenticate
	        	
	        	pairing = intent.getParcelableExtra(SafeKeyPairing.class.getCanonicalName());        	
	    		showAutenticatingTo(pairing.getSafeService());
	    		
	            final Intent requestIntent =
	            		new Intent(AuthenticateActivity.this, AuthenticateIntentService.class);
	            // Include all of the extras from the received intent to forward the terminal details
	            // if they are present
	            requestIntent.putExtras(getIntent());
	            requestIntent.setAction(AuthenticateIntentService.AUTHENTICATE_KEY_PAIRING_ACTION);        
	            startService(requestIntent);
	        } else {
	      	  	// Show the progress spinner whilst authenticating to the terminal
	            showSpinner();
	            
		        // Authenticate to the Terminal; once authenticated the channel is used to transmit the
		        // commitment and address of the service to authenticate to
		        final Intent requestIntent = new Intent(this, AuthenticateIntentService.class);
		        requestIntent.putExtra(AuthenticateIntentService.TERMINAL_COMMITMENT,
		        		getIntent().getByteArrayExtra(AcquireCodeActivity.TERMINAL_COMMITMENT));
		        requestIntent.putExtra(AuthenticateIntentService.TERMINAL_ADDRESS,
		        		getIntent().getParcelableExtra(AcquireCodeActivity.TERMINAL_ADDRESS));
		        requestIntent.setAction(AuthenticateIntentService.AUTHENTICATE_TERMINAL_ACTION);        
		        startService(requestIntent);
	        }    
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.authenticate, menu);
        return true;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
      service = savedInstanceState.getParcelable(SERVICE);
      pairing = savedInstanceState.getParcelable(PAIRING);
      terminalSharedKey = savedInstanceState.getByteArray(TERMINAL_SHARED_KEY);     
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putParcelable(SERVICE, service);
      savedInstanceState.putParcelable(PAIRING, pairing);
      savedInstanceState.putByteArray(TERMINAL_SHARED_KEY, terminalSharedKey);
    }    
    
    private void showAutenticatingTo(SafeService service) {
    	// Show the progress spinner
    	showSpinner();
    	
        // Set the "Authenticating to ..." text
        final TextView v = (TextView) findViewById(R.id.activity_authenticate__authenticating_to);
        final String t = String.format(
                getString(R.string.authenticating_to_fmt),
                service);
        v.setText(t);
    }
    
    private void successToast(SafeService service) {
        final String toastFmt = getString(R.string.auth_successful_fmt);
        final String toastMessage = String.format(toastFmt, service.getName());
        Toast.makeText(AuthenticateActivity.this,toastMessage,Toast.LENGTH_SHORT).show();       
    }    
    
    @Override
    public void onDestroy() {
    	LocalBroadcastManager.getInstance(this).unregisterReceiver(responseReceiver);
    	super.onDestroy();
    }    
    
    public static class AuthFailedHere extends AuthFailedDialog {
    	
    	public static AuthFailedHere getInstance(final AuthFailedSource source) {
    		final AuthFailedHere dialog = new AuthFailedHere();
    		
    		// Create args bundle containing the dialog source
    		final Bundle args = new Bundle();
    		args.putParcelable(SOURCE, source);
    		dialog.setArguments(args);
    		return dialog;
    	}

    	@Override
    	public Dialog onCreateDialog(final Bundle savedInstanceState) {
    		Dialog dialog = super.onCreateDialog(savedInstanceState);
    		dialog.setCanceledOnTouchOutside(true);
    		return dialog;
    	}      	
		
		@Override
    	public void onDismiss(DialogInterface dialog) {    		
    		super.onDismiss(dialog);
    		LOGGER.debug("auth failed dialog dismissed");
    		
      		// Call the activity's OnDismissListener
    		final Activity activity = getActivity();
    		if (activity != null && activity instanceof DialogInterface.OnDismissListener) {
    			((DialogInterface.OnDismissListener) activity).onDismiss(dialog);
    		}
    	}
    }
    
    public static class DelegationFailedHere extends DelegationFailedDialog {
    	 
    	public static DelegationFailedHere getInstance(
    			DelegationFailedSource source, ParcelableAuthToken token) {
    		DelegationFailedHere dialog = new DelegationFailedHere();
    		
    		// Create args bundle containing the dialog source and the token
    		final Bundle args = new Bundle();
    		args.putParcelable(SOURCE, source);
    		args.putParcelable(TOKEN, token);
    		dialog.setArguments(args);
    		
    		return dialog;    		
		}

    	@Override
    	public Dialog onCreateDialog(final Bundle savedInstanceState) {
    		Dialog dialog = super.onCreateDialog(savedInstanceState);
    		dialog.setCanceledOnTouchOutside(true);
    		return dialog;
    	}         	
		
		@Override
    	public void onDismiss(DialogInterface dialog) {
    		super.onDismiss(dialog);
    		LOGGER.debug("delegation failed dialog dismissed");
    		
      		// Call the activity's OnDismissListener
    		final Activity activity = getActivity();
    		if (activity != null && activity instanceof DialogInterface.OnDismissListener) {
    			((DialogInterface.OnDismissListener) activity).onDismiss(dialog);
    		}    		
    	}
    }

	@Override
	public void onPairingClicked(final SafePairing pairing) {
		LOGGER.info("{} selected", pairing);
		
		// Remove the LensPairingListFragment
		final FragmentManager fragmentManager = getFragmentManager();
		final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();		
        fragmentTransaction.remove(fragmentManager.findFragmentByTag(PAIRING_LIST_FRAGMENT));
        fragmentTransaction.commit();
		
		// Authenticate using this pairing
		authenticateToService(pairing);    
	}

	@Override
	public void onDismiss(final DialogInterface dialog) {
		LOGGER.debug("finishing activity");
		setResult(Activity.RESULT_CANCELED);
		finish();
	}
}