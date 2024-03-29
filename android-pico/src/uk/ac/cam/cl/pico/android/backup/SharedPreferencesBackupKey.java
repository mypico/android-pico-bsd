/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.backup;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.backup.BackupKey;
import uk.ac.cam.cl.pico.backup.BackupKeyException;
import uk.ac.cam.cl.pico.backup.BackupKeyInvalidLengthException;
import uk.ac.cam.cl.pico.backup.BackupKeyRestoreStateException;
import uk.ac.cam.cl.pico.android.core.PicoApplication;
import uk.ac.cam.cl.pico.comms.org.apache.commons.codec.binary.Base64;

/**
 * BackupKey that persists and restores through using the app's SharedPreferences.
 * Note: MODE_PRIVATE is default (this ensures data can only be read by either the application
 * or another application with the same user ID).
 * 
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
public final class SharedPreferencesBackupKey extends BackupKey {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(
    		SharedPreferencesBackupKey.class.getSimpleName());
    
    /**
     * Key used to stored the configured BackupKey in the of the app's SharedPreferences.
     */
	private final static String BACKUP_USER_SECRET_KEY = "BackupUserSecret";
    
	private SharedPreferencesBackupKey() {
		super();	
	}

	private SharedPreferencesBackupKey(final byte[] userSecret)
			throws BackupKeyInvalidLengthException {
		super(userSecret);	
	}
    
	/**
	 * Construct a new BackupKey instance.
	 * @param userSecret the user secret used as a basis to create the BackupKey.
	 * @return the new BackupKey instance.
	 * @throws BackupKeyInvalidLengthException
	 */
	public static BackupKey newInstance(final byte[] userSecret)
			throws BackupKeyInvalidLengthException {
		// Verify the method's preconditions
		checkNotNull(userSecret);
		
		final SharedPreferencesBackupKey backupKey = new SharedPreferencesBackupKey(userSecret);
		backupKey.persist();
		return backupKey;
	}
	
	/**
	 * Construct a new random BackupKey instance.
	 * @return the new random BackupKey instance.
	 */
	public static BackupKey newRandomInstance() {
		final SharedPreferencesBackupKey backupKey = new SharedPreferencesBackupKey();
		backupKey.persist();
		return backupKey;
	}
	
    /**
     * Persists the BackupKey to the app's SharedPreferences.
     */
    private void persist() {
        final SharedPreferences preferences =
        		PreferenceManager.getDefaultSharedPreferences(PicoApplication.getContext());
        final SharedPreferences.Editor editor = preferences.edit();
		editor.putString(BACKUP_USER_SECRET_KEY, Base64.encodeBase64String(userSecret));
        editor.commit();             
    }
    
    /**
     * Restores the BackupKey from the shared preferences.
     * @return The restored BackupKey instance (wrapped as an Optional)
     * @throws BackupKeyRestoreStateException if there is not BackupKey to restore or on an
     * error constructing the BackupKey to be restored.
     */
	public static BackupKey restoreInstance() throws BackupKeyRestoreStateException {
        final SharedPreferences preferences =
        		PreferenceManager.getDefaultSharedPreferences(PicoApplication.getContext());
        final String backupUserSecret = preferences.getString(BACKUP_USER_SECRET_KEY, "");
        if (isNullOrEmpty(backupUserSecret)) {
           throw new BackupKeyRestoreStateException("No BackupKey to restore");
        } else {
        	LOGGER.debug("Restoring user secret {}", backupUserSecret);
        	try {
        		return new SharedPreferencesBackupKey(Base64.decodeBase64(backupUserSecret));
        	} catch (BackupKeyException e) {
        		LOGGER.error("Error restoring the backup key", e);
                throw new BackupKeyRestoreStateException("BackupKey can't be restored");
        	}
        }
	}
}