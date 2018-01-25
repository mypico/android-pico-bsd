/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.data;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.pairing.LensPairingDetailActivity;
import uk.ac.cam.cl.pico.crypto.AuthToken;
import uk.ac.cam.cl.pico.data.DataAccessor;
import uk.ac.cam.cl.pico.data.DataFactory;
import uk.ac.cam.cl.pico.data.pairing.LensPairing;
import uk.ac.cam.cl.pico.data.pairing.LensPairingAccessor;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.base.Optional;

final public class SafeLensPairing extends SafePairing {

    /*
     * Need to override CREATOR otherwise a ClassCastException is thrown when we try to get back a
     * parcelled SafeLensPairing.
     */
    public static final Parcelable.Creator<SafeLensPairing> CREATOR =
            new Parcelable.Creator<SafeLensPairing>() {

                @Override
                public SafeLensPairing createFromParcel(Parcel source) {
                    // Just reuse the superclass CREATOR:
                    SafePairing p =
                            SafePairing.CREATOR.createFromParcel(source);
                    return new SafeLensPairing(
                            p.pairingId,
                            p.idIsKnown(),
                            p.getName(),
                            p.getSafeService(),
                            p.getDateCreated().orNull());
                }

                @Override
                public SafeLensPairing[] newArray(int size) {
                    return new SafeLensPairing[size];
                }
            };

    private SafeLensPairing(
            final int id,
            final boolean idIsKnown,
            final String name,
            final SafeService service,
            final Date dateCreated) {
        super(id, idIsKnown, name, service, dateCreated);
    }

    public SafeLensPairing(final LensPairing credentialPairing) {
        super(credentialPairing);
    }

    public SafeLensPairing(
            final String name,
            final SafeService service) {
        super(name, service);
    }

    public LensPairing createLensPairing(
            final DataFactory factory,
            final DataAccessor accessor,
            final Map<String, String> credentials) throws IOException {

        return new LensPairing(
                factory,
                getName(),
                getSafeService().getOrCreateService(factory, accessor),
                credentials);
    }

    public LensPairing getLensPairing(
            final LensPairingAccessor accessor) throws IOException {
        if (idIsKnown()) {
            return accessor.getLensPairingById(pairingId);
        } else {
            return null;
        }
    }

    @Deprecated
    public LensPairing getOrCreateCredentialPairing(
            final DataFactory factory,
            final DataAccessor accessor,
            final Map<String, String> credentials) throws IOException {
        final LensPairing existing = getLensPairing(accessor);
        if (existing != null) {
            return existing;
        } else {
            return createLensPairing(factory, accessor, credentials);
        }
    }
    
    @Override
    public Optional<Intent> detailIntent(final Context context) {
    	final Intent intent = new Intent(context, LensPairingDetailActivity.class);
    	intent.putExtra(LensPairingDetailActivity.PAIRING, this);
    	return Optional.of(intent);
    }
    
    // Helper for the public getXFailedDialog methods
    private AlertDialog fallbackDialog(final Activity context, CharSequence message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(message);
		builder.setPositiveButton(
				R.string.show_saved_credentials, new DialogInterface.OnClickListener() {
					
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent i = detailIntent(context).get();
				i.putExtra(LensPairingDetailActivity.SHOW_ON_RESUME, true);
				context.startActivity(i);
			}
		});
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Do nothing
			}
		});
		return builder.create();
    }

	@Override
	public AlertDialog getAuthFailedDialog(final Activity context) {
		// Format message for dialog, then use helper method.
		final String message = String.format(
				context.getString(R.string.auth_failed_fmt), getSafeService().getName());
		return fallbackDialog(context, message);
	}

	@Override
	public AlertDialog getDelegationFailedDialog(Activity context, AuthToken token) {
		// Use helper method
		return fallbackDialog(context, context.getString(R.string.delegation_failed));
	}
}
