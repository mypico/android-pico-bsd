/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.pairing;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.android.data.SafeKeyPairing;
import android.app.Activity;
import android.view.View;
import android.widget.ListView;

final public class KeyPairingListFragment extends PairingListFragment {

    public interface Listener {
        public void onNoPairings();

        public void onSinglePairing(SafeKeyPairing pairing);

        public void onMultiplePairings(int count);

        public void onPairingClicked(SafeKeyPairing pairing);
    }

    private class UpdateTask extends GetKeyPairingsTask {

        public UpdateTask() {
            super(picoService);
        }

        @Override
        public void onPostExecute(final List<SafeKeyPairing> pairings) {
            LOGGER.info(
                    "{} pairings retrieved from the database",
                    pairings.size());

            // Notify parent activity of the pairings returned if any. The
            // different callbacks allow the activity to take different
            // actions in each case.
            final int count = pairings.size();
            if (count == 0) {
                listener.onNoPairings();
            } else if (count == 1) {
                listener.onSinglePairing(pairings.get(0));
            } else if (count > 1) {
                listener.onMultiplePairings(count);

                // Update list adapter
                adapter.clear();
                adapter.addAll(pairings);
                adapter.notifyDataSetChanged();
            }
            else {
                LOGGER.error("Cannot have {} pairings", count);
            }
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(
            KeyPairingListFragment.class.getSimpleName());

    private Listener listener;

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        listener = (Listener) activity; // throws ClassCastException
    }

    @Override
    protected void updatePairingList() {
        // Update the list of pairings (happens asynchronously)
        LOGGER.debug("Updating pairings list...");
        new UpdateTask().execute(serviceInfo);
    }

    @Override
    public void onListItemClick(
            final ListView l,
            final View v,
            final int position,
            final long id) {
        final SafeKeyPairing pairing = (SafeKeyPairing) adapter.getItem(position);
        listener.onPairingClicked(pairing);
    }
}
