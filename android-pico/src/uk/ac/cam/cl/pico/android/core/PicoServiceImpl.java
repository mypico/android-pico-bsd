/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;
import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.data.SafeKeyPairing;
import uk.ac.cam.cl.pico.android.data.SafeLensPairing;
import uk.ac.cam.cl.pico.android.data.SafePairing;
import uk.ac.cam.cl.pico.android.data.SafeService;
import uk.ac.cam.cl.pico.android.data.SafeSession;
import uk.ac.cam.cl.pico.android.db.DbHelper;
import uk.ac.cam.cl.pico.comms.CombinedVerifierProxy;
import uk.ac.cam.cl.pico.comms.JsonMessageSerializer;
import uk.ac.cam.cl.pico.comms.RendezvousSigmaProxy;
import uk.ac.cam.cl.pico.comms.SocketCombinedProxy;
import uk.ac.cam.cl.pico.crypto.ContinuousProver;
import uk.ac.cam.cl.pico.crypto.LensProver;
import uk.ac.cam.cl.pico.crypto.Prover;
import uk.ac.cam.cl.pico.crypto.ServiceSigmaProver;
import uk.ac.cam.cl.pico.crypto.messages.SequenceNumber;
import uk.ac.cam.cl.pico.data.pairing.KeyPairing;
import uk.ac.cam.cl.pico.data.pairing.LensPairing;
import uk.ac.cam.cl.pico.data.pairing.Pairing;
import uk.ac.cam.cl.pico.data.pairing.PairingNotFoundException;
import uk.ac.cam.cl.pico.data.session.Session;
import uk.ac.cam.cl.pico.data.terminal.Terminal;
import uk.ac.cam.cl.pico.db.DbDataAccessor;
import uk.ac.cam.cl.pico.db.DbDataFactory;
import uk.ac.cam.cl.rendezvous.RendezvousChannel;

/**
 * TODO
 * <p>
 * TODO
 * 
 * @author Max Spencer <ms955@cl.cam.ac.uk>
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 * 
 */
final public class PicoServiceImpl extends Service implements PicoService {

	public static final String SESSION_INFO_UPDATE = "uk.ac.cam.cl.pico.android.service.SESSION_INFO_UPDATE";
	public static final int NOTIFICATION_ID = 5001;
	public static final String PROXY_SOCKET = "PROXY_SOCKET";
	public static final String PROXY_CHANNEL = "PROXY_CHANNEL";
	public static final String SEQUENCE_NUMBER = "SEQUENCE_NUMBER";
	public static final String GET_SINGLE_SESSION = "GET_SINGLE_SESSION";

	public static enum StartCommandType {
		START, PAUSE, RESUME, STOP
	}

	static final Logger LOGGER = LoggerFactory.getLogger(PicoServiceImpl.class.getSimpleName());

	private final IBinder binder = new PicoServiceBinder();
	private LocalBroadcastManager broadcastManager;
	private BroadcastReceiver broadcastReceiver;
	private SessionUpdateBroadcaster sessionUpdateBroadcaster;
	private Map<SafeSession, ContinuousProver> provers;

	// TODO make these into one thing
	private DbDataFactory dbDataFactory;
	private DbDataAccessor dbDataAccessor;

