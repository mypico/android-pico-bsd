/**
 * Copyright Pico project, 2016
 */

// Copyright University of Cambridge, 2013

package uk.ac.cam.cl.pico.android.data;

import java.io.IOException;
import java.util.Date;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.pairing.AuthFailedDialog.AuthFailedSource;
import uk.ac.cam.cl.pico.android.pairing.DelegationFailedDialog.DelegationFailedSource;
import uk.ac.cam.cl.pico.crypto.AuthToken;
import uk.ac.cam.cl.pico.data.DataAccessor;
import uk.ac.cam.cl.pico.data.DataFactory;
import uk.ac.cam.cl.pico.data.pairing.Pairing;
import uk.ac.cam.cl.pico.data.pairing.PairingAccessor;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.base.Optional;

/**
 * Representation of a Pairing instance for display on the UI.
 * <p>
 * The SafePairing class is a representation of a Pairing, that contains only the information
 * required by the UI. Sensitive data such as the Pairing's private key is not passed to the UI
 * thread.
 * 
 * @see Pairing
 * 
 * @author Max Spencer <ms955@cl.cam.ac.uk>
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 * 
 */
public class SafePairing implements Parcelable, AuthFailedSource, DelegationFailedSource {

    private static final int UNKNOWN_ID = -1;
    private static final long INVALID_DATE = -1;
    
    public static enum PairingType {
        KEY,
        CREDENTIAL
    }

    /**
     * Generates a SafePairing instance from a Parcelable class, whose data had previously been
     * written by Parcelable.writeToParcel().
     * 
     * @see android.os.Parcelable.Creator
     */
    public static final Parcelable.Creator<SafePairing> CREATOR =
            new Parcelable.Creator<SafePairing>() {

                /**
                 * Create a new instance of the Parcelable class. A SafePairing is instantiated
                 * given a Parcel whose data had previously been written by
                 * Parcelable.writeToParcel().
                 * 
                 * @param Parcel a Parcel whose data had previously been written by
                 *        Parcelable.writeToParcel()
                 * @return a unmarshaled SafePairing instance
                 */
                @Override
                public SafePairing createFromParcel(Parcel source) {
                    // Read boolean array containing idIsKnown flag
                    boolean[] b = new boolean[1];
                    source.readBooleanArray(b);

                    // Check whether the Pairing dateCreated file is set
                    final long dateCreatedField = source.readLong();
                    final Date dateCreated;
                    if (dateCreatedField == INVALID_DATE) {
                    	dateCreated = null;
                    } else {
                    	dateCreated = new Date(dateCreatedField);
                    }
                    
                	return new SafePairing(
                        source.readInt(), // id
                        b[0], // idIsKnown
                        source.readString(), // name
                        (SafeService) source.readParcelable(
                                SafeService.class.getClassLoader()), // service
                        dateCreated);
                }

                /**
                 * Create a new array of the Parcelable class.
                 * 
                 * @param size the size of the array to create.
                 * @return the array
                 */
                @Override
                public SafePairing[] newArray(int size) {
                    return new SafePairing[size];
                }
            };

    protected final int pairingId;
    private final boolean idIsKnown;
    private final String name;
    private final SafeService serviceInfo;
    private final Date dateCreated;

    /**
     * Constructor This Constructor is used to marshal and unmarshal a SafePairing instance.
     * 
     * @param id the Pairing instance's id.
     * @param idIsKnown true if the Pairing is empty, false otherwise
     * @param name the Pairing instance's name.
     * @param service the Pairing instances service.
     */
    protected SafePairing(
            final int id,
            final boolean idIsKnown,
            final String name,
            final SafeService service,
            final Date dateCreated) {
        if (service == null)
            throw new NullPointerException();

        this.pairingId = id;
        this.name = name;
        this.serviceInfo = service;
        this.dateCreated = dateCreated;
        this.idIsKnown = idIsKnown;
    }


    protected SafePairing(final String name, final SafeService service) {
        this(UNKNOWN_ID, false, name, service, null);
    }

