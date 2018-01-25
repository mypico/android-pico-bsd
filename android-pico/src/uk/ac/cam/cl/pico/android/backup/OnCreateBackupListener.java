/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.backup;

import com.google.common.base.Optional;

/**
 * Listener interface for communicating with the calling object.
 * 
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
public interface OnCreateBackupListener {
	
	static final public Optional<OnCreateBackupListener> NO_LISTENER = 
			Optional.<OnCreateBackupListener>absent();

    /**
     * Callback method on initiating a backup of the Pico pairings and service database.
     */
    void onCreateBackupStart();
    
    /**
     * Callback method on successful backup of the Pico pairings and service database.
     */
    void onCreateBackupSuccess();
    
    /**
     * Callback method on failure backing up the Pico pairings and service database.
     */
    void onCreateBackupFailure();
}