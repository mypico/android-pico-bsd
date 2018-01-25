/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.pairing;

import java.io.IOException;
import java.util.List;

import uk.ac.cam.cl.pico.android.core.PicoService;
import uk.ac.cam.cl.pico.android.data.SafeKeyPairing;
import uk.ac.cam.cl.pico.android.data.SafeService;
import android.os.AsyncTask;

abstract class GetKeyPairingsTask
        extends AsyncTask<SafeService, Void, List<SafeKeyPairing>> {

    private PicoService picoService;
    protected Throwable problem;

    public GetKeyPairingsTask(PicoService picoService) {
        this.picoService = picoService;
    }

    @Override
    protected List<SafeKeyPairing> doInBackground(SafeService... params) {
        SafeService service = params[0];
        try {
            return picoService.getKeyPairings(service);
        } catch (IOException e) {
            problem = e;
            return null;
        }
    }

    public abstract void onPostExecute(List<SafeKeyPairing> pairings);
}