    /**
     * Construct a SafePairing instance from a full Pairing.
     * 
     * @param pairing a full Pairing instance.
     */
    public SafePairing(final Pairing pairing) {
        if (pairing == null) {
            throw new NullPointerException(
                    "SafePairing cannot be created from a null Pairing");
        } else {
            pairingId = pairing.getId();
            name = pairing.getName();
            serviceInfo = new SafeService(pairing.getService());
            dateCreated = pairing.getDateCreated();
            // TODO: Check this with Max
            idIsKnown = true;
        }
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * 
     * @return true if the SafePairing is empty, false otherwise
     */
    public boolean idIsKnown() {
        return idIsKnown;
    }

    /**
     * Get the SafePairing instance's name.
     * 
     * @return the SafePairing instance's name.
     */
    public String getName() {

        return name;
    }

    public String getDisplayName() {
        return new StringBuilder()
                .append(this.serviceInfo.getName())
                .append(": ")
                .append(name)
                .toString();
    }

    public SafeService getSafeService() {
        return serviceInfo;
    }
    
    public Optional<Date> getDateCreated() {
    	return Optional.fromNullable(dateCreated);
    }

    public Pairing createPairing(DataFactory factory, DataAccessor accessor)
            throws IOException {
        return new Pairing(
                factory,
                name,
                serviceInfo.getOrCreateService(factory, accessor));
    }

    public Pairing getPairing(PairingAccessor accessor)
            throws IOException {
        if (idIsKnown) {
            return accessor.getPairingById(pairingId);
        } else {
            return null;
        }
    }

    public Pairing getOrCreatePairing(
            DataFactory factory, DataAccessor accessor) throws IOException {
        Pairing existing = getPairing(accessor);
        if (existing != null) {
            return existing;
        } else {
            return createPairing(factory, accessor);
        }
    }

    /*
     * public Pairing getFullPairing(PairingDao dao) throws SQLException { Pairing pairing = null;
     * 
     * if (isEmpty) { // Case where this is a newly created, "empty", SafePairing with // no
     * existing Pairing stored in the database.
     * 
     * // Get a full Service instance for the pairing // TODO add a similar getFull... method to
     * ServiceInfo uk.ac.cam.cl.pico.service.Service service = new
     * uk.ac.cam.cl.pico.service.Service( getServiceInfo().getPublicKey(),
     * getServiceInfo().getName(), getServiceInfo().getUri().toString() );
     * 
     * // Load config values Config config = Config.getInstance(); String provider; String
     * kpgAlgorithm; if ((provider = (String) config.get("crypto.provider")) == null) { throw new
     * IllegalArgumentException("crypto.provider config value is null"); } if ((kpgAlgorithm =
     * (String) config.get("crypto.kpg_algorithm")) == null) { throw new
     * IllegalArgumentException("crypto.kpg_algorithm config value is null"); }
     * 
     * 
     * // Create a key pair for the new pairing KeyPair keyPair = null; try { KeyPairGenerator kpg =
     * KeyPairGenerator.getInstance(kpgAlgorithm, provider); kpg.initialize(256); keyPair =
     * kpg.generateKeyPair(); } catch (NoSuchAlgorithmException e) { throw new
     * CryptoRuntimeException(e); } catch (NoSuchProviderException e) { throw new
     * CryptoRuntimeException(e); }
     * 
     * pairing = new Pairing(service, name, keyPair); LOGGER.debug("Created new Pairing instance");
     * } else { // Case where there is a Pairing corresponding to the supplied // SafePairing
     * already stored in the database.
     * 
     * // Retrieve that Pairing instance: pairing = dao.getPairingById(id);
     * LOGGER.debug("Retrieved existing Pairing instance from database", pairing); }
     * 
     * return pairing; }
     */

    /**
     * Describe the kinds of special objects contained in this Parcelable's marshalled
     * representation.
     * 
     * @returns a bitmask indicating the set of special object types marshalled by the Parcelable (0
     *          in this case).
     */
    @Override
    public int describeContents() {

        return 0;
    }

    /**
     * Marshal this object into a Parcel.
     * 
     * @param flags Additional flags about how the object should be written. May be 0 or
     *        PARCELABLE_WRITE_RETURN_VALUE.
     * @return a parcel
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {

        // Verify the method's preconditions
        if (out == null)
            throw new NullPointerException();

        out.writeBooleanArray(new boolean[] {idIsKnown});
        // Check whether the dateCreated attribute is set yet.
        // This will be null when a new SafePairing is created and a
        // corresponding Pairing has not yet been written to the database
        if (dateCreated == null) {
        	out.writeLong(INVALID_DATE);
        } else {
        	out.writeLong(dateCreated.getTime());
        }
        out.writeInt(pairingId);
        out.writeString(name);
        out.writeParcelable(serviceInfo, flags);       
    }
    
    /**
     * Optionally return an intent to start a detail activity for this pairing.
     * 
     * <p>The default implementation of this method returns an absent optional. Subclasses should
     * override it if appropriate.
     * 
     * @param context to use when creating intent
     * @return optionally an intent to start a detail activity
     */
    public Optional<Intent> detailIntent(final Context context) {
    	return Optional.absent();
    }
    
    /**
     * Start a detail activity for this pairing. Convenience method which starts the intent 
     * returned by {@link #detailIntent(Context)} if present, or does nothing otherwise.
     * 
     * @param context context to use when starting the activity
     */
    public void startDetail(Context context) {
    	Optional<Intent> i = detailIntent(context);
    	if (i.isPresent()) {
    		context.startActivity(i.get());
    	}
    }

	@Override
	public AlertDialog getAuthFailedDialog(Activity context) {
		// Form string for dialog
		final String message = String.format(
				context.getString(R.string.auth_failed_fmt), serviceInfo.getName());
		
    	// Build alert dialog
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
    
	@Override
    public AlertDialog getDelegationFailedDialog(Activity context, AuthToken token) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(R.string.delegation_failed);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Do nothing
			}
		});
		return builder.create();
    }
}
