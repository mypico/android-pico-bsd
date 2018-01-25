/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.EnumSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Base64;

import com.google.gson.JsonParseException;
import com.google.zxing.client.android.Intents;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.Display;
import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.core.InvalidCodeDialog.InvalidCodeCallbacks;
import uk.ac.cam.cl.pico.android.data.NonceParcel;
import uk.ac.cam.cl.pico.android.data.ParcelableCredentials;
import uk.ac.cam.cl.pico.android.data.SafeService;
import uk.ac.cam.cl.pico.android.delegate.DelegateActivity;
import uk.ac.cam.cl.pico.android.delegate.NewDelegatePairingActivity;
import uk.ac.cam.cl.pico.android.pairing.AuthenticateActivity;
import uk.ac.cam.cl.pico.android.pairing.ChooseKeyPairingActivity;
import uk.ac.cam.cl.pico.android.pairing.NewKeyPairingActivity;
import uk.ac.cam.cl.pico.android.pairing.NewLensPairingActivity;
import uk.ac.cam.cl.pico.comms.JsonMessageSerializer;
import uk.ac.cam.cl.pico.comms.RendezvousSigmaProxy;
import uk.ac.cam.cl.pico.crypto.CryptoFactory;
import uk.ac.cam.cl.pico.crypto.NewSigmaProver;
import uk.ac.cam.cl.pico.crypto.Nonce;
import uk.ac.cam.cl.pico.crypto.ProtocolViolationException;
import uk.ac.cam.cl.pico.crypto.NewSigmaProver.ProverAuthRejectedException;
import uk.ac.cam.cl.pico.crypto.NewSigmaProver.VerifierAuthFailedException;
import uk.ac.cam.cl.pico.data.pairing.LensPairing;
import uk.ac.cam.cl.pico.data.pairing.Pairing;
import uk.ac.cam.cl.pico.data.terminal.Terminal;
import uk.ac.cam.cl.pico.gson.VisualCodeGson;
import uk.ac.cam.cl.pico.visualcode.DelegatePairingVisualCode;
import uk.ac.cam.cl.pico.visualcode.InvalidVisualCodeException;
import uk.ac.cam.cl.pico.visualcode.KeyAuthenticationVisualCode;
import uk.ac.cam.cl.pico.visualcode.KeyPairingVisualCode;
import uk.ac.cam.cl.pico.visualcode.LensAuthenticationVisualCode;
import uk.ac.cam.cl.pico.visualcode.LensPairingVisualCode;
import uk.ac.cam.cl.pico.visualcode.SignedVisualCode;
import uk.ac.cam.cl.pico.visualcode.TerminalPairingVisualCode;
import uk.ac.cam.cl.pico.visualcode.VisualCode;
import uk.ac.cam.cl.pico.visualcode.WithTerminalDetails;
import uk.ac.cam.cl.rendezvous.RendezvousChannel;
import uk.ac.cam.cl.rendezvous.RendezvousClient;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Display;

import com.google.gson.JsonParseException;
import com.google.zxing.client.android.Intents;

