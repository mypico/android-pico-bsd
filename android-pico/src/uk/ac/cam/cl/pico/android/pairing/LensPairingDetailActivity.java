/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.pairing;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.core.ReattachTask;
import uk.ac.cam.cl.pico.android.data.SafeLensPairing;
import uk.ac.cam.cl.pico.android.db.DbHelper;
import uk.ac.cam.cl.pico.android.delegate.RulesActivity;
import uk.ac.cam.cl.pico.data.pairing.LensPairing;
import uk.ac.cam.cl.pico.data.pairing.LensPairingAccessor;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.google.common.base.Optional;

public class LensPairingDetailActivity extends Activity {

	private static final Logger LOGGER =
			LoggerFactory.getLogger(LensPairingDetailActivity.class.getSimpleName());
	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance();
	
	public static final String PAIRING =
			LensPairingDetailActivity.class.getCanonicalName() + "pairing";
	public static final String SHOW_ON_RESUME =
			LensPairingDetailActivity.class.getCanonicalName() + "visible";
	
	private static class LoadCredentials extends ReattachTask<SafeLensPairing, Void, Map<String, String> > {
		
		final LensPairingAccessor accessor;

		protected LoadCredentials(Activity activity, LensPairingAccessor accessor) {
			super(activity);
			
			this.accessor = checkNotNull(accessor, "accessor cannot be null");
		}

		@Override
		protected Map<String, String> doInBackground(SafeLensPairing... params) {
			try {
				final SafeLensPairing safePairing = params[0];
			
				// Retrieve the full pairing from the database
				final LensPairing pairing = safePairing.getLensPairing(accessor);
			
				// Return credentials
				return pairing.getCredentials();
			} catch (IOException e) {
				LOGGER.warn("IOException while loading credentials", e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(Map<String, String> credentials) {
			if (credentials != null) {
				// Get adapter for the credentials list view
				ArrayAdapter<String> adapter = 
						((LensPairingDetailActivity) getActivity()).getCredentialsAdapter();
				
				// Populate adapter
				adapter.clear();
				for (Map.Entry<String, String> entry: credentials.entrySet()) {
					adapter.add(entry.getKey() + ": " + entry.getValue());
				}
				adapter.notifyDataSetChanged();
			}
		}		
	}

	private SafeLensPairing pairing;
	private boolean showOnResume = false;
	private Switch credentialsSwitch;
	private ListView credentialsList;
	private ArrayAdapter<String> credentialsAdapter;
	private Optional<LensPairingAccessor> accessor = Optional.absent();
	private Button delegate;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lens_pairing_detail);
		
		// Get safe lens pairing from received intent 
		if (getIntent().hasExtra(PAIRING)) {
			pairing = (SafeLensPairing) getIntent().getParcelableExtra(PAIRING);
		} else {
			LOGGER.warn("safe pairing extra missing, finishing activity");
			finish();
		}
		
		// Set showOnResume flag if intent contains the corresponding extra
		showOnResume = getIntent().getBooleanExtra(SHOW_ON_RESUME, false);
		
		// Set text view contents
		final TextView name = (TextView) findViewById(R.id.pairing_name);
		name.setText(pairing.getDisplayName());
		if (pairing.getDateCreated().isPresent()) {
			final TextView created = (TextView) findViewById(R.id.created);
			created.setText(String.format(
					"%s: %s",
					getString(R.string.created_label),
					DATE_FORMAT.format(pairing.getDateCreated().get())));
			
		}
		
		// Set up credentials list view (initially hidden) with an adapter (initially empty).
		credentialsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		credentialsList = (ListView) findViewById(R.id.credentials_list);
		credentialsList.setAdapter(credentialsAdapter);
		
		// Set up credentials switch
		credentialsSwitch = (Switch) findViewById(R.id.credentials_switch);
		credentialsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					showCredentials();
				} else {
					hideCredentials();
				}
			}
		});
		
		delegate = (Button) findViewById(R.id.delegate_allow);
		delegate.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startDelegation(v);
			}
		});
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		try {
			accessor = Optional.of(DbHelper.getInstance(this).getLensPairingAccessor());
		} catch (SQLException e) {
			LOGGER.warn("unable to get an accessor", e);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// Show the hidden login credentials if the showOnResume flag is set and they aren't 
		// already visible (because the switch/adapter retain their state through rotations).
		if (showOnResume && !credentialsSwitch.isChecked()) {
			showCredentials();
		}
	}
	
	private ArrayAdapter<String> getCredentialsAdapter() {
		return credentialsAdapter;
	}
	
	private void showCredentials() {
		LOGGER.debug("showing credentials...");
		
		// Ensure switch is checked
		credentialsSwitch.setChecked(true);
		
		// Make the list view visible
		credentialsList.setVisibility(View.VISIBLE);
		
		// Start an async task to load the credentials into it
		if (accessor.isPresent()) {
			new LoadCredentials(this, accessor.get()).execute(pairing);
		} else {
			LOGGER.warn("no accessor, cannot load credentials from database");
		}
	}
	
	private void hideCredentials() {
		LOGGER.debug("hiding credentials...");
		
		// Ensure switch is unchecked
		credentialsSwitch.setChecked(false);
		
		// Clear contents of adapter
		credentialsAdapter.clear();
		credentialsAdapter.notifyDataSetChanged();
		
		// Hide the list view itself
		credentialsList.setVisibility(View.INVISIBLE);
	}
	
	/**
	 * Start a delegation activity to pasa a credential to another Pico
	 * @param v the view for the context
	 */
	private void startDelegation(View v) {
//		Context context = v.getContext();
//    	final Intent intent = new Intent(context, DelegateActivity.class);
//
//    	// Store the current pairing info in the intent
//    	intent.putExtra(DelegateActivity.PAIRING, pairing);
//
//    	context.startActivity(intent);

		Context context = v.getContext();
    	final Intent intent = new Intent(context, RulesActivity.class);

    	// Store the current pairing info in the intent
    	intent.putExtra(RulesActivity.PAIRING, pairing);

    	context.startActivity(intent);
	}
}
