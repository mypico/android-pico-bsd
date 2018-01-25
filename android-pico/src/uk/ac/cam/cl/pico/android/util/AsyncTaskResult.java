/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.util;

/**
 * TODO
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 * @param <T> TODO
 */
public class AsyncTaskResult<T> {
	
	final private T result;
	final private Exception error;
		
	public T getResult() {
	    return result;
	}
	
	public Exception getError() {
	    return error;
	}
		
	public AsyncTaskResult(final T result) {
	    super();
	    this.result = result;
	    this.error = null;
	}
		
	public AsyncTaskResult(final Exception error) {
	    super();
	    this.result = null;
	    this.error = error;	    
	}
}