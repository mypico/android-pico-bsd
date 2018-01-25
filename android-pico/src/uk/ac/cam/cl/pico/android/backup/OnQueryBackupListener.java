/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.backup;

/**
 * Listener interface for communicating with the calling object.
 * 
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
public interface OnQueryBackupListener {
  
    void onQueryBackupIsNotEmpty();
    void onQueryBackupIsEmpty();
    void onQueryBackupFailure();
}