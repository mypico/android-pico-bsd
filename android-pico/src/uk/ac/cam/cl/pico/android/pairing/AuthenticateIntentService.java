/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.pairing;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.sql.SQLException;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.LocalBroadcastManager;
import uk.ac.cam.cl.pico.android.comms.SigmaProxy;
import uk.ac.cam.cl.pico.android.core.AcquireCodeActivity;
import uk.ac.cam.cl.pico.android.core.PicoApplication;
import uk.ac.cam.cl.pico.android.core.PicoServiceImpl;
import uk.ac.cam.cl.pico.android.data.SafeKeyPairing;
import uk.ac.cam.cl.pico.android.data.SafeLensPairing;
import uk.ac.cam.cl.pico.android.data.SafeService;
import uk.ac.cam.cl.pico.android.data.SafeSession;
import uk.ac.cam.cl.pico.android.db.DbHelper;
import uk.ac.cam.cl.pico.comms.JsonMessageSerializer;
import uk.ac.cam.cl.pico.comms.RendezvousSigmaProxy;
import uk.ac.cam.cl.pico.comms.SocketCombinedProxy;
import uk.ac.cam.cl.pico.crypto.AuthToken;
import uk.ac.cam.cl.pico.crypto.LensProver;
import uk.ac.cam.cl.pico.crypto.NewSigmaProver;
import uk.ac.cam.cl.pico.crypto.NewSigmaProver.ProverAuthRejectedException;
import uk.ac.cam.cl.pico.crypto.NewSigmaProver.VerifierAuthFailedException;
import uk.ac.cam.cl.pico.crypto.ProtocolViolationException;
import uk.ac.cam.cl.pico.crypto.Prover;
import uk.ac.cam.cl.pico.crypto.messages.PicoReauthMessage;
import uk.ac.cam.cl.pico.crypto.messages.ReauthState;
import uk.ac.cam.cl.pico.crypto.messages.SequenceNumber;
import uk.ac.cam.cl.pico.data.pairing.KeyPairing;
import uk.ac.cam.cl.pico.data.pairing.LensPairing;
import uk.ac.cam.cl.pico.data.pairing.PairingNotFoundException;
import uk.ac.cam.cl.pico.data.session.Session;
import uk.ac.cam.cl.pico.data.session.SessionImpFactory;
import uk.ac.cam.cl.pico.data.terminal.Terminal;
import uk.ac.cam.cl.pico.db.DbDataAccessor;
import uk.ac.cam.cl.pico.db.DbDataFactory;
import uk.ac.cam.cl.rendezvous.RendezvousChannel;

