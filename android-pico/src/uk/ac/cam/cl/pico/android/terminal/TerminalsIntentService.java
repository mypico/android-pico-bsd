/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.terminal;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyPair;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.android.data.NonceParcel;
import uk.ac.cam.cl.pico.android.data.ParcelableTerminal;
import uk.ac.cam.cl.pico.android.data.SafeService;
import uk.ac.cam.cl.pico.android.db.DbHelper;
import uk.ac.cam.cl.pico.comms.JsonMessageSerializer;
import uk.ac.cam.cl.pico.comms.RendezvousSigmaProxy;
import uk.ac.cam.cl.pico.crypto.CryptoFactory;
import uk.ac.cam.cl.pico.crypto.NewSigmaProver;
import uk.ac.cam.cl.pico.crypto.Nonce;
import uk.ac.cam.cl.pico.crypto.ProtocolViolationException;
import uk.ac.cam.cl.pico.crypto.NewSigmaProver.ProverAuthRejectedException;
import uk.ac.cam.cl.pico.crypto.NewSigmaProver.VerifierAuthFailedException;
import uk.ac.cam.cl.pico.data.terminal.Terminal;
import uk.ac.cam.cl.pico.db.DbDataAccessor;
import uk.ac.cam.cl.pico.db.DbDataFactory;
import uk.ac.cam.cl.rendezvous.RendezvousChannel;

