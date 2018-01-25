/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.data;

import java.util.HashMap;
import java.util.Map;

import uk.ac.cam.cl.pico.visualcode.LensPairingVisualCode;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * TODOI.
 * <p>
 * TODO
 * 
 * @see TODO
 * 
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 * 
 */
final public class ParcelableCredentials implements Parcelable {

    /**
     * Generates a TODO instance from a Parcelable class, hose data had previously been written by
     * Parcelable.writeToParcel().
     * 
     * @see android.os.Parcelable.Creator
     */
    public static final Parcelable.Creator<ParcelableCredentials> CREATOR =
            new Parcelable.Creator<ParcelableCredentials>() {

                /**
                 * Create a new instance of the Parcelable class. A ServiceInfo is instantiated
                 * given a Parcel whose data had previously been written by
                 * Parcelable.writeToParcel().
                 * 
                 * @param Parcel a Parcel whose data had previously been written by
                 *        Parcelable.writeToParcel()
                 * @return a unmarshaled ServiceInfo instance
                 */
                @Override
                public ParcelableCredentials createFromParcel(final Parcel source) {

                    final Map<String, String> parcelledCredentials =
                            new HashMap<String, String>();
                    final int size = source.readInt();
                    for (int i = 0; i < size; i++) {

                        final String key = source.readString();
                        final String value = source.readString();
                        parcelledCredentials.put(key, value);
                    }
                    return new ParcelableCredentials(parcelledCredentials);
                }

                /**
                 * Create a new array of the Parcelable class.
                 * 
                 * @param size the size of the array to create.
                 * @return the array
                 */
                @Override
                public ParcelableCredentials[] newArray(final int size) {
                    return new ParcelableCredentials[size];
                }
            };

    private final Map<String, String> credentials;

    public ParcelableCredentials(final Map<String, String> credentials) {
        if (credentials == null) {
            throw new NullPointerException("ParcelableCredential cannot be " +
                    "created from a null credentials");
        }
        this.credentials = credentials;
    }

    @Override
    public String toString() {
        return credentials.toString();
    }

    /**
     * TODO.
     * 
     * @return tTODO.
     */
    public Map<String, String> getCredentials() {
        return credentials;
    }

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
    public void writeToParcel(final Parcel out, final int flags) {

        out.writeInt(credentials.size());
        for (String key : credentials.keySet()) {
            out.writeString(key);
            out.writeString(credentials.get(key));
        }
    }

    /**
     * Factory constructor method.
     * 
     * @param visualCode the VisualCode specifying the service.
     * @return new Credentials instance
     */
    public static ParcelableCredentials fromVisualCode(
            final LensPairingVisualCode visualCode) {
        return new ParcelableCredentials(visualCode.getCredentials());
    }

}
