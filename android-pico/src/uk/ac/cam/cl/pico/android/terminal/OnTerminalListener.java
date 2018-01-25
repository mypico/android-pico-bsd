/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.terminal;

/**
 * Listener interface for communicating with the calling object.
 * 
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
public interface OnTerminalListener {
	
    /**
     * Callback method on successfully adding a new Terminal.
     */
    void onAddTerminalSuccess(final String terminalName);

    /**
     * Callback method on adding a duplicate Terminal.
     */
    void onAddTerminalDuplicate(final String terminalName);

    /**
     * Callback method on failure to add a new Terminal.
     */
    void onAddTerminalFailure();

    /**
     * Callback method on successfully deleting a Terminal.
     */
    void onRemoveTerminalSuccess();
    
    /**
     * Callback method on failure to delete a Terminal.
     */
    void onRemoveTerminalFailure();
}