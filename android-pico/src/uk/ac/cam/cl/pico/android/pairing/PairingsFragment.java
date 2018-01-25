/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.pairing;

import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AbsListView.MultiChoiceModeListener;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.data.SafePairing;
import uk.ac.cam.cl.pico.android.pairing.DeletePairingDialog.DeletePairingListener;
import uk.ac.cam.cl.pico.android.util.ProgressDialogFragment;

public class PairingsFragment extends ListFragment
	implements DeletePairingListener {
	
	private static final Logger LOGGER =
			LoggerFactory.getLogger(PairingsFragment.class.getSimpleName());
	
	private ArrayAdapter<SafePairing> adapter;
	private Optional<ActionMode> actionMode = Optional.absent();	
    private final IntentFilter intentFilter = new IntentFilter();
    private final ResponseReceiver responseReceiver = new ResponseReceiver();   
	
    {
        intentFilter.addAction(PairingsIntentService.GET_ALL_PAIRINGS_ACTION);
        intentFilter.addAction(PairingsIntentService.DELETE_PAIRINGS_ACTION);
    }       
    
    /**
     * Broadcast receiver for receiving status updates from the KeypairingIntentService and LensPairingIntentService.
     * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
     */
    private class ResponseReceiver extends BroadcastReceiver {
    	
        @Override
        public void onReceive(final Context context, final Intent intent) {
        	if (intent.getAction().equals(PairingsIntentService.GET_ALL_PAIRINGS_ACTION)) {
        		if (!intent.hasExtra(PairingsIntentService.EXCEPTION)) {
        			final ArrayList<SafePairing> keyPairings =
        					intent.getParcelableArrayListExtra(PairingsIntentService.PAIRINGS);
        			getAllPairings(keyPairings);       
        		} else {
        	 		final Bundle extras = intent.getExtras();
            		final Throwable exception =
            				(Throwable) extras.getSerializable(PairingsIntentService.EXCEPTION);
               		LOGGER.error("Exception raise by IntentService {}", exception);
               		getAllPairingsError();
        		}
        	} else 	if (intent.getAction().equals(PairingsIntentService.DELETE_PAIRINGS_ACTION)) {
        		if (!intent.hasExtra(PairingsIntentService.EXCEPTION)) {
        			deletePairings(intent.getIntExtra(PairingsIntentService.PAIRINGS, 0),
	        				intent.getIntExtra(PairingsIntentService.PAIRINGS_DELETED, 0));
        		} else {
        			deletePairingsError();
        		}
        	} else {
        		LOGGER.error("Unrecognised action {}", intent.getAction());
        	}
        }        
    }  

    private void getAllPairings(final List<SafePairing> pairings) {
    	LOGGER.debug("Found pairings {}", pairings);
    	
    	adapter.clear();
		adapter.addAll(pairings);
		adapter.notifyDataSetChanged(); 

		// Hide the progress view
		setLoadingViewVisible(false);
    }
    
    private void getAllPairingsError() {
    	LOGGER.debug("Exception thrown querying pairings");
    }     

    private void deletePairings(final int total, final int deleted) {
    	LOGGER.info("Paired terminal successfuly deleted");
    	
        // Remove the ProgressDialog fragment - if attached
        final DialogFragment progressFragment = (DialogFragment)
        		getActivity().getFragmentManager().findFragmentByTag(
        				ProgressDialogFragment.TAG);      
        if (progressFragment != null) {
        	progressFragment.dismiss();
        }
        
    	// Update the list of terminals paired with Pico
    	updateList();
    	
		// Display a toast, several variations on the wording
		final String message;
		if (deleted == total) {
			message = getResources().getQuantityString(R.plurals.pairings_deleted, deleted, deleted);
		} else if (deleted == 0) {
			message = getResources().getQuantityString(R.plurals.pairings_not_deleted, total, total);
		} else {
			message = getResources().getString(R.string.some_pairings_deleted);
		}		
		Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();		
    }  
    
    private void deletePairingsError() {
    	LOGGER.error("Error deleting pairing");
		
        // Remove the ProgressDialog fragment - if attached
        final DialogFragment progressFragment = (DialogFragment)
        		getActivity().getFragmentManager().findFragmentByTag(
        				ProgressDialogFragment.TAG);      
        if (progressFragment != null) {
        	progressFragment.dismiss();
        }
        
    	final String message = getResources().getString(R.string.failure_deleting_pairing);	
		Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }  
    
	private static ArrayList<SafePairing> getSelected(final ListView list) {
		final ArrayList<SafePairing> selected =
				new ArrayList<SafePairing>(list.getCheckedItemCount());
		final SparseBooleanArray p = list.getCheckedItemPositions();
		
		for (int i = 0; i < p.size(); i++) {
			if (p.valueAt(i)) {		
				final SafePairing t = (SafePairing) list.getAdapter().getItem(
						p.keyAt(i));
				LOGGER.debug("Terminal {} selected at pos {}", t.getName(), i);
				selected.add(t);
			}
		}		
		return selected;
	}	
	
	private void setLoadingViewVisible(boolean visible){
		getView().findViewById(R.id.pairings_list).setVisibility(visible ? View.GONE : View.VISIBLE);
		getView().findViewById(R.id.pairings_list_progress).setVisibility(visible ? View.INVISIBLE : View.GONE);
	}
	
	@Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
    		final Bundle savedInstanceState) {
		adapter = new PairingsAdapter(inflater.getContext(), R.layout.row_pairing);
		setListAdapter(adapter);
        return inflater.inflate(R.layout.fragment_pairings, container, false);
    }
	
	@Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
    	super.onViewCreated(view, savedInstanceState);
    	final ListView listView = getListView();
    	listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    	listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {

			@Override
			public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
				int itemId = item.getItemId();
				if (itemId == R.id.action_remove_terminal) {
					final ArrayList<SafePairing> terminals = getSelected(listView);
					final DeletePairingDialog dialog =
							DeletePairingDialog.getInstance(terminals, PairingsFragment.this);
					// Show the dialog
					dialog.show(getFragmentManager(), DeletePairingDialog.class.getSimpleName());
					// The callbacks of the dialog will deal with deleting the selected items and
					// ending the action mode.					
					return true;
				} else {
					// Not handled here
					return false;
				}
			}

			@Override
			public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
				// Save a reference to the action mode (so that it can be finished async. outside
				// these methods).
				actionMode = Optional.of(mode);
				
				// Inflate the menu for the contextual action bar
				final MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.terminal_list, menu);
				return true;
			}

			@Override
			public void onDestroyActionMode(final ActionMode mode) {
				actionMode = Optional.absent();
			}

			@Override
			public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
				// Not used
				return false;
			}

			@Override
			public void onItemCheckedStateChanged(final ActionMode mode, final int position,
					final long id, final boolean checked) {
				// Not used -- no changes required when checked state changes
			}
    		
    	});
    }
	
    public void onResume() {
        super.onResume();
        
        // Register the BroadcastReceiver that receives responses from
        // the TermianlIntentService (unregistered in the onPause lifecycle method)
        LocalBroadcastManager.getInstance(getActivity())
        	.registerReceiver(responseReceiver, intentFilter);
        
    	// Update the list of pairings with Pico
        updateList();
    }
    
    @Override
    public synchronized void onPause() {
    	super.onPause();
    	
    	// Unregister the BroadcastReceiver  that receives responses from
        // the TermianlIntentService
    	LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(responseReceiver);
    }
    
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		SafePairing pairing = (SafePairing) getListView().getItemAtPosition(position);
		pairing.startDetail(getActivity());
	}
	
	private void updateList() {
		LOGGER.debug("Updating pairings shown on UI");
		
		// Query all the Key and Lens pairings
		final Intent lpIntent = new Intent(getActivity(), PairingsIntentService.class);
		lpIntent.setAction(PairingsIntentService.GET_ALL_PAIRINGS_ACTION);
        getActivity().startService(lpIntent);
	}
	
	/*
	 * Delete pairing dialog
	 */
	
	
	@Override
	public void onDeleteOk(final ArrayList<SafePairing> pairings) {
		if (actionMode.isPresent()) {
			// Show the progress dialog
			final ProgressDialogFragment progressDialog =
					ProgressDialogFragment.newInstance(R.string.delete_pairing__progress);
			progressDialog.show(getActivity().getFragmentManager(), ProgressDialogFragment.TAG);
			
	    	// Delete the Pairings(s)
	        final Intent intent = new Intent(getActivity(), PairingsIntentService.class);  
	        intent.putParcelableArrayListExtra(PairingsIntentService.PAIRINGS, pairings);
	        intent.setAction(PairingsIntentService.DELETE_PAIRINGS_ACTION);
	        getActivity().startService(intent);
			actionMode.get().finish();
		}
	}

	@Override
	public void onDeleteCancel() {
		// Do nothing
	}
}