/**
 * IntentService associated with the AuthenticateActivity.
 * Results are returned to the calling activity through Intents broadcast
 * with the LocalBroadcastManager.
 * 
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
public class AuthenticateIntentService extends IntentService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AuthenticateIntentService.class.getSimpleName());

	static final String AUTHENTICATE_PAIRING_ACTION = "AUTHENTICATE_PAIRING";
	static final String AUTHENTICATE_KEY_PAIRING_ACTION = "AUTHENTICATE_KEY_PAIRING";
	static final String AUTHENTICATE_PAIRING_DELEGATION_FAILED = "AUTHENTICATE_PAIRING_DELEGATION_FAILED";
	static final String AUTHENTICATE_TERMINAL_ACTION = "AUTHENTICATE_TERMINAL";
	static final String AUTHENTICATE_TERMINAL_UNTRUSTED = "AUTHENTICATE_TERMINAL_UNTRUSTED";	
	static final String AUTHENTICATE_DELEGATED = "AUTHENTICATE_DELEGATED";	
	static final String TERMINAL_COMMITMENT = "TERMINAL_COMMITMENT";
	static final String TERMINAL_ADDRESS = "TERMINAL_ADDRESS";
	static final String TERMINAL_SHARED_KEY = "SECRET_KEY";
	static final String LOGIN_FORM = "LOGIN_FORM";
	static final String COOKIE_STRING = "COOKIE_STRING";
	static final String SERVICE = "SERVICE";
	static final String PAIRING = "PAIRING";
	static final String SESSION = "SESSION";
	static final String AUTHTOKEN = "AUTHTOKEN";
	public static final String EXTRA_DATA = "EXTRA_DATA";
	static final String EXCEPTION = "EXCEPTION";
	
    private DbDataFactory dbDataFactory;
    private DbDataAccessor dbDataAccessor;  
    
    private Intent receivedIntent;
    
	public AuthenticateIntentService() {
		this(AuthenticateIntentService.class.getCanonicalName());
	}
	
	public AuthenticateIntentService(final String name) {
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
		receivedIntent = intent;
		if (intent.getAction().equals(AUTHENTICATE_PAIRING_ACTION)) {
			if (intent.hasExtra(PAIRING) &&
					intent.hasExtra(SERVICE) &&
					intent.hasExtra(TERMINAL_ADDRESS) && 
					intent.hasExtra(TERMINAL_SHARED_KEY) &&
					intent.hasExtra(LOGIN_FORM) &&
					intent.hasExtra(COOKIE_STRING)) {
				// Extract the pairing, service, terminal address and shared key from the Intent
				final SafeLensPairing pairing = intent.getParcelableExtra(PAIRING);
				final SafeService service = intent.getParcelableExtra(SERVICE);
				final Uri terminalAddress = intent.getParcelableExtra(TERMINAL_ADDRESS);
				final byte[] terminalSharedKey = intent.getByteArrayExtra(TERMINAL_SHARED_KEY);
				final String loginForm = intent.getStringExtra(LOGIN_FORM);
				final String cookieString = intent.getStringExtra(COOKIE_STRING);
						
				// Authenticate to the service using the specified pairing
				authenticatePairing(pairing, service, loginForm, cookieString, terminalAddress, terminalSharedKey);
			} else {
				LOGGER.error("Intent {} doesn't contain required extras", intent);
			}
		} else if (intent.getAction().equals(AUTHENTICATE_KEY_PAIRING_ACTION)) {
			if (intent.hasExtra(AcquireCodeActivity.SERVICE) &&
					intent.hasExtra(SafeKeyPairing.class.getCanonicalName())) {
				// Extract the pairing, service, terminal address and shared key from the Intent
				final SafeKeyPairing pairing = intent.getParcelableExtra(SafeKeyPairing.class.getCanonicalName());
				final SafeService service = intent.getParcelableExtra(AcquireCodeActivity.SERVICE);
				final Uri terminalAddress = intent.getParcelableExtra(AcquireCodeActivity.TERMINAL_ADDRESS);
				final String terminalCommitment = intent.getStringExtra(AcquireCodeActivity.TERMINAL_COMMITMENT);
				
				// Authenticate to the service using the specified pairing
				authenticatePairing(pairing, service, terminalAddress, terminalCommitment);
			} else {
				LOGGER.error("Intent {} doesn't contain required extras", intent);
			}
		} else if (intent.getAction().equals(AUTHENTICATE_TERMINAL_ACTION)) {
			if (intent.hasExtra(TERMINAL_ADDRESS) &&
					intent.hasExtra(TERMINAL_COMMITMENT)) {
				// Extract the terminal address and commitment from the Intent
				final Uri terminalAddress = intent.getParcelableExtra(TERMINAL_ADDRESS);
				final byte[] terminalCommitment = intent.getByteArrayExtra(TERMINAL_COMMITMENT);
				
				// Authenticate to the terminal specified by the terminalCommitment
				authenticateTerminal(terminalAddress, terminalCommitment);		
			} else {
				LOGGER.error("Intent {} doesn't contain required extras", intent);
			}
		} else if (intent.getAction().equals(AUTHENTICATE_DELEGATED)) {
			if (intent.hasExtra(AUTHTOKEN) && intent.hasExtra(TERMINAL_ADDRESS)
					&& intent.hasExtra(TERMINAL_SHARED_KEY)) {
				// Extract the authtoken, terminal address and shared key from the Intent
				final AuthToken token = intent.getParcelableExtra(AUTHTOKEN);
				final Uri terminalAddress = intent.getParcelableExtra(TERMINAL_ADDRESS);
				final byte[] terminalSharedKey = intent.getByteArrayExtra(TERMINAL_SHARED_KEY);
				final SafeService service = intent.getParcelableExtra(SERVICE);
						
				authenticateDelegated(token, terminalAddress, terminalSharedKey, service);
			} else {
				LOGGER.error("Intent {} doesn't contain required extras", intent);
			}
		} else {
			LOGGER.warn("Unrecongised action {} ignored", intent.getAction());
		}
	}	
    
	private void authenticatePairing(final SafeKeyPairing pairing, final SafeService service,
			final Uri terminalAddress, final String terminalCommitment) {
		// Return the result as a broadcast
		final Intent localIntent = new Intent(AUTHENTICATE_PAIRING_ACTION);
		
		try {
		    // Promote to a full KeyPairing
	        final KeyPairing keyPairing = pairing.getKeyPairing(dbDataAccessor);
	        if (keyPairing != null) {	        	
	        	// Instantiate and return a concrete proxy subclass based on the service address
	        	final Uri address = pairing.getSafeService().getAddress();
	        	LOGGER.info("pairing.getSafeService().getAddress(): " + address.toString());
	        	LOGGER.info("service.getAddress():" + service.getAddress());
	        	LOGGER.info("address.getHost():" + address.getHost());
	        	
	            if (address.getScheme().equals("tcp")) {
	            	final SocketCombinedProxy proxy = new SocketCombinedProxy(
		                    address.getHost(),
		                    address.getPort(),
		                    new JsonMessageSerializer());
	            	
	            	byte[] extraData = null;
					if(receivedIntent.hasExtra("myExtraData")){
						extraData = receivedIntent.getByteArrayExtra("myExtraData");
						LOGGER.debug("Extra data received was {}",receivedIntent.getByteArrayExtra("myExtraData"));
					}else{
						LOGGER.debug("No extra data was received");
					}
	            	// Construct the prover:
					final NewSigmaProver prover = new NewSigmaProver(
							NewSigmaProver.VERSION_1_1,
							new KeyPair(keyPairing.getPublicKey(), keyPairing.getPrivateKey()),
							extraData, //extra data here
							proxy,
							service.getCommitment());
		
		            final Session session;
					if (prover.prove()) {						
						session = Session.newInstanceActive(
								dbDataFactory,
                                Integer.toString(prover.getVerifierSessionId()),
                                prover.getSharedKey(),
                                keyPairing,
                                null);
						
					} else {						
						session = Session.newInstanceClosed(
								dbDataFactory,
                                Integer.toString(prover.getVerifierSessionId()),
                                keyPairing,
                                null);
					}
					
		            // TO be removed - no need to persist sessions
		            if (session.getStatus() != Session.Status.ERROR) {
		                // If the session is ok, then save it.
		                LOGGER.debug("Persisting session");
		                session.save();                                           
		            }
		         // Carry out the authentication and get the Session instance result:
		            final SafeSession safeSession = new SafeSession(session);	    
		            if (session.getStatus() == Session.Status.ACTIVE) {
		                // Start continuous authentication
		                final Intent intent = new Intent(this, PicoServiceImpl.class);
		              // final ParcelFileDescriptor parcelFd = ParcelFileDescriptor.fromSocket(proxy.getSocket());
		                //intent.putExtra(PicoServiceImpl.PROXY_SOCKET, parcelFd);
		                //add safeSession and proxy to global map to be picked up by PicoServiceImpl
		                PicoApplication.addProxy(safeSession, proxy);
		                //get sequence number from last non-continuous auth message
		                //and send it as a challenge in the first continuous auth message
		                intent.putExtra(PicoServiceImpl.SEQUENCE_NUMBER, prover.getReceivedExtraData()); 
		                intent.putExtra(
		                        PicoServiceImpl.StartCommandType.class.getCanonicalName(),
		                        PicoServiceImpl.StartCommandType.START.ordinal());
		                intent.putExtra(SafeSession.class.getCanonicalName(), safeSession);
		                startService(intent);
		            }
		            
		            localIntent.putExtra(SESSION, safeSession);
	            } else if (address.getScheme().equals("http")) {	            	
			        // Create a rendezvous channel			        
					final RendezvousChannel channel =
							new RendezvousChannel(SafeService.UriToURI(service.getAddress()).toURL());
					
					final RendezvousSigmaProxy proxy = new RendezvousSigmaProxy(
							channel, new JsonMessageSerializer());		
					
					//set the extra data to be null unless some was received from the coffee chooser
					byte[] extraData = null;
					if(receivedIntent.hasExtra("myExtraData")){
						extraData = receivedIntent.getByteArrayExtra("myExtraData");
						LOGGER.debug("Extra data received was {}",receivedIntent.getByteArrayExtra("myExtraData"));
					}
					
					
					final NewSigmaProver prover = new NewSigmaProver(
							NewSigmaProver.VERSION_1_1,
							new KeyPair(keyPairing.getPublicKey(), keyPairing.getPrivateKey()),
							extraData, //extra data here
							proxy,
							service.getCommitment());
		
					// Authenticate to the Terminal
					LOGGER.debug("Authenticating to {} over RendezvousChannel {}",
							service, channel.getUrl());	

					final Session session;
					if (prover.prove()) {						
						session = Session.newInstanceActive(
								dbDataFactory,
                                Integer.toString(prover.getVerifierSessionId()),
                                prover.getSharedKey(),
                                keyPairing,
                                null);
						
					} else {						
						session = Session.newInstanceClosed(
								dbDataFactory,
                                Integer.toString(prover.getVerifierSessionId()),
                                keyPairing,
                                null);
					}
					
		            // Carry out the authentication and get the Session instance result:
		            final SafeSession safeSession = new SafeSession(session);	    
		            if (session.getStatus() == Session.Status.ACTIVE) {
		                // Start continuous authentication
		                final Intent intent = new Intent(this, PicoServiceImpl.class);
		                intent.putExtra(PicoServiceImpl.PROXY_CHANNEL, channel.getUrl());

		                intent.putExtra(
		                        PicoServiceImpl.StartCommandType.class.getCanonicalName(),
		                        PicoServiceImpl.StartCommandType.START.ordinal());
		                intent.putExtra(SafeSession.class.getCanonicalName(), safeSession);
		                startService(intent);
		            }
		            
		            localIntent.putExtra(SESSION, safeSession);
		            
		            
	            }else if(address.getScheme().equals("btspp")){
	            	String serviceAddr = service.getAddress().toString();
	            	LOGGER.info(serviceAddr.substring(address.toString().length()-12));
	            	String hwAddressStr = serviceAddr.substring(address.toString().length()-12).toUpperCase();
	            	//insert colon between each pair of characters
	            	String hwAddress = hwAddressStr.replaceAll("..(?!$)", "$0:");
	            	
	            	//create bluetooth socket connecting to specific BT hardware address
	            	try {
	        			BluetoothManager bMgr = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
	        			BluetoothAdapter bAdapter = bMgr.getAdapter();
	        			BluetoothDevice bDevice = bAdapter.getRemoteDevice(hwAddress);
	        			UUID uuid = new UUID(0l,Long.valueOf(hwAddressStr, 16));

	        			BluetoothSocket bSocket = bDevice.createRfcommSocketToServiceRecord(uuid);
	        			bSocket.connect();    		
	        			LOGGER.info("Bluetooth socket is connected");
	            				
					final SigmaProxy proxy = new SigmaProxy(
							bSocket, new JsonMessageSerializer());		
					
					//set the extra data to be null unless some was received
					byte[] extraData = null;
					if(receivedIntent.hasExtra(AuthenticateIntentService.EXTRA_DATA)){
						extraData = receivedIntent.getByteArrayExtra(AuthenticateIntentService.EXTRA_DATA);
					}
					
					
					final NewSigmaProver prover = new NewSigmaProver(
							NewSigmaProver.VERSION_1_1,
							new KeyPair(keyPairing.getPublicKey(), keyPairing.getPrivateKey()),
							extraData, //extra data here
							proxy,
							service.getCommitment());
		
					// Authenticate to the Terminal
					LOGGER.debug("Authenticating to {} over Bluetooth Channel",
							service);	

					final Session session;
					if (prover.prove()) {						
						session = Session.newInstanceActive(
								dbDataFactory,
                                Integer.toString(prover.getVerifierSessionId()),
                                prover.getSharedKey(),
                                keyPairing,
                                null);
						
					} else {						
						session = Session.newInstanceClosed(
								dbDataFactory,
                                Integer.toString(prover.getVerifierSessionId()),
                                keyPairing,
                                null);
					}
					
		            // Carry out the authentication and get the Session instance result:
		            final SafeSession safeSession = new SafeSession(session);	    
		            if (session.getStatus() == Session.Status.ACTIVE) {
		              /*  // Start continuous authentication
		                final Intent intent = new Intent(this, PicoServiceImpl.class);
		                intent.putExtra(PicoServiceImpl.PROXY_CHANNEL, channel.getUrl());

		                intent.putExtra(
		                        PicoServiceImpl.StartCommandType.class.getCanonicalName(),
		                        PicoServiceImpl.StartCommandType.START.ordinal());
		                intent.putExtra(SafeSession.class.getCanonicalName(), safeSession);
		                startService(intent);*/
		            }
		            
		            localIntent.putExtra(SESSION, safeSession);
		            LOGGER.info("Closing bluetooth socket");
		            bSocket.close();
	            	} catch (IOException e) {
	        			LOGGER.error(e.getMessage());
	        		}
	        			
	            }
	            
	            else {
	                throw new IllegalArgumentException(
	                		"unsupported service protocol: " + address.getScheme());
	            }
	        } else {
	            LOGGER.error("KeyPairing is invalid");
	            throw new PairingNotFoundException("KeyPairing is invalid");
	        }
		} catch (Exception e) {
			final Bundle extras = new Bundle();
			extras.putSerializable(EXCEPTION, (Serializable) e);
			localIntent.putExtras(extras);			       
		} finally {
		    LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);			
		}
	}

	private void authenticatePairing(final SafeLensPairing pairing, final SafeService service,
			final String loginForm, final String cookieString, final Uri terminalAddress, final byte[] terminalSharedKey) {
		// Return the result as a broadcast
		final Intent localIntent = new Intent(AUTHENTICATE_PAIRING_ACTION);
		try {
			try {
		        // Promote to a full LensPairing
		        final LensPairing credentialPairing = pairing.getLensPairing(dbDataAccessor);
		        if (credentialPairing != null) {
		            // Get the address to authenticate to
		            final URI serviceAddress = new URI(service.getAddress().toString());
	
		            LOGGER.debug("Authenticating credential pairing: {}, {}", pairing, serviceAddress);
	
		            // Construct the prover
		            final Prover prover = new LensProver(credentialPairing, serviceAddress, loginForm, cookieString, dbDataFactory);
	
		            // Carry out the authentication and get the Session instance result:
		            final Session session = prover.startSession();
		            localIntent.putExtra(SESSION, new SafeSession(session));	        
		            if (session.getStatus() != Session.Status.ERROR) {
		                // If the session is ok, then save it...
		                session.save();
		                
		                // and save the new service address
		                credentialPairing.getService().setAddress(serviceAddress);
		                credentialPairing.save();
		            }			           
		            
		            // Delegate the AuthToken to the Terminal 
					final PicoReauthMessage message = new PicoReauthMessage(
							session.getId(), ReauthState.CONTINUE, SequenceNumber.getRandomInstance(),
							session.getAuthToken().toByteArray());			
					
			        // Create a rendezvous channel
					LOGGER.debug("Creating RendezvousChannel at {}", terminalAddress);
			        final URL url = SafeService.UriToURI(terminalAddress).toURL();		        
					final RendezvousChannel channel = new RendezvousChannel(url);
					
					// Make a proxy for the terminal verifier
					final RendezvousSigmaProxy proxy = new RendezvousSigmaProxy(
							channel, new JsonMessageSerializer());
						
					proxy.reauth(message.encrypt(new SecretKeySpec(terminalSharedKey, "AES/GCM/NoPadding")));	    
		        } else {
		        	LOGGER.error("Pairing not found");
		        }
			} catch (IOException e) {
				LOGGER.error("IOException...");
				throw e;
			} catch (URISyntaxException e) {
				LOGGER.error("URISyntax...");
				throw e;
			} catch (InvalidKeyException e) {
				LOGGER.error("InvalidKey...");
				throw e;
			}
		} catch (Exception e) {
			final Bundle extras = new Bundle();
			extras.putSerializable(EXCEPTION, (Serializable) e);
			localIntent.putExtras(extras);
		} finally {
		    LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
        }		
	}

	private void authenticateDelegated(final AuthToken token, final Uri terminalAddress, final byte[] terminalSharedKey, SafeService service) {
		// Return the result as a broadcast
		final Intent localIntent = new Intent(AUTHENTICATE_DELEGATED);
		
		try {
			try {
	        // Delegate the AuthToken to the Terminal
			// TODO: Figure out what the session.getId() value should be
			PicoReauthMessage message = new PicoReauthMessage(
						/*session.getId()*/ 0, ReauthState.CONTINUE, SequenceNumber.getRandomInstance(),
						token.toByteArray());
			
	        // Create a rendezvous channel
			LOGGER.debug("Creating RendezvousChannel at {}", terminalAddress);
	        final URL url = SafeService.UriToURI(terminalAddress).toURL();		        
			final RendezvousChannel channel = new RendezvousChannel(url);
			
			// Make a proxy for the terminal verifier
			final RendezvousSigmaProxy proxy = new RendezvousSigmaProxy(
					channel, new JsonMessageSerializer());
				
			proxy.reauth(message.encrypt(new SecretKeySpec(terminalSharedKey, "AES/GCM/NoPadding")));	    

            localIntent.putExtra(SERVICE, service);
			
			} catch (IOException e) {
				LOGGER.error("IOException...");
				throw e;
			} catch (InvalidKeyException e) {
				LOGGER.error("InvalidKey...");
				throw e;
			}
		} catch (Exception e) {
			final Bundle extras = new Bundle();
			extras.putSerializable(EXCEPTION, (Serializable) e);
			localIntent.putExtras(extras);
		} finally {
		    LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	    }				
	}

	private void authenticateTerminal(final Uri terminalAddress, final byte[] terminalCommitment) {
		// Return the result as a broadcast intent
		final Intent localIntent = new Intent();
		try {			
			// Lookup the terminal based on it's commitment
			final Terminal terminal = dbDataAccessor.getTerminalByCommitment(terminalCommitment);
			if (terminal != null) {
				LOGGER.debug("Terminal {} is trusted", terminal);
				localIntent.setAction(AUTHENTICATE_TERMINAL_ACTION);
				
		        // Create a rendezvous channel		
		        final URL url = SafeService.UriToURI(terminalAddress).toURL();		        
				final RendezvousChannel channel = new RendezvousChannel(url);
				
				// Make a proxy for the terminal verifier
				final RendezvousSigmaProxy proxy = new RendezvousSigmaProxy(
						channel, new JsonMessageSerializer());
				      
				final NewSigmaProver prover = new NewSigmaProver(
						NewSigmaProver.VERSION_1_1,
						new KeyPair(terminal.getPicoPublicKey(), terminal.getPicoPrivateKey()),
						null,
						proxy,
						terminal.getCommitment());
				
				// Authenticate to the Terminal
				LOGGER.debug("Authenticating to {} over RendezvousChannel {}",
						terminal, terminalAddress);					
				prover.prove();

				// Return the extra data and the terminal shared key
				localIntent.putExtra(EXTRA_DATA, prover.getReceivedExtraData());
				localIntent.putExtra(TERMINAL_SHARED_KEY, prover.getSharedKey().getEncoded());
			} else {
				LOGGER.warn("Terminal with commitment {} is not trusted", terminalCommitment);
				localIntent.setAction(AUTHENTICATE_TERMINAL_UNTRUSTED);
			}
		} catch (IOException e) {
			LOGGER.warn("unable to authenticate to terminal", e);
			final Bundle extras = new Bundle();
			extras.putSerializable(EXCEPTION, (Serializable) e);
			localIntent.putExtras(extras);
		} catch (ProverAuthRejectedException e) {
			LOGGER.warn("terminal rejected authentication", e);
			final Bundle extras = new Bundle();
			extras.putSerializable(EXCEPTION, (Serializable) e);
			localIntent.putExtras(extras);
		} catch (ProtocolViolationException e) {
			LOGGER.warn("terminal violated the authentication protocol", e);
			final Bundle extras = new Bundle();
			extras.putSerializable(EXCEPTION, (Serializable) e);
			localIntent.putExtras(extras);
		} catch (VerifierAuthFailedException e) {
			LOGGER.warn("terminal violated the authentication protocol", e);
			final Bundle extras = new Bundle();
			extras.putSerializable(EXCEPTION, (Serializable) e);
			localIntent.putExtras(extras);		
		} finally {
		    LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
		}
	}
}