/**
 * Activity for acquiring and parsing visual codes. This activity can be used in
 * two ways:
 * 
 * <p>
 * <ul>
 * <li>If started for a result (
 * {@link Activity#startActivityForResult(Intent, int)}) it will return the
 * parsed data contained in the visual code in the result intent.
 * <li>If a result is not requested ({@link Activity#startActivity(Intent)}) it
 * will start another activity appropriate for the type of visual code scanned.
 * In this case the parsed data from the visual code will be attached to the
 * intent which starts that next activity.
 * </ul>
 * 
 * <p>
 * These are the sets of extras and next activity (if not started for result)
 * for each visual code type:
 * 
 * <h3>{@link KeyAuthenticationVisualCode}</h3>
 * 
 * <p>
 * Next activity: {@link ChooseKeyPairingActivity}
 * 
 * <p>
 * Extras:
 * <ul>
 * <li>{@link AcquireCodeActivity#SERVICE SERVICE}
 * <li>{@link AcquireCodeActivity#TERMINAL_ADDRESS TERMINAL_ADDRESS} (optional)
 * <li>{@link AcquireCodeActivity#TERMINAL_COMMITMENT TERMINAL_COMMITMENT}
 * (optional)
 * </ul>
 * 
 * <h3>{@link KeyPairingVisualCode}</h3>
 * 
 * <p>
 * Next activity: {@link NewKeyPairingActivity}
 * 
 * <p>
 * Extras:
 * <ul>
 * <li>{@link AcquireCodeActivity#SERVICE SERVICE}
 * <li>{@link AcquireCodeActivity#TERMINAL_ADDRESS TERMINAL_ADDRESS} (optional)
 * <li>{@link AcquireCodeActivity#TERMINAL_COMMITMENT TERMINAL_COMMITMENT}
 * (optional)
 * </ul>
 * 
 * <h3>{@link LensAuthenticationVisualCode}</h3>
 * 
 * <p>
 * Next activity: {@link ChooseLensPairingActivity}
 * 
 * <p>
 * Extras:
 * <ul>
 * <li>{@link AcquireCodeActivity#SERVICE SERVICE}
 * <li>{@link AcquireCodeActivity#TERMINAL_ADDRESS TERMINAL_ADDRESS} (optional)
 * <li>{@link AcquireCodeActivity#TERMINAL_COMMITMENT TERMINAL_COMMITMENT}
 * (optional)
 * </ul>
 * 
 * <h3>{@link LensPairingVisualCode}</h3>
 * 
 * <p>
 * Next activity: {@link NewLensPairingActivity}
 * 
 * <p>
 * Extras:
 * <ul>
 * <li>{@link AcquireCodeActivity#SERVICE SERVICE}
 * <li>{@link AcquireCodeActivity#CREDENTIALS CREDENTIALS}
 * </ul>
 * 
 * <h3>{@link TerminalPairingVisualCode}</h3>
 * 
 * <p>
 * Next activity: None (this activity will just finish)
 * 
 * <p>
 * Extras:
 * <ul>
 * <li>{@link AcquireCodeActivity#TERMINAL_ADDRESS TERMINAL_ADDRESS}
 * <li>{@link AcquireCodeActivity#TERMINAL_COMMITMENT TERMINAL_COMMITMENT}
 * <li>{@link AcquireCodeActivity#TERMINAL_NAME TERMINAL_NAME}
 * <li>{@link AcquireCodeActivity#NONCE NONCE}
 * </ul>
 * 
 * <p>
 * The keys for the extras in the result/next intent, which contain the parsed
 * data of the QR code, are supplied as constant members of this class. The
 * types of the extras they correspond to are documented below.
 * 
 * <p>
 * The types of visual codes which will be accepted can be set using boolean
 * extras of the intent which starts this activity. The appropriate extra keys
 * are {@link AcquireCodeActivity#AUTH_ALLOWED},
 * {@link AcquireCodeActivity#PAIRING_ALLOWED} and
 * {@link AcquireCodeActivity#TERMINAL_PAIRING_ALLOWED}. If none of these extras
 * are set to <code>true</code>, the activity will default to allowing
 * authentication and pairing codes, but not terminal pairing codes.
 * 
 * @author Max Spencer <ms955@cl.cam.ac.uk>
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 * 
 */
final public class AcquireCodeActivity extends Activity implements InvalidCodeCallbacks {

	public final static String AUTH_ALLOWED = AcquireCodeActivity.class.getCanonicalName() + "authAllowed";
	public final static String PAIRING_ALLOWED = AcquireCodeActivity.class.getCanonicalName() + "pairingAllowed";
	public final static String TERMINAL_PAIRING_ALLOWED = AcquireCodeActivity.class.getCanonicalName()
			+ "terminalPairingAllowed";
	public final static String NO_MENU = "uk.ac.cam.cl.pico.android.core.NO_MENU";

	@Deprecated
	public final static String SERVICE_INFO_INTENT = SafeService.class.getCanonicalName();

	/**
	 * Key for the terminal name extra. This extra is a {@link String}.
	 */
	public static final String TERMINAL_NAME = AcquireCodeActivity.class.getCanonicalName() + "terminalName";

	/**
	 * Key for the nonce extra. This extra is a {@link NonceParcel}.
	 */
	public static final String NONCE = AcquireCodeActivity.class.getCanonicalName() + "nonce";

