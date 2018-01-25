/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.pairing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.android.core.PicoService;
import uk.ac.cam.cl.pico.android.core.PicoServiceImpl;
import uk.ac.cam.cl.pico.android.core.PicoServiceImpl.PicoServiceBinder;
import uk.ac.cam.cl.pico.android.data.SafePairing;
import uk.ac.cam.cl.pico.android.data.SafeService;
import android.app.ListFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Abstract base class for PairingListFragments, a concrete subclass such as
 * {@link KeyPairingListFragment} or {@link LensPairingListFragment} must be used.
 * 
 * @author Max Spencer <ms955@cl.cam.ac.uk>
 * 
 */
public abstract class PairingListFragment
        extends ListFragment implements ServiceConnection {

    private final static Logger LOGGER =
            LoggerFactory.getLogger(PairingListFragment.class.getSimpleName());

    protected ArrayAdapter<SafePairing> adapter;
    protected SafeService serviceInfo;

    protected PicoService picoService;
    private boolean bound = false;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get service info from parent activity's intent
        final Intent intent = getActivity().getIntent();
        serviceInfo = (SafeService) intent.getParcelableExtra(
                SafeService.class.getCanonicalName());

        if (serviceInfo != null) {
            LOGGER.debug("Got service info from activity's intent");
        } else {
            LOGGER.warn("Failed to get service info from activity's intent");
        }

        LOGGER.debug("Fragment created");
    }

    @Override
    public View onCreateView(
            final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        adapter = new ArrayAdapter<SafePairing>(
                inflater.getContext(),
                android.R.layout.simple_list_item_1
                );
        setListAdapter(adapter);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Bind to the Pico service
        final Intent intent =
                new Intent(getActivity(), PicoServiceImpl.class);
        getActivity().bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    public void onResume() {
        super.onResume();

        // If the pico service is bound, then update the pairing list.
        if (bound) {
            updatePairingList();
        }
    }

    protected abstract void updatePairingList();

    @Override
    public void onStop() {
        super.onStop();
        // Unbind from the Pico service
        if (bound) {
            getActivity().unbindService(this);
            bound = false;
        }
    }

    @Override
    public abstract void onListItemClick(
            final ListView l,
            final View v,
            final int position,
            final long id);

    // implements ServiceConnection

    @Override
    public void onServiceConnected(
            final ComponentName name, final IBinder binder) {
        picoService = ((PicoServiceBinder) binder).getService();
        bound = true;

        updatePairingList();
    }

    @Override
    public void onServiceDisconnected(final ComponentName name) {
        bound = false;
    }
}
