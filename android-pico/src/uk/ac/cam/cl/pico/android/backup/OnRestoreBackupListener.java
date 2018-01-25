/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.backup;

import com.google.common.base.Optional;

/**
 * Listener interface for  communicating with the calling object.
 * 
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
public interface OnRestoreBackupListener {
  
	static final public Optional<OnRestoreBackupListener> NO_LISTENER =
			Optional.<OnRestoreBackupListener>absent();

    /**
     * Callback method on initiating restoring a backup.
     */
    void onRestoreBackupStart();

    /**
     * Callback method on downloading  a backup to restore.
     */
    void onRestoreBackupDownloaded();
    
    /**
     * Callback method on restoring a backup.
     */
    void onRestoreBackupSuccess();

    /**
     * Callback method on cancelling the restoring a backup.
     */
    void onRestoreBackupCancelled();
    
    /**
     * Callback method on encountering an error restoring a backup.
     */
    void onRestoreBackupFailure();
}