	public class PicoServiceBinder extends Binder {
		public PicoService getService() {
			return PicoServiceImpl.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onCreate() {
		LOGGER.debug("Starting PicoServiceImpl");

		// Ormlite helper thin
		OrmLiteSqliteOpenHelper helper = OpenHelperManager.getHelper(this, DbHelper.class);

		try {
			dbDataFactory = new DbDataFactory(helper.getConnectionSource());
			dbDataAccessor = new DbDataAccessor(helper.getConnectionSource());
		} catch (SQLException e) {
			LOGGER.warn("Failed to connect to database");
		}

		broadcastManager = LocalBroadcastManager.getInstance(this);

		sessionUpdateBroadcaster = new SessionUpdateBroadcaster(broadcastManager);

		provers = new HashMap<SafeSession, ContinuousProver>();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Start in the foreground
		LOGGER.debug(StartCommandType.class.getCanonicalName());
		Notification notification = new Notification.Builder(this)
				.setContentTitle("Pico is continuously authenticating").setSmallIcon(R.drawable.ic_launcher_pico)
				.build();
		startForeground(NOTIFICATION_ID, notification);

		if (intent != null) {
			final SafeSession sessionInfo;
			if (intent.hasExtra(GET_SINGLE_SESSION)) {
				LOGGER.debug("Need to get single safe session instance from list");
				// get single safesession instance in provers map
				Iterator<SafeSession> it = provers.keySet().iterator();
				if (provers.size() == 1) { //expecting only one continuous auth session for the saw demo
					if (it.hasNext()) {
						sessionInfo = it.next();
						LOGGER.debug("Got single safe session instance");
					} else { // no active provers, uh oh
						sessionInfo = null;
						LOGGER.error("No provers found");
					}
				} else {
					sessionInfo = null;
					LOGGER.error("provers map was the wrong size. Expected 1 got {}", provers.size());
				}
			} else {
				// Unpack SafeSession instance from intent
				LOGGER.debug("Unpacking safe session from intent");
				sessionInfo = (SafeSession) intent.getParcelableExtra(SafeSession.class.getCanonicalName());
			}
			if (sessionInfo == null) {
				LOGGER.error("Can't start ContinuousProver SafeSession null");
			} else {
				if (sessionInfo.getStatus() == Session.Status.CLOSED
						|| sessionInfo.getStatus() == Session.Status.ERROR) {
					LOGGER.warn("Can't start ContinuousProver SafeSession " + "status CLOSED or ERROR");
				} else {
					// Here we do different things depending on the "type"
					// of start command
					// i.e. Sometimes the service is being asked to start a
					// new continuous
					// auth session, other times to pause/resume/stop an
					// existing one.
					final int ord = intent.getIntExtra(StartCommandType.class.getCanonicalName(),
							StartCommandType.START.ordinal());
					final StartCommandType type = StartCommandType.values()[ord];
					// Retrieve the provers from the map

					if (type == StartCommandType.START) {
						final CombinedVerifierProxy proxy;
						if (intent.hasExtra(SEQUENCE_NUMBER)) { // Socket
																// connection

							// Create a proxy from the parcelled socket
							// final Socket socket =
							// intent.getParcelableExtra(PROXY_SOCKET);
							// proxy = new SocketCombinedProxy(socket, new
							// JsonMessageSerializer());

							// get proxy from global map
							proxy = PicoApplication.getProxy(sessionInfo);

							final SequenceNumber sequenceNumber = SequenceNumber
									.fromByteArray(intent.getByteArrayExtra(SEQUENCE_NUMBER));
							try {
								// Create the ContinousProver
								final ContinuousProver contProver = new ContinuousProver(
										sessionInfo.getSession(dbDataAccessor), proxy, sessionUpdateBroadcaster,
										HandlerScheduler.getInstance(), sequenceNumber);

								// Add to provers map for further
								// actions (pause etc) if not already there
								if (provers.get(sessionInfo) == null) {
									provers.put(sessionInfo, contProver);
								} else {
									LOGGER.warn(
											"provers Map already contained this session. This should have been a new session");
								}

								LOGGER.debug("Start continuous authentication");
								// Start the continuous auth cycle
								new Thread(new Runnable() {
									public void run() {
										contProver.updateVerifier();
									}
								}).start();
							} catch (IOException e) {
								LOGGER.error("Failed creating continuous prover");
							}
						} else if (intent.hasExtra(PROXY_CHANNEL)) {
							// Create a rendezvous channel
							final URL url = intent.getParcelableExtra(PROXY_CHANNEL);
							final RendezvousChannel channel = new RendezvousChannel(url);

							proxy = new RendezvousSigmaProxy(channel, new JsonMessageSerializer());

							try {
								// Create the ContinousProver
								final ContinuousProver contProver = new ContinuousProver(
										sessionInfo.getSession(dbDataAccessor), proxy, sessionUpdateBroadcaster,
										HandlerScheduler.getInstance(), SequenceNumber.getRandomInstance());

								// Add to provers map for further
								// actions (pause etc)
								provers.put(sessionInfo, contProver);

								LOGGER.debug("Start continuous authentication");

								// Start the continuous auth cycle
								new Thread(new Runnable() {
									public void run() {
										contProver.updateVerifier();
									}
								}).start();
							} catch (IOException e) {
								LOGGER.error("Failed creating continuous prover");
							}
						} else {
							LOGGER.error("Failed creating continuous prover");
						}
					} else {
						final ContinuousProver prover = provers.get(sessionInfo);
						if (prover != null) {
							if (type == StartCommandType.PAUSE) {
								LOGGER.debug("Pause continuous authentication");
								new Thread(new Runnable() {
									public void run() {
										prover.pause();
									}
								}).start();
							} else if (type == StartCommandType.RESUME) {
								LOGGER.debug("Resume continuous authentication");
								new Thread(new Runnable() {
									public void run() {
										prover.resume();
									}
								}).start();
							} else if (type == StartCommandType.STOP) {
								LOGGER.debug("Stop continuous authentication");
								provers.remove(sessionInfo);
								new Thread(new Runnable() {
									public void run() {
										prover.stop();
									}
								}).start();
								// Stop the service (if this was the only
								// continuous auth
								// session)
								if (stopSelfResult(startId)) {
									// Unregister broadcast receiver if the
									// service is going to
									// stop
									broadcastManager.unregisterReceiver(broadcastReceiver);
									LOGGER.info("Unregistered lock broadcast receiver");
								}
							} else {
								LOGGER.error("Invalid StatTypeCommand");
							}
						} else {
							LOGGER.warn("No prover found for session");
							Toast toast = Toast.makeText(this, "No Prover found for Session", Toast.LENGTH_SHORT);
							toast.show();
						}
					}

				}
			}
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		LOGGER.debug("PicoServiceImpl is being destroyed...");
		super.onDestroy();
		// TODO see:
		// http://ormlite.com/javadoc/ormlite-android/com/j256/ormlite/android/apptools/OpenHelperManager.html#releaseHelper%28%29
		// OpenHelperManager.releaseHelper();
	}

	@Override
	public List<SafeKeyPairing> getKeyPairings(final SafeService service) throws IOException {

		final byte[] c = service.getCommitment();
		LOGGER.debug("Getting KeyPairings for service commitment {}", c);
		final List<KeyPairing> pairings = dbDataAccessor.getKeyPairingsByServiceCommitment(c);

		// Convert the Pairing instances to PairingInfo before returning the
		// result to the UI
		final List<SafeKeyPairing> pairingInfos = new ArrayList<SafeKeyPairing>();
		for (KeyPairing p : pairings) {
			pairingInfos.add(new SafeKeyPairing(p));
		}
		return pairingInfos;
	}

	@Override
	@Deprecated
	public List<SafeLensPairing> getLensPairings(final SafeService service) throws IOException {

		final byte[] c = service.getCommitment();
		LOGGER.debug("Getting LensPairings for service commitment {}", c);

		final List<LensPairing> pairings = dbDataAccessor.getLensPairingsByServiceCommitment(c);

		// Convert the Pairing instances to PairingInfo before returning the
		// result to the UI
		final List<SafeLensPairing> pairingInfos = new ArrayList<SafeLensPairing>();
		for (final LensPairing p : pairings) {
			pairingInfos.add(new SafeLensPairing(p));
		}
		return pairingInfos;
	}

	/*
	 * Return a ProxyService for a service identified by a ServiceInfo instance.
	 */

	private CombinedVerifierProxy getServiceProxy(SafeService serviceInfo) {
		// Get whole address
		final Uri address = serviceInfo.getAddress();

		// Instantiate and return a concrete proxy subclass. Depends
		if (address.getScheme().equals("tcp")) {
			return new SocketCombinedProxy(address.getHost(), address.getPort(), new JsonMessageSerializer());
		} else {
			// TODO change to log this and return null or something..
			throw new IllegalArgumentException("unsupported service protocol: " + address.getScheme());
		}
	}

	@Deprecated
	@Override
	public SafeSession keyAuthenticate(final SafeKeyPairing pairing) throws IOException, PairingNotFoundException {

		// Verify the method's preconditions
		checkNotNull(pairing);

		LOGGER.debug("Authenticating key pairing {}", pairing);

		// Promote to a full KeyPairing
		final KeyPairing keyPairing = pairing.getKeyPairing(dbDataAccessor);
		if (keyPairing != null) {
			// Get a verifier proxy using the service info (of the pairing
			// info):
			CombinedVerifierProxy proxy = getServiceProxy(pairing.getSafeService());

			// Construct the prover:
			ServiceSigmaProver prover = new ServiceSigmaProver(keyPairing, proxy, dbDataFactory);

			// Carry out the authentication and get the Session instance result:
			final Session session = prover.startSession();

			// TO be removed - no need to persist sessions
			if (session.getStatus() != Session.Status.ERROR) {
				// If the session is ok, then save it.
				LOGGER.debug("Persisting session");
				session.save();
			}

			final SafeSession safeSession = new SafeSession(session);
			if (session.getStatus() == Session.Status.ACTIVE) {
				// Create the ContinousProver
				ContinuousProver contProver = prover.getContinuousProver(proxy, session, sessionUpdateBroadcaster,
						HandlerScheduler.getInstance());

				// Add to provers map for further actions (pause etc)
				provers.put(safeSession, contProver);

				// Start continuous authentication
				final Intent intent = new Intent(getApplicationContext(), PicoServiceImpl.class);
				intent.putExtra(StartCommandType.class.getCanonicalName(), StartCommandType.START.ordinal());
				intent.putExtra(SafeSession.class.getCanonicalName(), safeSession);
				startService(intent);
			}

			return safeSession;
		} else {
			LOGGER.error("KeyPairing is invalid");
			throw new PairingNotFoundException("KeyPairing is invalid");
		}
	}

	@Override
	@Deprecated
	public SafeSession lensAuthenticate(final SafeLensPairing pairing, final Uri newServiceAddress,
			final String loginForm, final String cookieString) throws IOException, PairingNotFoundException {

		// Verify the method's preconditions
		checkNotNull(pairing);

		// Promote to a full LensPairing
		final LensPairing credentialPairing = pairing.getLensPairing(dbDataAccessor);
		if (credentialPairing != null) {

			// Get the address to authenticate to
			final URI serviceAddress;
			if (newServiceAddress != null) {
				serviceAddress = SafeService.UriToURI(newServiceAddress);
			} else {
				serviceAddress = credentialPairing.getService().getAddress();
			}

			LOGGER.debug("Authenticating credential pairing: {}, {}", pairing, serviceAddress);

			// Construct the prover TODO fix this
			final Prover prover = new LensProver(credentialPairing, serviceAddress, loginForm, cookieString,
					dbDataFactory);

			// Carry out the authentication and get the Session instance result:
			final Session session = prover.startSession();
			if (session.getStatus() != Session.Status.ERROR) {
				// If the session is ok, then save it...
				session.save();

				// and save the new service address
				credentialPairing.getService().setAddress(serviceAddress);
				credentialPairing.save();
			}

			return new SafeSession(session);
		} else {
			LOGGER.error("LensPairing is invalid");
			throw new PairingNotFoundException("CredentialPairing is invalid");
		}
	}

	/**
	 * TODO
	 * 
	 * @param safePairing
	 * @param newName
	 * @return TODO
	 */
	@Override
	public SafePairing renamePairing(final SafePairing safePairing, final String newName)
			throws IOException, PairingNotFoundException {
		// Verify the method's preconditions
		checkNotNull(safePairing, "Cannot rename pairing with a null safe pairing");
		checkNotNull(newName, "Cannot rename pairing to Null");
		if (!safePairing.idIsKnown()) {
			throw new IllegalArgumentException("Cannot rename pairing with safe pairing with unknown id");
		}

		final Pairing pairing = safePairing.getPairing(dbDataAccessor);
		if (pairing != null) {
			pairing.setName(newName);

			// Persist update to underlying storage.
			pairing.save();

			// Return the updated PairingInfo
			return new SafePairing(pairing);
		} else {
			throw new PairingNotFoundException("Pairing is invalid");
		}
	}

	@Override
	public void pauseSession(final SafeSession sessionInfo) {
		// Verify the method's preconditions
		checkNotNull(sessionInfo);

		final Intent intent = new Intent(getApplicationContext(), PicoServiceImpl.class);
		intent.putExtra(StartCommandType.class.getCanonicalName(), StartCommandType.PAUSE.ordinal());
		intent.putExtra(SafeSession.class.getCanonicalName(), sessionInfo);
		startService(intent);
	}

	@Override
	public void resumeSession(final SafeSession sessionInfo) {
		// Verify the method's preconditions
		checkNotNull(sessionInfo);

		final Intent intent = new Intent(getApplicationContext(), PicoServiceImpl.class);
		intent.putExtra(StartCommandType.class.getCanonicalName(), StartCommandType.RESUME.ordinal());
		intent.putExtra(SafeSession.class.getCanonicalName(), sessionInfo);
		startService(intent);
	}

	@Override
	public void closeSession(final SafeSession sessionInfo) {
		// Verify the method's preconditions
		checkNotNull(sessionInfo);

		final Intent intent = new Intent(getApplicationContext(), PicoServiceImpl.class);
		intent.putExtra(StartCommandType.class.getCanonicalName(), StartCommandType.STOP.ordinal());
		intent.putExtra(SafeSession.class.getCanonicalName(), sessionInfo);
		startService(intent);
	}

	@Override
	@Deprecated
	public void getTerminal(final byte[] terminalCommitment, final GetTerminalCallback callback) {
		new AsyncTask<byte[], Void, Optional<Terminal>>() {

			@Override
			protected Optional<Terminal> doInBackground(byte[]... params) {
				try {
					final Terminal t = dbDataAccessor.getTerminalByCommitment(terminalCommitment);
					return Optional.fromNullable(t);
				} catch (IOException e) {
					return Optional.absent();
				}
			}

			@Override
			public void onPostExecute(Optional<Terminal> result) {
				callback.onGetTerminalResult(result);
			}
		}.execute(terminalCommitment);
	}

	@Override
	@Deprecated
	public List<Terminal> getTerminals() throws IOException {
		return dbDataAccessor.getAllTerminals();
	}

	@Override
	@Deprecated
	public void getTerminals(final GetTerminalsCallback callback) {
		new AsyncTask<Void, Void, Optional<List<Terminal>>>() {

			private IOException e;

			@Override
			protected Optional<List<Terminal>> doInBackground(Void... arg0) {
				try {
					return Optional.of(getTerminals());
				} catch (IOException e) {
					this.e = e;
					return Optional.absent();
				}
			}

			@Override
			protected void onPostExecute(Optional<List<Terminal>> result) {
				if (result.isPresent()) {
					callback.onGetTerminalsResult(result.get());
				} else {
					callback.onGetTerminalsError(e);
				}
			}
		}.execute();
	}
}