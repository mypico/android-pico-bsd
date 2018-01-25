/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.terminal;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.crypto.AuthToken;
import android.content.Context;
import android.content.Intent;

import com.google.common.base.Optional;

final public class SocketWriteAuthToken extends WriteAuthToken {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            SocketWriteAuthToken.class.getSimpleName());

    private final SocketAddress address;
    private final int timeout;

    public SocketWriteAuthToken(Context context, Optional<Intent> fallback, SocketAddress address, int timeout) {
    	super(context, fallback);
        
        this.address = address;
        this.timeout = timeout;
    }

    @Override
    protected boolean write(AuthToken token) {
        LOGGER.debug("Writing auth token to socket: {}", address);
        
        // Try to connect to the terminal, signalling failure (return false)
        // after some timeout.
        final Socket socket = new Socket();
        try {
            socket.connect(address, timeout);
            try {
	            // Once the socket is connected, try to write the full form of the auth
	            // token to it as one line. Signalling failure if an IOException
	            // occurs.
	            final BufferedWriter writer = new BufferedWriter(
	                    new OutputStreamWriter(socket.getOutputStream()));
	            try {
	                writer.write(token.getFull());
	                writer.newLine();
	                writer.flush();
	            } finally {
		            writer.close();
	            }
            } finally {
                socket.close();
            }
        } catch (SocketTimeoutException e) {
            LOGGER.warn("Failed to write auth token {}", e);
            return false;
        } catch (IOException e) {
            LOGGER.warn("Failed to write auth token {}", e);
            return false;
        } 

        // If all that succeeded without one of the return false's getting hit
        return true;
    }
}