	/**
	 * Key for the terminal address extra. This extra is a {@link Uri}.
	 */
	public static final String TERMINAL_ADDRESS = AcquireCodeActivity.class.getCanonicalName() + "terminalAddress";
	/**
	 * String for getting boolean flag from another app specifying not to scan a
	 * QR code, but to get details from that app
	 */
	public static final String DATA_FROM_EXTERNAL_APP = "DATA_FROM_EXTERNAL_APP";

	/**
	 * Key for the terminal commitment extra. This extra is a byte array.
	 */
	public static final String TERMINAL_COMMITMENT = AcquireCodeActivity.class.getCanonicalName() + "terminalCommit";

	// TODO change these to have the above form. Requires checking that nowhere
	// else is using the
	// current key values.
	/**
	 * Key for the service extra. This extra is a {@link SafeService}.
	 */
	public final static String SERVICE = SafeService.class.getCanonicalName();

	/**
	 * Key for the credentials extra. This extra is a
	 * {@link ParcelableCredentials}.
	 */
	public final static String CREDENTIALS = ParcelableCredentials.class.getCanonicalName();

	private static final Logger LOGGER = LoggerFactory.getLogger(AcquireCodeActivity.class.getSimpleName());
	private static final int CAPTURE_CODE = 0;
	private static final String IS_SCANNING = "isScanning";
	private static final String INVALID_CODE_DIALOG = "invalidCodeDialog";

	private enum CodeType {
		AUTH(R.string.a_login_qr_code), PAIRING(R.string.a_pairing_qr_code), TERMINAL_PAIRING(
				R.string.a_terminal_pairing_qr_code);

		private final int withArticleResId;

		private CodeType(int withArticleResId) {
			this.withArticleResId = withArticleResId;
		}

		public int withArticleResId() {
			return withArticleResId;
		}
	}

	private static class WrongCodeTypeException extends Exception {

		private static final long serialVersionUID = 882410541131303071L;

		private CodeType wrongType;

		public WrongCodeTypeException(CodeType wrongType) {
			this.wrongType = wrongType;
		}

		public String wrongTypeWithArticle(Context context) {
			return context.getString(wrongType.withArticleResId());
		}
	}

	/**
	 * Verifies the signature of a {@link SignedVisualCode}.
	 * 
	 * @param visualCode
	 *            the visual code to verify
	 * @param signedBytes
	 *            the bytes the signature should be over
	 * @param publicKey
	 *            the public key to verify the signature with
	 * @return <code>true</code> if the signature of <code>visualCode</code>
	 *         verifies with the supplied bytes and public key, or
	 *         <code>false</code> otherwise.
	 * @throws InvalidKeyException
	 * @throws SignatureException
	 */
	private static boolean verifyVisualCode(final SignedVisualCode visualCode, final byte[] signedBytes,
			final PublicKey publicKey) throws InvalidKeyException, SignatureException {
		checkNotNull(visualCode, "visualCode cannot be null");
		checkNotNull(signedBytes, "signedBytes cannot be null");
		checkNotNull(publicKey, "publicKey cannot be null");

		final Signature verifier = CryptoFactory.INSTANCE.sha256Ecdsa();
		verifier.initVerify(publicKey);
		verifier.update(signedBytes);
		return verifier.verify(visualCode.getSignature());
	}

	/**
	 * Verify the signature of a {@link KeyPairingVisualCode} using the public
	 * key it contains.
	 * 
	 * @param code
	 *            the visual code
	 * @return <code>true</code> if the signature verifies or <code>false</code>
	 *         otherwise
	 * @throws InvalidVisualCodeException
	 *             if the signature cannot be verified
	 */
	private static boolean verifyKeyPairingVisualCode(final KeyPairingVisualCode code)
			throws InvalidVisualCodeException {
		checkNotNull(code, "code cannot be null");

		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		final byte[] signedBytes;
		try {
			os.write(code.getServiceName().getBytes("UTF-8"));
			os.write(code.getServiceAddress().toString().getBytes("UTF-8"));
			signedBytes = os.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("ByteArrayOutputStream should never throw an IOException", e);
		}

		try {
			return verifyVisualCode(code, signedBytes, code.getServicePublicKey());
		} catch (InvalidKeyException e) {
			throw new InvalidVisualCodeException("Signed visual code has an invalid key", e);
		} catch (SignatureException e) {
			throw new InvalidVisualCodeException("Unable to verify visual code signature", e);
		}
	}

