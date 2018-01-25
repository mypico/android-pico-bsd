/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.data;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyPair;
import java.util.Date;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.crypto.AuthToken;
import uk.ac.cam.cl.pico.crypto.CryptoFactory;
import uk.ac.cam.cl.pico.data.DataAccessor;
import uk.ac.cam.cl.pico.data.DataFactory;
import uk.ac.cam.cl.pico.data.pairing.KeyPairing;
import uk.ac.cam.cl.pico.data.pairing.KeyPairingAccessor;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Parcel;
import android.os.Parcelable;

final public class SafeKeyPairing extends SafePairing {

    /*
     * Need to override CREATOR otherwise a ClassCastException is thrown when we try to get back a
     * parcelled SafeKeyPairing.
     */
    public static final Parcelable.Creator<SafeKeyPairing> CREATOR =
            new Parcelable.Creator<SafeKeyPairing>() {

                @Override
                public SafeKeyPairing createFromParcel(Parcel source) {
                    // Just reuse the superclass CREATOR:
                    SafePairing p =
                            SafePairing.CREATOR.createFromParcel(source);
                    return new SafeKeyPairing(
                            p.pairingId,
                            p.idIsKnown(),
                            p.getName(),
                            p.getSafeService(),
                            p.getDateCreated().orNull());
                }

                @Override
                public SafeKeyPairing[] newArray(int size) {
                    return new SafeKeyPairing[size];
                }
            };

    private SafeKeyPairing(
            final int id,
            final boolean idIsKnown,
            final String name,
            final SafeService service,
            final Date dateCreated) {
        super(id, idIsKnown, name, service, dateCreated);
    }

    public SafeKeyPairing(final String name, final SafeService service) {
        super(name, service);
    }

    public SafeKeyPairing(KeyPairing keyPairing) {
        super(keyPairing);
    }

    public KeyPairing createKeyPairing(DataFactory factory, DataAccessor accessor)
    		throws IOException {
    	// Generate a key pair for the new full key pairing
    	KeyPair keyPair = CryptoFactory.INSTANCE.ecKpg().generateKeyPair();
        return new KeyPairing(
                factory,
                getName(),
                getSafeService().getOrCreateService(factory, accessor),
                keyPair);
    }

    public KeyPairing getKeyPairing(KeyPairingAccessor accessor) throws IOException {
        if (idIsKnown()) {
            return accessor.getKeyPairingById(pairingId);
        } else {
            return null;
        }
    }

    public KeyPairing getOrCreateKeyPairing(DataFactory factory, DataAccessor accessor)
            throws IOException {
        KeyPairing existing = getKeyPairing(accessor);
        if (existing != null) {
            return existing;
        } else {
            return createKeyPairing(factory, accessor);
        }
    }

	@Override
	public AlertDialog getDelegationFailedDialog(Activity context, AuthToken token) {
		// TODO remove use of exceptions for control flow
		String message;
		try {
			URL fallbackUrl = new URL(token.getFallback());
			
			// if MalformedURLException not thrown then fallback is a valid URL
			// Prepare a message which includes the fallback of the auth token
			message = context.getString(R.string.delegation_failed_transcribe) + ": " + fallbackUrl.toString();
		} catch (MalformedURLException e) {
			message = context.getString(R.string.delegation_failed);
		}
		
		// Build dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(message);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Do nothing
			}
		});
		return builder.create();
	}
}
