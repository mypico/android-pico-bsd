/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.data.SafeSession;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

abstract class SessionListFragment extends ListFragment {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SessionListFragment.class.getSimpleName());

    private class SessionUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Get session info from intent and check not null
            SafeSession session = (SafeSession) intent.getParcelableExtra(
            		SafeSession.class.getCanonicalName());
            if (session != null) {
                LOGGER.debug("session info retrieved from broadcast update intent");
                // Call on the subclass to cope with this new update
                update(session);
            } else {
                LOGGER.warn("no session info in broadcast update intent");
            }
        }
    }

    protected SessionArrayAdapter adapter;
    private LocalBroadcastManager broadcastManager;
    private BroadcastReceiver broadcastReceiver;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        broadcastManager = LocalBroadcastManager.getInstance(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        broadcastReceiver = new SessionUpdateReceiver();
    }

    @Override
    public View onCreateView(
    		LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        adapter = new SessionArrayAdapter(inflater.getContext(), R.layout.list_session_info);
        setListAdapter(adapter);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
    	super.onViewCreated(view, savedInstanceState);
    	setEmptyText(getText(R.string.no_session_history));
    }

    @Override
    public void onResume() {
        super.onResume();
        broadcastManager.registerReceiver(
        		broadcastReceiver, new IntentFilter(PicoServiceImpl.SESSION_INFO_UPDATE));
        LOGGER.debug("Registered broadcast receiver");
    }

    @Override
    public void onPause() {
        super.onPause();
        broadcastManager.unregisterReceiver(broadcastReceiver);
        LOGGER.debug("Unregistered broadcast receiver");
    }

    public abstract void update(SafeSession session);
}
