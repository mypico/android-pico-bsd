/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.util;

public final class InvalidPGPWordException extends InvalidWordException {
	
    private static final long serialVersionUID = 1L;
    private final int position;
	private final String word;

	public InvalidPGPWordException(final int position, final String word) {
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