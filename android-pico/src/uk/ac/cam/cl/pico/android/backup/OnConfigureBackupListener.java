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
public interface OnConfigureBackupListener {
	
	static final public Optional<OnConfigureBackupListener> NO_LISTENER = 
			Optional.<OnConfigureBackupListener>absent();

    /**
     * Callback method on configuring a backup mechanism.
     * @param backup The backup provider that was successfully configured.
     */
    void onConfigureBackupSuccess(final IBackupProvider backup);

    /**
     * Callback method on cancelling the configuration of a backup mechanism.
     */
    void onConfigureBackupCancelled();
    
    /**
     * Callback method on failure to configure a backup mechanism.
     */
    void onConfigureBackupFailure();
}