/**
 * IntentService associated with the TerminalsFragment.
 * This service performs queries and writes data to Pico terminals database.
 * Results are returned to the calling activity through Intents broadcast
 * with the LocalBroadcastManager.
 * 
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
public class TerminalsIntentService extends IntentService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TerminalsIntentService.class.getSimpleName());

	public static final String IS_TERMINAL_PRESENT_ACTION = "IS_TERMINAL_PRESENT";
	public static final String ADD_TERMINAL_ACTION = "ADD_TERMINAL";
	public static final String DELETE_TERMINAL_ACTION = "DELETE_TERMINAL";
	public static final String UPDATE_TERMINALS_ACTION = "UPDATE_TERMINALS";
	public static final String NAME = "NAME";
	public static final String COMMITMENT = "COMMITMENT";
	public static final String ADDRESS = "ADDRESS";
	public static final String NONCE = "NONCE";
	public static final String TERMINALS = "TERMINALS";
	public static final String TERMINALS_DELETED = "TERMINALS_DELETED";
	public static final String EXCEPTION = "EXCEPTION";
	
    private DbDataFactory dbDataFactory;
    private DbDataAccessor dbDataAccessor;  
    	
	public TerminalsIntentService() {
		this(TerminalsIntentService.class.getCanonicalName());
	}
	
	public TerminalsIntentService(final String name) {
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
		if (intent.getAction().equals(IS_TERMINAL_PRESENT_ACTION)) {
			isTerminalPresent(intent);
		} else if (intent.getAction().equals(ADD_TERMINAL_ACTION)) {
			addTerminals(intent);				
		} else if (intent.getAction().equals(DELETE_TERMINAL_ACTION)) {
			deleteTerminals(intent);
		} else if (intent.getAction().equals(UPDATE_TERMINALS_ACTION)) {
			updateTerminals(intent);
		}else {
			LOGGER.warn("Unrecongised action {} ignored", intent.getAction());
		}
	}
	
	private void deleteTerminals(final Intent intent) {
		final List<ParcelableTerminal> terminals = intent.getParcelableArrayListExtra(TERMINALS);	
		final int total = terminals.size();
		int i = 1;
		int deleted = 0;
		LOGGER.debug("deleting {} terminal(s)...", total);
			
        // Return the result as a broadcast
		final Intent localIntent = new Intent(DELETE_TERMINAL_ACTION);
		try {
			for (ParcelableTerminal t : terminals) {
				LOGGER.debug("Deleting terminal {}", t.getName());
				LOGGER.debug("{} of {}...", i++, total);
				
				try {
					final Terminal tToDel = dbDataAccessor.getTerminalById(t.getId());
					tToDel.delete();
					++deleted;
					LOGGER.info("{} deleted (note: all deleted terminals have an id of 0)", t);
				} catch (IOException e) {
					LOGGER.warn(t.toString() + " not deleted (IOException)", e);
				}
			}
		} finally {
			localIntent.putExtra(TERMINALS, total);
			localIntent.putExtra(TERMINALS_DELETED, deleted);
		    LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
		}
	}
	
	private void addTerminals(final Intent intent) {
		final Uri newTerminalAddress = (Uri) intent.getParcelableExtra(ADDRESS);
		final byte[] newTerminalCommitment =
				(byte[]) intent.getByteArrayExtra(COMMITMENT);
		final String newTerminalName = (String) intent.getStringExtra(NAME);
		final Nonce newTerminalNonce =
				((NonceParcel) intent.getParcelableExtra(NONCE)).getNonce();
		
		// Create new Pico keys for the terminal pairing
        final KeyPair picoKeyPair = CryptoFactory.INSTANCE.ecKpg().generateKeyPair();
        
        // Create new Terminal instance
        final Terminal newTerminal = new Terminal(
        		dbDataFactory, newTerminalName, newTerminalCommitment, picoKeyPair);

        // Return the result as a broadcast
		final Intent localIntent = new Intent(ADD_TERMINAL_ACTION);			
		try {
	        // Convert newTerminalAddress from Uri to URL
	        final URL terminalUrl = SafeService.UriToURI(newTerminalAddress).toURL();
			
			// Make a proxy for the terminal's sigma verifier to handle communication via the
			// rendezvous point.
			final RendezvousSigmaProxy proxy = new RendezvousSigmaProxy(
					new RendezvousChannel(terminalUrl),
					new JsonMessageSerializer());
			
			// Make the sigma prover which will authenticate to the terminal and send the required
			// nonce.
			final NewSigmaProver prover = new NewSigmaProver(
					NewSigmaProver.VERSION_1_1,
					new KeyPair(newTerminal.getPicoPublicKey(), newTerminal.getPicoPrivateKey()),
					newTerminalNonce.getValue(),
					proxy,
					newTerminal.getCommitment());
			
			// Carry out the sigma protocol with the terminal
			try {
				prover.prove();
				
				// If no exceptions are thrown above, the authentication was successful, so save
				// the new Terminal record.
				try {
					newTerminal.save();
				} catch (IOException e) {
					LOGGER.warn("failed to save new terminal pairing", e);
					throw e;
				}
			} catch (IOException e) {
				LOGGER.warn("failed to create new terminal pairing (IOException)", e);
				throw e;
			} catch (ProverAuthRejectedException e) {
				LOGGER.warn("failed to create new terminal pairing (rejected)", e);
				throw e;
			} catch (ProtocolViolationException e) {
				LOGGER.warn("failed to create new terminal pairing (protocol violation)", e);
				throw e;
			} catch (VerifierAuthFailedException e) {
				LOGGER.warn("failed to create new terminal pairing (verifier auth)", e);
				throw e;
			} 
		} catch (MalformedURLException e) {
			LOGGER.warn("Unable to convert newTerminalAddress from Uri to URL");
			final Bundle extras = new Bundle();
			extras.putSerializable(EXCEPTION, (Serializable) e);
			localIntent.putExtras(extras);
		} catch (Exception e) {
			final Bundle extras = new Bundle();
			extras.putSerializable(EXCEPTION, (Serializable) e);
			localIntent.putExtras(extras);
		} finally {			
		    LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
		}	
	}
	
	private void updateTerminals(final Intent intent) {
        // Return the result as a broadcast
		final Intent localIntent = new Intent(UPDATE_TERMINALS_ACTION);		
		try {
			final List<Terminal> terminals = dbDataAccessor.getAllTerminals();
			final ArrayList <ParcelableTerminal> parcelableTerminals = new ArrayList<ParcelableTerminal>();
			for (Terminal t : terminals) {
				parcelableTerminals.add(new ParcelableTerminal(t));
			}
			localIntent.putParcelableArrayListExtra(TERMINALS, parcelableTerminals);
		} catch (IOException e) {
			final Bundle extras = new Bundle();
			extras.putSerializable(EXCEPTION, (Serializable) e);
			localIntent.putExtras(extras);
		} finally {
		    LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
		}
	}
	
	private void isTerminalPresent(final Intent intent) {
		final byte[] commitment =
				(byte[]) intent.getByteArrayExtra(COMMITMENT);
		
        // Return the result as a broadcast
		final Intent localIntent = new Intent(IS_TERMINAL_PRESENT_ACTION);
		try {
            final Terminal terminal =
            		dbDataAccessor.getTerminalByCommitment(commitment);
            if (terminal == null) {
                localIntent.putExtra(IS_TERMINAL_PRESENT_ACTION, false);
            } else {
                localIntent.putExtra(IS_TERMINAL_PRESENT_ACTION, true);
            }
		} catch (IOException e) {
			final Bundle extras = new Bundle();
			extras.putSerializable(EXCEPTION, (Serializable) e);
			localIntent.putExtras(extras);
		} finally {
		    LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
		}
	}	
}