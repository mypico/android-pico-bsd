/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import uk.ac.cam.cl.pico.android.R;
import android.content.Context;

public final class PgpWordListByteString extends WordListByteStringUtils {
    
	private class InvalidPGPWordException extends InvalidWordException {
		
        private static final long serialVersionUID = 1L;
        private final int position;
		private final String word;

		public InvalidPGPWordException(int position, String word) {
			super(String.format("Word %s at position %d is not in the " +
					(position%2 == 0 ? "even" : "odd") + " PGP word List", word, position));
			this.position = position;
			this.word = word;
		}
		
		public String getHumanErrorMessage() {
			final StringBuilder sb = new StringBuilder();
			sb.append(position);
			sb.append(word);
			return sb.toString();
		}
	}
		
	private final String[] evenWordList;
	private final String[] oddWordList;
	private final Map<String,Integer> invertedEvenWordList;
	private final Map<String,Integer> invertedOddWordList;
	
	public PgpWordListByteString(final Context c) {
		evenWordList = c.getApplicationContext().getResources().getStringArray(R.array.pgp_word_list_even);
		oddWordList = c.getApplicationContext().getResources().getStringArray(R.array.pgp_word_list_odd);
		invertedEvenWordList = invertWordList(evenWordList);
		invertedOddWordList = invertWordList(oddWordList);
	}
	
	private Map<String,Integer> invertWordList(final String[] wordList) {
		final Map<String,Integer> invertedWordList = new HashMap<String,Integer>(wordList.length);
		for (int i=0; i < wordList.length; i++) {
			invertedWordList.put(wordList[i].toLowerCase(Locale.UK), Integer.valueOf((byte) i));
		}
		return invertedWordList;
	}
	
	public String toWords(final byte[] bytes) {
		final StringBuilder sb = new StringBuilder();
		boolean even = true;
		
		for (byte b: bytes) {
			if(even){
				sb.append(evenWordList[b + 128]);
			} else {  // odd
				sb.append(oddWordList[b + 128]);
			}
			sb.append(' ');
			even =! even;
		}		
		return sb.toString();
	}
	
	public byte[] fromWords(String wordsString) throws InvalidWordException {
		final String[] words = wordsString.split(" ");
		return fromWords(words);
	}
	
	public byte[] fromWords(String[] s) throws InvalidPGPWordException {
		final byte[] bytes = new byte[s.length];
		boolean even = true;
		for (int index=0; index<s.length; index++) {
			final Integer b;
			if (even) {
				b = invertedEvenWordList.get(s[index].toLowerCase(Locale.US));
			} else { // odd
				b = invertedOddWordList.get(s[index].toLowerCase(Locale.US));
			}
			if (b==null) throw new InvalidPGPWordException(index, s[index]);
			bytes[index] = (byte) (b - 128);
			even = !even;
		}		
		return bytes;
	}
	
	public String toHex(byte[] bytes) {
		final StringBuilder sb = new StringBuilder();
	    for (byte b : bytes) {
	        sb.append(String.format("%02X ", b));
	    }
	    return sb.toString();
	}

	@Override
	public String[] getWordList() {
		final String[] allWords = new String[evenWordList.length + oddWordList.length];
		System.arraycopy(evenWordList, 0, allWords, 0, evenWordList.length);
		System.arraycopy(oddWordList, 0, allWords, evenWordList.length, oddWordList.length);
		return allWords;
	}
}