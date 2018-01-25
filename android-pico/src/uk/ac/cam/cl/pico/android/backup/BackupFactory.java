/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.backup;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import com.google.common.base.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.android.core.PicoApplication;
import uk.ac.cam.cl.pico.android.backup.IBackupProvider.BackupType;

/**
 * Factory class for creating a BackupProviderFragment.
 * 
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
public final class BackupFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(
    		BackupFactory.class.getSimpleName());
    
    /**
     * Key used to stored the configured BackupProvider in the of the app's SharedPreferences.
     */
    private final static String BACKUP_KEY = "Backup";

    /**
     * Factory method for creating a BackupProviderFragement. 
     * 
     * @param backupType The BackupProviderFragement to create.
     * @param activity The Activity to which the Fragment will be attached.
     * @return The new BackupProviderFragment as an IBackupProvider and wrapped as an Optional.
     */
    public static Optional<IBackupProvider> newBackup(final BackupType backupType,
    		final Activity activity) {
    	// Verify the method preconditions
    	checkNotNull(backupType);
    	checkNotNull(activity);
    	
    	final Optional<IBackupProvider> backupProvider;
        switch (backupType) {
        case DROPBOX:
            LOGGER.debug("DropBox backup configured");
            backupProvider = Optional.of(addDropBoxFragment(activity));
            break;
        case ONEDRIVE:
            LOGGER.debug("OneDrive backup configured");
            backupProvider = Optional.of(addOneDriveFragment(activity));
            break;
        case GOOGLEDRIVE:
            LOGGER.debug("Google Drive backup configured");
            backupProvider = Optional.of(addGoogleDriveFragment(activity));
            break;
        case SDCARD:
            LOGGER.debug("SD card backup configured");
            backupProvider = Optional.of(addSdCardFragment(activity));
            break;
        case NONE:
        default:
        	LOGGER.error("No backup mechanism specified");
            backupProvider = IBackupProvider.NO_BACKUP_PROVIDER;
            break;
        }
        return backupProvider;
    }
    
    /**
     * Factory method for restoring the backup provider. 
     * 
     * @param activity The Activity to which the Fragment will be attached.
     * @return The new BackupProviderFragment as an IBackupProvider and wrapped as an Optional.
     */
    public static Optional<IBackupProvider> newBackup(final Activity activity) {
    	// Verify the method preconditions
    	checkNotNull(activity);
    	
    	return newBackup(restoreBackupType(), activity);
    }
    
    /**
     * Returns the configured backup mechanism (or none) from the shared preferences.
     * @return The configured backup mechanism (BackupType).
     */
    public static BackupType restoreBackupType() {
        final SharedPreferences preferences =
        		PreferenceManager.getDefaultSharedPreferences(PicoApplication.getContext());
        final String backupPref = preferences.getString(BACKUP_KEY,"");
        if (!isNullOrEmpty(backupPref)) {
            try {
                final BackupType backupType = BackupType.valueOf(backupPref);
                return backupType;
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Backup is invalid {}", backupPref);
                return BackupType.NONE;
            }            
        }
        return BackupType.NONE;
    }

    /**
     * Stores the configured backup mechanism in the shared preferences.
     * @param backupType The backup type to persist.
     */
    public static void persistBackupType(final BackupType backupType) {
    	// Verify the method preconditions
    	checkNotNull(backupType);

        final SharedPreferences preferences =
        		PreferenceManager.getDefaultSharedPreferences(PicoApplication.getContext());
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(BACKUP_KEY, backupType.toString());
        editor.commit();         
    }  
    
    /**
     * Add the DropBox fragment UI to the activity.
     * @param activity The Activity to which the Fragment will be attached.
     * @param onConfigureBackupListener The callback interface used
     * notify the Activity that the BackupProviderFragment has been configured.
     * @return The new BackupProviderFragment as an IBackupProvider.
     */
    private static IBackupProvider addDropBoxFragment(final Activity activity) {      
    	// Create the DropBoxBackupProviderFragment; setReatainInstance(true) ensures that the
    	// instance state of the Fragment is maintained across reconfiguration of the
    	// Activity
        final DropboxBackupProviderFragment backupProviderFragment =
        		new DropboxBackupProviderFragment();
        return addBackupProviderFragment(activity, backupProviderFragment);
    }
    
    /**
     * Add the OneDrive fragment UI to the activity.
     * @param activity The Activity to which the Fragment will be attached.
     * @param onConfigureBackupListener The callback interface used
     * notify the Activity that the BackupProviderFragment has been configured.
     * @return The new BackupProviderFragment as an IBackupProvider.
     */
    private static IBackupProvider addOneDriveFragment(final Activity activity) {   
    	// Create the OneDriveBackupProviderFragment; setReatainInstance(true) ensures that the
    	// instance state of the Fragment is maintained across reconfiguration of the Activity
        final OneDriveBackupProviderFragment backupProviderFragment =
        		new OneDriveBackupProviderFragment();
        return addBackupProviderFragment(activity, backupProviderFragment);
    }
   
    /**
     * Add the GoogleDrive fragment UI to the activity.
     * @param activity The Activity to which the Fragment will be attached.
     * @param onConfigureBackupListener The callback interface used
     * notify the Activity that the BackupProviderFragment has been configured.
     * @return The new BackupProviderFragment as an IBackupProvider.
     */
    private static IBackupProvider addGoogleDriveFragment(final Activity activity) {   
    	// Create the GoogleDriveBackupProviderFragment; setReatainInstance(true) ensures that the
    	// instance state of the Fragment is maintained across reconfiguration of the Activity
        final GoogleDriveBackupProviderFragment backupProviderFragment =
        		new GoogleDriveBackupProviderFragment();
        return addBackupProviderFragment(activity, backupProviderFragment);
    }
    
    /**
     * Add the SD card fragment UI to the activity.
     * @param activity The Activity to which the Fragment will be attached.
     * @param onConfigureBackupListener The callback interface used
     * notify the Activity that the BackupProviderFragment has been configured.
     * @return The new BackupProviderFragment as an IBackupProvider.
     */
    private static IBackupProvider addSdCardFragment(final Activity activity) { 
    	// Create the SdCardBackupProviderFragmeent; setReatainInstance(true) ensures that the
    	// instance state of the Fragment is maintained across reconfiguration of the Activity
        final SdCardBackupProviderFragment backupProviderFragment =
        		new SdCardBackupProviderFragment();
        return addBackupProviderFragment(activity, backupProviderFragment);
    }
    
    /**
     * Add the fragment UI to the activity.
     * @param activity The Activity to which the Fragment will be attached.
     * @param backupProviderFragement The BackupProviderFragement to add.
     * @return The new BackupProviderFragment as an IBackupProvider.
     */
    private static IBackupProvider addBackupProviderFragment(final Activity activity,
    		final BackupProviderFragment backupProviderFragment) { 
        // Add the BackupProviderFragment to the Activity
    	backupProviderFragment.setRetainInstance(true);
        final FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
        transaction.add(backupProviderFragment, BackupProviderFragment.TAG);
        transaction.commit();
        return backupProviderFragment;
    }
}