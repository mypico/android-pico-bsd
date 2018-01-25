/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.core;

import java.io.IOException;
import java.util.List;

import uk.ac.cam.cl.pico.android.data.SafeKeyPairing;
import uk.ac.cam.cl.pico.android.data.SafeLensPairing;
import uk.ac.cam.cl.pico.android.data.SafePairing;
import uk.ac.cam.cl.pico.android.data.SafeService;
import uk.ac.cam.cl.pico.android.data.SafeSession;
import uk.ac.cam.cl.pico.data.pairing.PairingNotFoundException;
import uk.ac.cam.cl.pico.data.terminal.Terminal;
import android.net.Uri;

import com.google.common.base.Optional;

public interface PicoService {

    public List<SafeKeyPairing> getKeyPairings(SafeService service)
            throws IOException;

    public List<SafeLensPairing> getLensPairings(
            SafeService service) throws IOException;

    public SafeSession keyAuthenticate(SafeKeyPairing pairing)
            throws IOException, PairingNotFoundException;

    public SafeSession lensAuthenticate(SafeLensPairing pairing,
            Uri loginUri, String loginForm, String cookieString) throws IOException, PairingNotFoundException;

    public SafePairing renamePairing(SafePairing pairing, String name)
            throws IOException, PairingNotFoundException;

    public void pauseSession(SafeSession sessionInfo);

    public void resumeSession(SafeSession sessionInfo);

    public void closeSession(SafeSession sessionInfo);
    
    public List<Terminal> getTerminals() throws IOException;
    
    public interface GetTerminalsCallback {
    	public void onGetTerminalsResult(List<Terminal> result);
    	public void onGetTerminalsError(IOException e);	
    }
    
    public void getTerminals(GetTerminalsCallback callback);
    
    public interface GetTerminalCallback {
    	public void onGetTerminalResult(Optional<Terminal> result);
    }

	void getTerminal(byte[] terminalCommitment, GetTerminalCallback callback);
}