	/**
	 * If present add the terminal details from a visual code to an intent.
	 * 
	 * @param intent
	 *            the intent
	 * @param code
	 *            the visual code
	 * @return <code>true</code> if the terminal details were present or
	 *         <code>false</code> otherwise
	 */
	private static boolean putTerminalDetailsIfPresent(Intent intent, WithTerminalDetails code) {
		if (code.hasTerminal()) {
			LOGGER.debug("Visual code contains terminal details");
			Uri terminalAddress = SafeService.URIToUri(code.getTerminalAddress());
			intent.putExtra(TERMINAL_ADDRESS, terminalAddress);
			intent.putExtra(TERMINAL_COMMITMENT, code.getTerminalCommitment());
			return true;
		} else {
			LOGGER.debug("Visual code does not contain terminal details");
			return false;
		}
	}

	/**
	 * Add the terminal details from a visual code to an intent.
	 * 
	 * @param intent
	 *            the intent
	 * @param code
	 *            the visual code
	 * @throws InvalidVisualCodeException
	 *             if the terminal details are not present
	 */
	private static void putTerminalDetails(Intent intent, WithTerminalDetails code) throws InvalidVisualCodeException {
		if (!putTerminalDetailsIfPresent(intent, code)) {
			throw new InvalidVisualCodeException("Visual code does not contain terminal details");
		}
	}

	private boolean isScanning;
	private boolean startedForResult;
	private EnumSet<CodeType> allowedTypes;

