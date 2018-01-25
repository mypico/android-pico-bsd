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

import uk.ac.cam.cl.pico.android.data.ParcelableCredentials;
import uk.ac.cam.cl.pico.android.data.SafeKeyPairing;
import uk.ac.cam.cl.pico.android.data.SafeLensPairing;
import uk.ac.cam.cl.pico.android.data.SafePairing;
import uk.ac.cam.cl.pico.android.data.SafeService;
import uk.ac.cam.cl.pico.android.db.DbHelper;
import uk.ac.cam.cl.pico.data.pairing.KeyPairing;
import uk.ac.cam.cl.pico.data.pairing.LensPairing;
import uk.ac.cam.cl.pico.db.DbDataAccessor;
import uk.ac.cam.cl.pico.db.DbDataFactory;

/**
 * IntentService associated with the NewLensPairingActivity.
 * This service performs queries and writes data to Pico pairings database.
 * Results are returned to the calling activity through Intents broadcast
 * with the LocalBroadcastManager.
 * 
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
public class PairingsIntentService extends IntentService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PairingsIntentService.class.getSimpleName());

	static final String IS_PAIRING_PRESENT_ACTION = "IS_PAIRING_PRESENT";
	static final String PERSIST_PAIRING_ACTION = "PERSIST_PAIRING";
	static final String GET_ALL_PAIRINGS_ACTION = "GET_ALL_PAIRINGS";
	static final String DELETE_PAIRINGS_ACTION = "DELETE_PAIRINGS";
	static final String PAIRING = "PAIRING";
	static final String PAIRINGS = "PAIRINGS";
	static final String PAIRINGS_DELETED = "PAIRINGS_DELETED";
	static final String SERVICE = "SERVICE";
	static final String CREDENTIALS = "CREDENTIALS";
	static final String EXCEPTION = "EXCEPTION";
	
    private DbDataFactory dbDataFactory;
    private DbDataAccessor dbDataAccessor;  
    	
	public PairingsIntentService() {
		this(PairingsIntentService.class.getCanonicalName());
	}
	
	public PairingsIntentService(final String name) {
		super(name);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
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
	}	
	
	@Override
	protected void onHandleIntent(final Intent intent) {
		if (intent.getAction().equals(IS_PAIRING_PRESENT_ACTION)) {
			final SafeService service =
					(SafeService) intent.getParcelableExtra(SERVICE);
			final ParcelableCredentials credentials =
					(ParcelableCredentials) intent.getParcelableExtra(CREDENTIALS);
			
            // Return the result as a broadcast
			final Intent localIntent = new Intent(IS_PAIRING_PRESENT_ACTION);
			try {
                final List<LensPairing> pairings = dbDataAccessor
                		.getLensPairingsByServiceCommitmentAndCredentials(
                				service.getCommitment(), credentials.getCredentials());
                if (pairings.size() == 0) {
                    localIntent.putExtra(IS_PAIRING_PRESENT_ACTION, false);
                } else {
                    localIntent.putExtra(IS_PAIRING_PRESENT_ACTION, true);
                }
			} catch (IOException e) {
				final Bundle extras = new Bundle();
				extras.putSerializable(EXCEPTION, (Serializable) e);
				localIntent.putExtras(extras);
			} finally {
			    LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
			}
		} else if (intent.getAction().equals(PERSIST_PAIRING_ACTION)) {
			final SafeLensPairing pairing =
					(SafeLensPairing) intent.getParcelableExtra(PAIRING);
			final ParcelableCredentials credentials =
					(ParcelableCredentials) intent.getParcelableExtra(CREDENTIALS);
			
            // Return the result as a broadcast
            final Intent localIntent = new Intent(PERSIST_PAIRING_ACTION);
			try {
                final LensPairing newPairing = pairing.createLensPairing(
                        dbDataFactory, dbDataAccessor, credentials.getCredentials());
                newPairing.save();
                
                localIntent.putExtra(PERSIST_PAIRING_ACTION, true);
                localIntent.putExtra(PAIRING, new SafeLensPairing(newPairing));

			} catch (IOException e) {
				final Bundle extras = new Bundle();
				extras.putSerializable(EXCEPTION, (Serializable) e);
				localIntent.putExtra(PAIRING, pairing);
				localIntent.putExtras(extras);
            } finally {
                LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
            }
		} else if (intent.getAction().equals(GET_ALL_PAIRINGS_ACTION)) {
	        // Return the result as a broadcast
	        final Intent localIntent = new Intent(GET_ALL_PAIRINGS_ACTION);
	        
			try {
	            // Get all pairings
	            final List<LensPairing> lps = dbDataAccessor.getAllLensPairings();
	            final List<KeyPairing> kps = dbDataAccessor.getAllKeyPairings();
	
				// Compose a list of safe pairings
				final ArrayList<SafePairing> result = new ArrayList<SafePairing>();
				for (LensPairing lp : lps) {
					result.add(new SafeLensPairing(lp));
				}
			
				for (KeyPairing kp : kps) {
					result.add(new SafeKeyPairing(kp));
				}
				
				LOGGER.debug("Sending pairings {}", result);
	            localIntent.putParcelableArrayListExtra(PAIRINGS, result);
			} catch (IOException e) {
				final Bundle extras = new Bundle();
				extras.putSerializable(EXCEPTION, (Serializable) e);
				localIntent.putExtras(extras);
	        } finally {
	            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	        }
		} else if (intent.getAction().equals(DELETE_PAIRINGS_ACTION)) {
			final ArrayList<SafePairing> pairings =
					intent.getParcelableArrayListExtra(PairingsIntentService.PAIRINGS);
			final int total = pairings.size();
			int deleted = 0;
			LOGGER.debug("deleting {} pairings(s)...", total);
					
	        // Return the result as a broadcast
			final Intent localIntent = new Intent(DELETE_PAIRINGS_ACTION);
			try{ 
				for (SafePairing sp : pairings) {
					try {
						sp.getPairing(dbDataAccessor).delete();
						++deleted;
						LOGGER.info("{} deleted (note: all deleted pairings have an id of 0)", sp);
					} catch (IOException e) {
						LOGGER.warn(sp.toString() + " not deleted (IOException)", e);
					}
				}
			} finally {
				localIntent.putExtra(PAIRINGS, total);
				localIntent.putExtra(PAIRINGS_DELETED, deleted);
			    LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
			}
		} else {
			LOGGER.warn("Unrecongised action {} ignored", intent.getAction());
		}
	}
}