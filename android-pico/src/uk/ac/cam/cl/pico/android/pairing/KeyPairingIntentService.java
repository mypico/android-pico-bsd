/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.pairing;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.android.data.SafeKeyPairing;
import uk.ac.cam.cl.pico.android.data.SafePairing;
import uk.ac.cam.cl.pico.android.db.DbHelper;
import uk.ac.cam.cl.pico.data.pairing.KeyPairing;
import uk.ac.cam.cl.pico.db.DbDataAccessor;
import uk.ac.cam.cl.pico.db.DbDataFactory;

/**
 * IntentService associated with the NewKeyPairingActivity.
 * This service performs queries and writes data to Pico pairings database.
 * Results are returned to the calling activity through Intents broadcast
 * with the LocalBroadcastManager.
 * 
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
public class KeyPairingIntentService extends IntentService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(KeyPairingIntentService.class.getSimpleName());

	static final String PERSIST_PAIRING_ACTION = "PERSIST_PAIRING";
	static final String DELETE_PAIRING_ACTION = "DELETE_PAIRING";
	static final String GET_ALL_KEY_PAIRINGS_ACTION = "GET_ALL_KEY_PAIRINGS";
	static final String PAIRING = "PAIRING";
	static final String PAIRINGS = "PAIRINGS";
	static final String SERVICE = "SERVICE";
	static final String EXCEPTION = "EXCEPTION";
	
    private DbDataFactory dbDataFactory;
    private DbDataAccessor dbDataAccessor;  
    	
	public KeyPairingIntentService() {
		this(KeyPairingIntentService.class.getCanonicalName());
		LOGGER.debug("NewKeyPairingIntentService() done");
	}
	
	public KeyPairingIntentService(final String name) {
		super(name);
		LOGGER.debug("NewKeyPairingIntentService(String) done");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		LOGGER.debug("onCreate");
		
	    // Ormlite helper
        final OrmLiteSqliteOpenHelper helper =
        		OpenHelperManager.getHelper(this, DbHelper.class);
        try {
            dbDataFactory = new DbDataFactory(helper.getConnectionSource());
            dbDataAccessor = new DbDataAccessor(helper.getConnectionSource());
        } catch (SQLException e) {
            LOGGER.error("Failed to connect to database");
            throw new RuntimeException(e);
        }
        LOGGER.debug("done");
	}	
	
	@Override
	protected void onHandleIntent(final Intent intent) {
		LOGGER.debug("onHandleIntent");
		 if (intent.getAction().equals(PERSIST_PAIRING_ACTION)) {
			 if (intent.hasExtra(PAIRING)) {
				final SafeKeyPairing pairing =
						(SafeKeyPairing) intent.getParcelableExtra(PAIRING);
	
	            // Return the result as a broadcast
	            final Intent localIntent = new Intent(PERSIST_PAIRING_ACTION);
				try {
	                final KeyPairing newPairing = pairing.createKeyPairing(
	                        dbDataFactory, dbDataAccessor);
	                newPairing.save();
	
	                localIntent.putExtra(PERSIST_PAIRING_ACTION, true);
	                localIntent.putExtra(PAIRING, new SafeKeyPairing(newPairing));
				} catch (IOException e) {
					final Bundle extras = new Bundle();
					extras.putSerializable(EXCEPTION, (Serializable) e);
					localIntent.putExtras(extras);
	            } finally {
	                LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	            }
			} else {
				LOGGER.error("Intent {} doesn't contain required extras", intent);
			}
		} else if (intent.getAction().equals(GET_ALL_KEY_PAIRINGS_ACTION)) {
            // Return the result as a broadcast
            final Intent localIntent = new Intent(GET_ALL_KEY_PAIRINGS_ACTION);
            
			try {
	            // Get all pairings
	            final List<KeyPairing> kps = dbDataAccessor.getAllKeyPairings();

				// Compose a list of safe pairings
				final ArrayList<SafePairing> result = new ArrayList<SafePairing>(kps.size());
				for (KeyPairing kp : kps) {
					result.add(new SafeKeyPairing(kp));
				}
				
                localIntent.putParcelableArrayListExtra(PAIRINGS, result);
			} catch (IOException e) {
				final Bundle extras = new Bundle();
				extras.putSerializable(EXCEPTION, (Serializable) e);
				localIntent.putExtras(extras);
            } finally {
                LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
            }
		} else {				
			LOGGER.warn("Unrecongised action {} ignored", intent.getAction());
		}
	}
}