	/**
	 * AcquireCodeActivity lifecycle method. Restores the previously acquired
	 * visual code data (if stored) and acquires a visual code.
	 * 
	 * @param savedInstanceState
	 *            the Bundle to restore the visual code from.
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set a layout so that when this activity appears in the "recent apps"
		// menu it looks
		// reasonable
		setContentView(R.layout.activity_acquire_code);

		// Determine whether this activity has been started for a result
		startedForResult = (getCallingActivity() != null);

		// Determine the allowed types based on the extras in the received
		// intent
		allowedTypes = EnumSet.noneOf(CodeType.class);
		if (getIntent().getBooleanExtra(AUTH_ALLOWED, false)) {
			allowedTypes.add(CodeType.AUTH);
		}
		if (getIntent().getBooleanExtra(PAIRING_ALLOWED, false)) {
			allowedTypes.add(CodeType.PAIRING);
		}
		if (getIntent().getBooleanExtra(TERMINAL_PAIRING_ALLOWED, false)) {
			allowedTypes.add(CodeType.TERMINAL_PAIRING);
		}
		if (allowedTypes.isEmpty()) {
			// ...if none were set, default to allowing auth and pairing codes
			allowedTypes.add(CodeType.AUTH);
			allowedTypes.add(CodeType.PAIRING);
		}

		// Restore isScanning flag from saved state
		if (savedInstanceState != null) {
			isScanning = savedInstanceState.getBoolean(IS_SCANNING, false);
		} else {
			isScanning = false;
		}

		// get data straight from another app instead of QR code
		if (getIntent().getBooleanExtra(DATA_FROM_EXTERNAL_APP, false)) {
			LOGGER.debug("Getting data from external application");
			Intent intent = new Intent(getIntent());
			String serviceCommitment = getIntent().getStringExtra("EXTERNAL_SERVICE_COMMITMENT");
			byte[] decodedServiceCommitment = Base64.decode(serviceCommitment);
			Uri address = Uri.parse(getIntent().getStringExtra("EXTERNAL_SERVICE_ADDRESS"));
			SafeService service = new SafeService(null, decodedServiceCommitment, address, null);
			intent.putExtra(SERVICE, service);

			// This activity was just started and is not supposed to return
			// a result, in this case we just start the activity specified
			// in the intent (as set in intentFromCode)
			intent.setClass(this, ChooseKeyPairingActivity.class);
			startActivity(intent);
			LOGGER.debug("AcquireCodeActivity is finishing");
			finish();
		} else {
			// ...then start scanning if isScanning is not set
			if (!isScanning) {
				acquireCode();
			}
		}
	}

	@Override
	public void onSaveInstanceState(final Bundle savedInstanceState) {
		savedInstanceState.putBoolean(IS_SCANNING, isScanning);
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent resultIntent) {
		if (requestCode == CAPTURE_CODE) {
			if (resultCode == RESULT_OK) {
				LOGGER.debug("ZXing CaptureActivity returned a result");

				// Get results as a string (JSON serialisation)
				final String c = resultIntent.getStringExtra(Intents.Scan.RESULT);

				if (!isNullOrEmpty(c)) {
					LOGGER.trace("QR code contents: " + c);
					try {
						final VisualCode code = VisualCodeGson.gson.fromJson(c, VisualCode.class);

						try {
							Intent intent = intentFromCode(code);
							if (intent != null) {
								if (startedForResult) {
									// This activity was started for result, so
									// set the result data
									// (the intent) and finish()
									setResult(Activity.RESULT_OK, intent);
									finish();
								} else {
									// This activity was just started and is not
									// supposed to return
									// a result, in this case we just start the
									// activity specified
									// in the intent (as set in intentFromCode)
									startActivity(intent);
									finish();
								}
							}
						} catch (WrongCodeTypeException e) {
							// Visual code type allowed
							final String message = getString(R.string.wrong_qr_type, allowedTypesString(),
									e.wrongTypeWithArticle(this));
							InvalidCodeDialog.getInstance(message).show(getFragmentManager(), INVALID_CODE_DIALOG);
						}

					} catch (InvalidVisualCodeException e) {
						// In this case the QR code be parsed as a valid type of
						// Pico QR code, but
						// is still somehow invalid, e.g. bad signature, missing
						// fields
						LOGGER.warn("Invalid Pico visual code", e);
						InvalidCodeDialog.getInstance(R.string.invalid_pico_qr_code).show(getFragmentManager(),
								INVALID_CODE_DIALOG);
					} catch (JsonParseException e) {
						// In this case the QR code could not be parsed by the
						// VisualCodeGson
						// instance and is therefore not a valid Pico QR code.
						LOGGER.warn("Not a Pico visual code", e);
						InvalidCodeDialog.getInstance(R.string.not_a_pico_qr_code).show(getFragmentManager(),
								INVALID_CODE_DIALOG);
					}
				} else {
					LOGGER.warn("Result is null or empty!");
					// This is really an error condition, but as it originates
					// with the ZXing
					// activity (and hopefully isn't actually possible), we will
					// silently handle it
					// by just finishing this activity as if it were cancelled.
					setResult(Activity.RESULT_CANCELED);
					finish();
				}
			} else if (resultCode == RESULT_CANCELED) {
				LOGGER.debug("ZXing CaptureActivity was cancelled, finishing activity");
				if (startedForResult) {
					setResult(Activity.RESULT_CANCELED);
				}
				finish();
			}
		}
	}

	private String allowedTypesString() {
		final int n = allowedTypes.size();
		final CodeType[] types = new CodeType[n];
		allowedTypes.toArray(types);

		if (types.length == 1) {
			return getString(types[0].withArticleResId());
		} else if (types.length == 2) {
			return getString(R.string.or_2, getString(types[0].withArticleResId()),
					getString(types[1].withArticleResId()));
		} else if (types.length == 3) {
			return getString(R.string.or_3, getString(types[0].withArticleResId()),
					getString(types[1].withArticleResId()), getString(types[2].withArticleResId()));
		} else {
			return getString(R.string.nothing);
		}
	}

	/**
     * Return an intent appropriate for the supplied visual code. If this activity was started for 
     * a result, the returned intent will not specify a target.
     *  
     * @param code the visual code
     * @return appropriate intent
     * @throws InvalidVisualCodeException if the visual code is invalid
	 * @throws WrongCodeTypeException if the code is not of an allowed type
     */
    private Intent intentFromCode(VisualCode code)
    		throws InvalidVisualCodeException, WrongCodeTypeException {
    	if (code instanceof KeyAuthenticationVisualCode) { // KEY AUTH
    		LOGGER.debug("KeyAuthenticationVisualCode instance");
    		final KeyAuthenticationVisualCode kaCode = (KeyAuthenticationVisualCode) code;
    		
    		// Check if authentication codes are allowed
    		if (allowedTypes.contains(CodeType.AUTH)) {
    			final Intent intent = new Intent();
    			if (!startedForResult) {
    				// Set target activity
    				intent.setClass(this, ChooseKeyPairingActivity.class);
    			}
    			
    			// Add service from visual code to intent
    			SafeService service = SafeService.fromVisualCode(kaCode);
    			intent.putExtra(SERVICE, service);
            
    			// Add terminal details to intent
    			putTerminalDetailsIfPresent(intent, kaCode);
    			
				// Add extra data to intent
				intent.putExtra("myExtraData", kaCode.getExtraData());
				LOGGER.debug("Extra data straight from visual code was {}", kaCode.getExtraData());
    			// Return complete intent
    			return intent;
    		} else {
    			LOGGER.debug("Authentication codes not allowed");
    			throw new WrongCodeTypeException(CodeType.AUTH);
    		}
    		
    	} else if (code instanceof KeyPairingVisualCode) { // KEY PAIRING
    		LOGGER.debug("KeyPairingVisualCode instance");
    		final KeyPairingVisualCode kpCode = (KeyPairingVisualCode) code;
    		
    		// Check if authentication codes are allowed
    		if (allowedTypes.contains(CodeType.PAIRING)) {
    			if (verifyKeyPairingVisualCode(kpCode)) {
    				final Intent intent = new Intent();
    				if (!startedForResult) {
    					// Set target activity
    					intent.setClass(this, NewKeyPairingActivity.class);
    				}
    				
    				// Add service from visual code to intent
    				SafeService service = SafeService.fromVisualCode(kpCode);
        			intent.putExtra(SERVICE, service);
                    
					// Add extra data to intent
					intent.putExtra("myExtraData", kpCode.getExtraData());
					LOGGER.debug("Extra data straight from visual code was {}", kpCode.getExtraData());

        			// Add terminal details to intent
        			putTerminalDetailsIfPresent(intent, kpCode);
        			
        			return intent;
    			} else {
    				throw new InvalidVisualCodeException("Visual code has invalid signature");
    			}
    			
    		} else {
    			LOGGER.debug("Pairing codes not allowed");
    			throw new WrongCodeTypeException(CodeType.PAIRING);
    		}
    		
    	} else if (code instanceof LensAuthenticationVisualCode) { // LENS AUTH
    		LOGGER.debug("LensAuthenticationVisualCode instance");
    		final LensAuthenticationVisualCode laCode = (LensAuthenticationVisualCode) code;
    		
    		// Check if authentication codes are allowed
    		if (allowedTypes.contains(CodeType.AUTH)) {
    			final Intent intent = new Intent();
    			if (!startedForResult) {
    				intent.setClass(this, AuthenticateActivity.class);
    			}
                
                // Add the terminal details to the next intent (if present)
                putTerminalDetailsIfPresent(intent, laCode);
                
                return intent;
    		} else {
    			LOGGER.debug("Authentication codes not allowed");
    			throw new WrongCodeTypeException(CodeType.AUTH);
    		}
    		
    	} else if (code instanceof LensPairingVisualCode) { // LENS PAIRING
    		LOGGER.debug("LensPairingVisualCode instance");
    		final LensPairingVisualCode lpCode = (LensPairingVisualCode) code;
    		
    		// Check if pairing codes are allowed
    		if (allowedTypes.contains(CodeType.PAIRING)) {
    			final Intent intent = new Intent();
    			if (!startedForResult) {
    				intent.setClass(this, NewLensPairingActivity.class);
    			}
    			
    			// Add service from visual code to intent
    			SafeService service = SafeService.fromVisualCode(lpCode);
    			intent.putExtra(SERVICE, service);

    			// Add credentials from visual code to intent
    			final ParcelableCredentials cs = ParcelableCredentials.fromVisualCode(lpCode);
                intent.putExtra(CREDENTIALS, cs);
                
                return intent;
    		} else {
    			LOGGER.debug("Pairing codes not allowed");
    			throw new WrongCodeTypeException(CodeType.PAIRING);
    		}
    		
    	} else if (code instanceof TerminalPairingVisualCode) { // TERMINAL PAIRING
    		LOGGER.debug("TerminalPairingVisualCode instance");
    		final TerminalPairingVisualCode tpCode = (TerminalPairingVisualCode) code;
    		
    		// Check if terminal pairing codes are allowed
    		if (allowedTypes.contains(CodeType.TERMINAL_PAIRING)) {
    			final Intent intent = new Intent();
    			if (!startedForResult) {
    				// No sensible target activity for this type of code
    			}
    			
    			// Add terminal details
    			putTerminalDetails(intent, tpCode);
    			
    			// Add terminal name (not included above)
    			intent.putExtra(TERMINAL_NAME, tpCode.getTerminalName());
    			
    			// Add nonce
    			intent.putExtra(NONCE, new NonceParcel(tpCode.getNonce()));
    			
    			return intent;
    		} else {
    			LOGGER.debug("Terminal pairing codes not allowed");
    			throw new WrongCodeTypeException(CodeType.TERMINAL_PAIRING);
    		}
    		
    	} else if (code instanceof DelegatePairingVisualCode) { // LENS PAIRING
    		LOGGER.debug("DelegatePairingVisualCode instance");
    		final DelegatePairingVisualCode dpCode = (DelegatePairingVisualCode) code;
    		
    		// Check if pairing codes are allowed
    		if (allowedTypes.contains(CodeType.PAIRING)) {
    			final Intent intent = new Intent();
    			if (!startedForResult) {
    				intent.setClass(this, NewDelegatePairingActivity.class);
    			}
    			
    			// TERMINAL_ADDRESS
    			// TERMINAL_COMMITMENT
    			// TERMINAL_NAME
    			// NONCE
    			
    			// Add terminal details
    			putTerminalDetails(intent, dpCode);
    			
    			// Add terminal name (not included above)
    			intent.putExtra(TERMINAL_NAME, dpCode.getTerminalName());
    			
    			// Add nonce
    			intent.putExtra(NONCE, new NonceParcel(dpCode.getNonce()));
    			
    			
    			
                return intent;
    		} else {
    			LOGGER.debug("Pairing codes not allowed");
    			throw new WrongCodeTypeException(CodeType.PAIRING);
    		}
    		
    	} else {
    		LOGGER.warn("VisualCode type not properly handled!");
        	return null;    		
    	}
    	// Not returning null here by default on purpose so that there's a compilation error if any
    	// case isn't individually addressed;
    }
    
