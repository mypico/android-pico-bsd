/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.core;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.data.SafeSession;
import uk.ac.cam.cl.pico.data.session.Session;
import uk.ac.cam.cl.pico.data.session.Session.Status;
import android.app.Activity;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;

public class CurrentSessionListFragment extends SessionListFragment {
	
	public interface Listener {
		void onSessionResume(SafeSession session);
		void onSessionPause(SafeSession session);
		void onSessionClose(SafeSession session); // return true if closed successfully
	}
	
	private Listener listener;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		listener = (Listener) activity;
		// throws ClassCastException if activity doesn't implement Listener interface.
	}

	private synchronized void resumeSelected() {
		SparseBooleanArray b = getListView().getCheckedItemPositions();
		for (int i = 0; i < adapter.getCount(); i++) {
			if (b.get(i)) {
				SafeSession session = adapter.getItem(i);
				if (session.getStatus() == Status.PAUSED) {
					// only resume sessions which are currently paused
					listener.onSessionResume(session);
				}
			}
		}
	}

	private synchronized void pauseSelected() {
		SparseBooleanArray b = getListView().getCheckedItemPositions();
		for (int i = 0; i < adapter.getCount(); i++) {
			if (b.get(i)) {
				SafeSession session = adapter.getItem(i);
				if (session.getStatus() == Status.ACTIVE) {
					// on pause session which are currently active
					listener.onSessionPause(session);
				}
			}
		}
	}

	private synchronized void closeSelected() {
		SparseBooleanArray b = getListView().getCheckedItemPositions();
		for (int i = 0; i < adapter.getCount(); i++) {
			SafeSession session = adapter.getItem(i);
			Session.Status status = session.getStatus();
			if (b.get(i)) {
				if (status == Status.ACTIVE || status == Status.PAUSED) {
					// only stop sessions which are currently active or paused 
					// (should be all of them in a current session list)
					listener.onSessionClose(session);
				}
			}
		}
	}
	
	@Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
    	super.onViewCreated(view, savedInstanceState);
    	final ListView listView = getListView();
    	listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    	listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				int itemId = item.getItemId();
				if (itemId == R.id.action_resume) {
					resumeSelected();
					mode.finish();
					return true;
				} else if (itemId == R.id.action_pause) {
					pauseSelected();
					mode.finish();
					return true;
				} else if (itemId == R.id.action_close) {
					closeSelected();
					mode.finish();
					return true;
				} else {
					// Not handled here
					return false;
				}
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				// Inflate the menu for the contextual action bar
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.session_list, menu);
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				// Default changes when context ended
			}

			@Override
			public boolean onPrepareActionMode(ActionMode arg0, Menu arg1) {
				// Not used
				return false;
			}

			@Override
			public void onItemCheckedStateChanged(
					ActionMode mode, int position, long id, boolean checked) {
				// Not used -- no changes required when checked state changes
			}
    		
    	});
    }

	@Override
	public synchronized void update(SafeSession session) {
		// Ensure the list adapter is initialised
		if (adapter == null) {
			throw new IllegalStateException("adapter not initialised");
		}
		
		int index = adapter.getPosition(session);
        if (index >= 0) {
            // already present, need to remove the current adapter/list entry
            adapter.remove(session);
        } else {
        	// not present
            index = 0;
        }
        // add the adapter/list entry
        adapter.insert(session, index);
	}
}
