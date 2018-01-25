/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.data;

import java.io.IOException;

import uk.ac.cam.cl.pico.crypto.AuthToken;
import uk.ac.cam.cl.pico.crypto.AuthTokenFactory;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A decorator class that makes an AuthToken instance Parcelable.
 * 
 * @author Max Spencer <ms955@cl.cam.ac.uk>
 * 
 */
final public class ParcelableAuthToken implements AuthToken, Parcelable {

    public static final Parcelable.Creator<ParcelableAuthToken> CREATOR =
            new Parcelable.Creator<ParcelableAuthToken>() {

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
                public ParcelableAuthToken createFromParcel(Parcel source) {
                    final byte[] tokenBytes = new byte[source.readInt()];
                    source.readByteArray(tokenBytes);
                    try {
                        return new ParcelableAuthToken(
                                AuthTokenFactory.fromByteArray(tokenBytes));
                    } catch (IOException e) {
                        throw new RuntimeException(
                                "Exception occured whilst creating " +
                                        "ParcelableSessionAuthToken from Parcel", e);
                    }
                }

                @Override
                public ParcelableAuthToken[] newArray(int size) {
                    return new ParcelableAuthToken[size];
                }
            };

    private AuthToken authToken;

    public ParcelableAuthToken(AuthToken authToken) {
        this.authToken = authToken;
    }

    @Override
    public String getFull() {
        return authToken.getFull();
    }

    @Override
    public String getFallback() {
        return authToken.getFallback();
    }

    @Override
    public byte[] toByteArray() throws IOException {
        return authToken.toByteArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        try {
            final byte[] tokenBytes = authToken.toByteArray();
            out.writeInt(tokenBytes.length);
            out.writeByteArray(tokenBytes);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Exception occured writing ParcelableSessionAuthToken " +
                            "to Parcel", e);
        }
    }
}
