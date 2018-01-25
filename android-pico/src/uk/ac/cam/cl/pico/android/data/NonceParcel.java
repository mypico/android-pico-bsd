/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.data;

import android.os.Parcel;
import android.os.Parcelable;
import uk.ac.cam.cl.pico.crypto.Nonce;

public class NonceParcel implements Parcelable {
	
	public static final Parcelable.Creator<NonceParcel> CREATOR =
			new Parcelable.Creator<NonceParcel>() {

				@Override
				public NonceParcel createFromParcel(Parcel source) {
					final byte[] bytes = new byte[source.readInt()];
					source.readByteArray(bytes);
					return new NonceParcel(Nonce.getInstance(bytes));
				}

				@Override
				public NonceParcel[] newArray(int size) {
					return new NonceParcel[size];
				}
	};

	private final Nonce nonce;
	
	public NonceParcel(Nonce nonce) {
		this.nonce = nonce;
	}
	
	public Nonce getNonce() {
		return this.nonce;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		final byte[] bytes = nonce.getValue();
		dest.writeInt(bytes.length);
		dest.writeByteArray(bytes);
	}
}
