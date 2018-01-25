/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.data;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Parcel;
import android.os.Parcelable;
import uk.ac.cam.cl.pico.crypto.CryptoFactory;
import uk.ac.cam.cl.pico.data.terminal.Terminal;

public class ParcelableTerminal implements Parcelable {
	
	private static final Logger LOGGER =
			LoggerFactory.getLogger(ParcelableTerminal.class.getSimpleName());
	
	final private int id;
	final private String name;
	final private byte[] commitment;
	final private PublicKey picoPublicKey;
	final private PrivateKey picoPrivateKey;
	
	public static final Parcelable.Creator<ParcelableTerminal> CREATOR =
			new Parcelable.Creator<ParcelableTerminal>() {

				@Override
				public ParcelableTerminal createFromParcel(final Parcel source) {
					final int id = source.readInt();
					final String name = source.readString();
					final byte[] commitment = new byte[source.readInt()];
					source.readByteArray(commitment);
					final byte[] pubKeyBytes = new byte[source.readInt()];
					source.readByteArray(pubKeyBytes);
					final byte[] privKeyBytes = new byte[source.readInt()];
					source.readByteArray(privKeyBytes);
					
					try {
						final KeyFactory kf = CryptoFactory.INSTANCE.ecKeyFactory();					
						PublicKey pubKey;					
						pubKey = kf.generatePublic(new X509EncodedKeySpec(pubKeyBytes));					
						final PrivateKey privKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privKeyBytes));						
						return new ParcelableTerminal(id, name, commitment, pubKey, privKey);
					} catch (InvalidKeySpecException e) {
						LOGGER.error("InvalidKeySpecError generating ParcelableTerminal");
						return null;
					}
				}

				@Override
				public ParcelableTerminal[] newArray(int size) {
					return new ParcelableTerminal[size];
				}
	};

	public ParcelableTerminal(final Terminal terminal) {
		this.id = terminal.getId();
		this.name = terminal.getName();
		this.commitment = terminal.getCommitment();
		this.picoPublicKey = terminal.getPicoPublicKey();
		this.picoPrivateKey = terminal.getPicoPrivateKey();
	}

	public ParcelableTerminal(final int id, final String name,
			final byte[] commitment, final PublicKey picoPublicKey,
			final PrivateKey picoPrivateKey) {
		this.id = id;
		this.name = name;
		this.commitment = commitment;
		this.picoPublicKey = picoPublicKey;
		this.picoPrivateKey = picoPrivateKey;
	}
	
	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public byte[] getCommitment() {
		return commitment;
	}
	
	public PublicKey getPicoPublicKey() {
		return picoPublicKey;
	}
	public PrivateKey getPicoPrivateKey() {
		return picoPrivateKey;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeInt(id);
		dest.writeString(name);
		dest.writeInt(commitment.length);
		dest.writeByteArray(commitment);
		dest.writeInt(picoPublicKey.getEncoded().length);
		dest.writeByteArray(picoPublicKey.getEncoded());
		dest.writeInt(picoPrivateKey.getEncoded().length);
		dest.writeByteArray(picoPrivateKey.getEncoded());
	}
}
