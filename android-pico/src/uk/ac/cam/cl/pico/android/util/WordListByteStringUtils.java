/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.util;


/**
 * TODO
 * 
 * @author TODO
 *
 */
public abstract class WordListByteStringUtils {
    
	public abstract String toWords(byte[] bytes);
	
	public abstract byte[] fromWords(String s) throws InvalidWordException;
	
	public abstract String[] getWordList();
}