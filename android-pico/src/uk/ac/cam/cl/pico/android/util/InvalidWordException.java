/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.util;

/**
 * TODO
 * 
 * @author Chris Warrington <cw471@cl.cam.ac.uk>
 *
 */
public abstract class InvalidWordException extends Exception {

    private static final long serialVersionUID = 1L;
    
    public InvalidWordException(final String description) {
		super(description);
	}
	public abstract String getHumanErrorMessage();
}