/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.terminal;

import java.io.IOException;
import java.net.URL;
import java.security.KeyPair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.comms.JsonMessageSerializer;
import uk.ac.cam.cl.pico.comms.RendezvousSigmaProxy;
import uk.ac.cam.cl.pico.crypto.AuthToken;
import uk.ac.cam.cl.pico.crypto.NewSigmaProver;
import uk.ac.cam.cl.pico.crypto.NewSigmaProver.ProverAuthRejectedException;
import uk.ac.cam.cl.pico.crypto.NewSigmaProver.VerifierAuthFailedException;
import uk.ac.cam.cl.pico.crypto.ProtocolViolationException;
import uk.ac.cam.cl.pico.data.terminal.Terminal;
import uk.ac.cam.cl.rendezvous.RendezvousChannel;
import android.content.Context;
import android.content.Intent;

import com.google.common.base.Optional;

public class RendezvousWriteAuthToken extends WriteAuthToken {
	
	private static final Logger LOGGER = 
			LoggerFactory.getLogger(RendezvousWriteAuthToken.class.getSimpleName());
	private static final Optional<Intent> NO_FALLBACK = Optional.absent();
	
	private final RendezvousChannel channel;
	private final Terminal terminal;

	public RendezvousWriteAuthToken(
			Context context, Optional<Intent> fallback, URL url, Terminal terminal) {
		super(context, fallback);
		channel = new RendezvousChannel(url);
		this.terminal = terminal;
	}
	
	public RendezvousWriteAuthToken(Context context, URL url, Terminal terminal) {
		this(context, NO_FALLBACK, url, terminal);
	}

	@Override
	protected boolean write(AuthToken token) {
		LOGGER.debug("Sending auth token to {}", channel.getUrl().toString());
		
		// Make a proxy for the terminal verifier
		final RendezvousSigmaProxy proxy = new RendezvousSigmaProxy(
				channel, new JsonMessageSerializer());
		
		final NewSigmaProver prover;
		try {
			prover = new NewSigmaProver(
					NewSigmaProver.VERSION_1_1,
					new KeyPair(terminal.getPicoPublicKey(), terminal.getPicoPrivateKey()),
					token.toByteArray(),
					proxy,
					terminal.getCommitment());
		} catch (IOException e) {
			LOGGER.warn("could not serialize auth token", e);
			return false;
		}
		
		try {
			prover.prove();
		} catch (IOException e) {
			LOGGER.warn("unable to authenticate to terminal", e);
			return false;
		} catch (ProverAuthRejectedException e) {
			LOGGER.warn("terminal rejected authentication", e);
			return false;
		} catch (ProtocolViolationException e) {
			LOGGER.warn("terminal violated the authentication protocol", e);
			return false;
		} catch (VerifierAuthFailedException e) {
			LOGGER.warn("terminal failed to authenticate", e);
			return false;
		}
		
		// Otherwise, if no exceptions were thrown, the prover authenticated and transferred the
		// token as its extra data successfully!
		return true;
    }
}
