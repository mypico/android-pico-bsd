/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.backup;

import java.util.EnumSet;

import uk.ac.cam.cl.pico.android.backup.BackupProviderFragment.RestoreOption;

import com.google.common.base.Optional;

/**
 * Interface implemented by each of the different backup providers supported by Pico: 
 * DropBox, SD Card, ...
 * 
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
public interface IBackupProvider {

    /**
     * Enumeration of the backup providers supported by Pico.
     * 
     * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
     *
     */
    public enum BackupType {   
        NONE("None"),
        DROPBOX("Dropbox"),
        ONEDRIVE("Microsoft OneDrive"),
        GOOGLEDRIVE("Google Drive"),
        SDCARD("SD Card");
        
		private final String providerName;
        BackupType(final String providerName) {
            this.providerName = providerName;
        }
        
        public String getProviderName() {
        	return providerName;
        }
    } 
    
    /**
     * No backup provider selected.
     */
	static final Optional<IBackupProvider> NO_BACKUP_PROVIDER =
			Optional.<IBackupProvider>absent();	
    
    /**
     * Get the type of the backup provider.
     */
    BackupType getBackupType();
    
    /**
     * Perform a backup of the Pico database using the backup mechanism.
     * The calling Activity must implement OnCreateBackupListener, through which the
     * result is returned.
     */
    void backup();

    /**
     * Query whether the backup provider contains Pico backups.     * 
     * The calling Activity must implement OnQueryBackupListener, through which the
     * result is returned.
     */
    void isEmpty();
    
    /**
     * Restore the Pico database from the latest backup.
     * The calling Activity must implement OnRestoreBackupListener, through which the
     * result is returned.
     */
    void restoreLatestBackup(); 
    
    /**
     * Restore the Pico database from a user selected backup.
     * The calling Activity must implement OnRestoreBackupListener, through which the
     * result is returned.
     */
    void restoreBackup();        
    
    /*
     * TODO 
     */
    public void decryptRestoredBackup(final byte[] userSecret);
    
	/**
	 * Returns the RestoreOptions supported by the BackupProvider.
	 * By default all restore options are supported. Sub-classes of the BackupProviderFragment
	 * for specific providers (such as OneDriver, DropBox) may choose which of these methods they
	 * support.
	 * @return EnumSet of RestoreOptions supported by the BackupProviderFragment.
	 */
	public EnumSet<RestoreOption> getRestoreOptions();
}