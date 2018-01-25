/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.terminal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.crypto.AuthToken;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.google.common.base.Optional;

abstract class WriteAuthToken extends AsyncTask<AuthToken, Void, Boolean> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(
            SocketWriteAuthToken.class.getSimpleName());
	
	protected Context context;
	private Optional<Intent> fallback;
	protected AuthToken token;
	
	public WriteAuthToken(Context context, Optional<Intent> fallback) {
		this.context = context;
		this.fallback = fallback;
	}
	
	protected abstract boolean write(AuthToken token);

	@Override
    protected Boolean doInBackground(AuthToken... params) {
		token = params[0];
		return write(token);
    }

    @Override
    public void onPostExecute(final Boolean wasSuccessful) {
        if (wasSuccessful) {
            LOGGER.debug("Auth token written successfully");
        } else {
            LOGGER.debug("Auth token was not written");
            if (fallback.isPresent()) {
            	LOGGER.debug("Starting fallback...");
            	context.startActivity(fallback.get());
            } else {
            	LOGGER.debug("No fallback specified");
            }
        }
    }
}