    /**
     * Launch the zxing CaptureActivity to acquired the visual code. Result is received via
     * onActivityResult().
     */
    private void acquireCode() {
        LOGGER.debug("Starting zxing CaptureActivity...");
        
        // Set isScanning flag
        isScanning = true;

        final Intent intent = new Intent(this, PicoCaptureActivity.class);
        intent.putExtra(NO_MENU, getIntent().getBooleanExtra(NO_MENU, false));
        intent.setAction("com.google.zxing.client.android.SCAN");
        
        // Set the capture mode for QR codes
        intent.putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE);
        
        // Don't save the result in the zxing Barcode Scanner app's history (if present)
        intent.putExtra(Intents.Scan.SAVE_HISTORY, false);
        
        // Make view finder a square with side length equal to the smaller of the two screen
        // dimensions
        final Display display = getWindowManager().getDefaultDisplay();
        final Point screenSize = new Point();
        display.getSize(screenSize);
        int size = (int) (0.9 * Math.min(screenSize.x, screenSize.y));
        intent.putExtra(Intents.Scan.WIDTH, size);
        intent.putExtra(Intents.Scan.HEIGHT, size);
        
        // Don't pause after successfully capturing the code
        intent.putExtra(Intents.Scan.RESULT_DISPLAY_DURATION_MS, 0L);
        
        // Not prompt message
        intent.putExtra(Intents.Scan.PROMPT_MESSAGE, "");
        
        // Start
        startActivityForResult(intent, CAPTURE_CODE);
    }

    @Override
	public void onScanAnother() {
		acquireCode();
	}

	@Override
	public void onCancel() {
		setResult(Activity.RESULT_CANCELED);
		finish();
	}
}