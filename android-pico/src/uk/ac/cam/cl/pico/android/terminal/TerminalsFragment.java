/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.terminal;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
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
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.core.AcquireCodeActivity;
import uk.ac.cam.cl.pico.android.data.NonceParcel;
import uk.ac.cam.cl.pico.android.data.ParcelableTerminal;
import uk.ac.cam.cl.pico.android.terminal.AddTerminalDialog.AddTerminalListener;
import uk.ac.cam.cl.pico.android.terminal.DeleteTerminalDialog.DeleteTerminalListener;
import uk.ac.cam.cl.pico.android.util.ProgressDialogFragment;
import uk.ac.cam.cl.pico.crypto.Nonce;

public class TerminalsFragment extends ListFragment 
		implements DeleteTerminalListener, AddTerminalListener {
	
	private static final Logger LOGGER =
			LoggerFactory.getLogger(TerminalsFragment.class.getSimpleName());
	private static final int SCAN_CODE = 0;
	private static final String ADD_TERMINAL_DIALOG = "addTerminalDialog";	
	public static final String TAG = "TerminalsFragment";
	
    private final IntentFilter intentFilter = new IntentFilter();
    private final ResponseReceiver responseReceiver = new ResponseReceiver();
    
	private ArrayAdapter<ParcelableTerminal> adapter;
	private Optional<ActionMode> actionMode = Optional.absent();
	private Uri newTerminalAddress;
	private byte[] newTerminalCommitment;
	private String newTerminalName;
	private Nonce newTerminalNonce;
	
    {
    	// Create an IntentFilter for the actions returned by the TerminalsIntentService
        intentFilter.addAction(TerminalsIntentService.IS_TERMINAL_PRESENT_ACTION);
        intentFilter.addAction(TerminalsIntentService.ADD_TERMINAL_ACTION);
        intentFilter.addAction(TerminalsIntentService.DELETE_TERMINAL_ACTION);
        intentFilter.addAction(TerminalsIntentService.UPDATE_TERMINALS_ACTION);
    }     
        
    /**
     * Broadcast receiver for receiving status updates from the TerminalsIntentService.
     * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
     */
    private class ResponseReceiver extends BroadcastReceiver {
    	
        @Override
        public void onReceive(final Context context, final Intent intent) {
        	if (intent.getAction().equals(TerminalsIntentService.IS_TERMINAL_PRESENT_ACTION)) {
        		if (!intent.hasExtra(TerminalsIntentService.EXCEPTION)) {
	        		if (intent.getBooleanExtra(TerminalsIntentService.IS_TERMINAL_PRESENT_ACTION, false)) {
	        			terminalIsPresent();
	        		} else {
	        			terminalIsNotPresent();
	        		}	        
        		} else {
        	 		final Bundle extras = intent.getExtras();
            		final Throwable exception =
            				(Throwable) extras.getSerializable(TerminalsIntentService.EXCEPTION);
               		LOGGER.error("Exception raise by IntentService {}", exception);
               		addTerminalFailure();
        		}
        	} else if (intent.getAction().equals(TerminalsIntentService.ADD_TERMINAL_ACTION)) {
        		if (!intent.hasExtra(TerminalsIntentService.EXCEPTION)) {
        			terminalAddedSuccessfully();	        	
        		} else {
        	 		final Bundle extras = intent.getExtras();
            		final Throwable exception =
            				(Throwable) extras.getSerializable(TerminalsIntentService.EXCEPTION);
               		LOGGER.error("Exception raise by IntentService {}", exception);
               		addTerminalFailure();
        		}
        	} else if (intent.getAction().equals(TerminalsIntentService.DELETE_TERMINAL_ACTION)) {
        		if (!intent.hasExtra(TerminalsIntentService.EXCEPTION)) {
	        		terminalDeletedSuccessfully(intent.getIntExtra(TerminalsIntentService.TERMINALS, 0),
	        				intent.getIntExtra(TerminalsIntentService.TERMINALS_DELETED, 0));
        		} else {
        	 		final Bundle extras = intent.getExtras();
            		final Throwable exception =
            				(Throwable) extras.getSerializable(TerminalsIntentService.EXCEPTION);
               		LOGGER.error("Exception raise by IntentService {}", exception);
               		terminalDeletedFailure();
        		}
        	} else if (intent.getAction().equals(TerminalsIntentService.UPDATE_TERMINALS_ACTION)) {
        		if (!intent.hasExtra(TerminalsIntentService.EXCEPTION)) {
	        		final List<ParcelableTerminal> terminals =
	        				intent.getParcelableArrayListExtra(TerminalsIntentService.TERMINALS);
	        		updateTerminals(terminals);
        		} else {
            		final Bundle extras = intent.getExtras();
            		final Throwable exception =
            				(Throwable) extras.getSerializable(TerminalsIntentService.EXCEPTION);
               		LOGGER.error("Exception raise by IntentService {}", exception);
        		}
        	} else {
        		LOGGER.error("Unrecognised action {}", intent.getAction());
        	}
        }        
    }  

    void updateTerminals(final List<ParcelableTerminal> terminals) {
    	LOGGER.debug("Updating list of paired terminals");
    	
    	adapter.clear();
		adapter.addAll(terminals);
		adapter.notifyDataSetChanged();
		
		// Hide the progress view
		setLoadingViewVisible(false);		
    }
    
    void terminalAddedSuccessfully() {
    	LOGGER.info("Paired with terminal successfuly");
    	
        // Remove the ProgressDialog fragment - if attached
        final DialogFragment progressFragment = (DialogFragment)
        		getActivity().getFragmentManager().findFragmentByTag(
        				ProgressDialogFragment.TAG);      
        if (progressFragment != null) {
        	progressFragment.dismiss();
        }
        
    	// Update the list of terminals paired with Pico
    	updateList();
    	
    	final String message = getResources().getString(R.string.terminal_added_successfully);	
		Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
		
		final Activity activity = getActivity();	
		if (activity instanceof OnTerminalListener) {
   			((OnTerminalListener) activity).onAddTerminalSuccess(newTerminalName);
   		}
    }
    
	void addTerminalFailure() {
    	LOGGER.error("Error pairing with terminal");
		
        // Remove the ProgressDialog fragment - if attached
        final DialogFragment progressFragment = (DialogFragment)
        		getActivity().getFragmentManager().findFragmentByTag(
        				ProgressDialogFragment.TAG);      
        if (progressFragment != null) {
        	progressFragment.dismiss();
        }
        
    	final String message = getResources().getString(R.string.failure_adding_terminal);	
		Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
        
		final Activity activity = getActivity();		
   		if (activity instanceof OnTerminalListener) {
   			((OnTerminalListener) activity).onAddTerminalFailure();
   		}
	}
	
    void terminalDeletedSuccessfully(final int total, final int deleted) {
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
			message = getResources().getQuantityString(R.plurals.terminals_deleted, deleted, deleted);
		} else if (deleted == 0) {
			message = getResources().getQuantityString(R.plurals.terminals_not_deleted, total, total);
		} else {
			message = getResources().getString(R.string.some_terminals_deleted);
		}		
		Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
		
		final Activity activity = getActivity();	
   		if (activity instanceof OnTerminalListener) {
   			((OnTerminalListener) activity).onRemoveTerminalSuccess();
   		}
    }
    
	void terminalDeletedFailure() {
		LOGGER.error("Error deleting paired terminal");
		
        // Remove the ProgressDialog fragment - if attached
        final DialogFragment progressFragment = (DialogFragment)
        		getActivity().getFragmentManager().findFragmentByTag(
        				ProgressDialogFragment.TAG);      
        if (progressFragment != null) {
        	progressFragment.dismiss();
        }
        
    	final String message = getResources().getString(R.string.failure_deleting_terminal);	
		Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
		
		final Activity activity = getActivity();		
   		if (activity instanceof OnTerminalListener) {
   			((OnTerminalListener) activity).onRemoveTerminalFailure();
   		}
	}
	
	
	void terminalIsPresent() {
		LOGGER.info("{} is already paired with Pico", newTerminalName);
		
		final Activity activity = getActivity();		
   		if (activity instanceof OnTerminalListener) {
   			((OnTerminalListener) activity).onAddTerminalDuplicate(newTerminalName);   			
   		}
	}
	
	void terminalIsNotPresent() {
		LOGGER.debug("{} is not already paired with Pico", newTerminalName);
		
    	// Launch confirmation dialog
    	AddTerminalDialog.getInstance(newTerminalName, this)
    		.show(getFragmentManager(), ADD_TERMINAL_DIALOG);
	}	
	    
	private static ArrayList<ParcelableTerminal> getSelected(final ListView list) {
		final ArrayList<ParcelableTerminal> selected =
				new ArrayList<ParcelableTerminal>(list.getCheckedItemCount());
		final SparseBooleanArray p = list.getCheckedItemPositions();
		
		for (int i = 0; i < p.size(); i++) {
			if (p.valueAt(i)) {		
				final ParcelableTerminal t = (ParcelableTerminal) list.getAdapter().getItem(
						p.keyAt(i));
				LOGGER.debug("Terminal {} selected at pos {}", t.getName(), i);
				selected.add(t);
			}
		}		
		return selected;
	}	
	
	private void setLoadingViewVisible(boolean visible){
		getView().findViewById(R.id.terminals_list).setVisibility(visible ? View.GONE : View.VISIBLE);
		getView().findViewById(R.id.terminals_list_progress).setVisibility(visible ? View.INVISIBLE : View.GONE);
	}
	
	/*
	 * Options menu (action bar) stuff
	 */
	
	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
		// Fragments have to call this method when they implement onCreateOptionsMenu
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		inflater.inflate(R.menu.terminals, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_new_terminal) {
			startAddTerminal();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/*
	 * Main lifecycle methods
	 */

	@Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
    		final Bundle savedInstanceState) {
		adapter = new TerminalsAdapter(inflater.getContext(), R.layout.row_terminal);
		setListAdapter(adapter);
        return inflater.inflate(R.layout.fragment_terminals, container, false);
    }
	
	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		// Request that this fragment instance is retained through config changes of the parent
		// activity
		setRetainInstance(true);
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
					final ArrayList<ParcelableTerminal> terminals = getSelected(listView);
					final DeleteTerminalDialog dialog =
							DeleteTerminalDialog.getInstance(terminals, TerminalsFragment.this);
					// Show the dialog
					dialog.show(getFragmentManager(), DeleteTerminalDialog.class.getSimpleName());
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
        
    	// Update the list of terminals paired with Pico
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
	public void onActivityResult(
			final int requestCode, final int resultCode, final Intent result) {
        if (requestCode == SCAN_CODE) {
            if (resultCode == Activity.RESULT_OK) {            
                
            	// Unpack results
            	newTerminalAddress = (Uri) result.getParcelableExtra(AcquireCodeActivity.TERMINAL_ADDRESS);
            	newTerminalCommitment = result.getByteArrayExtra(AcquireCodeActivity.TERMINAL_COMMITMENT);
            	newTerminalName = result.getStringExtra(AcquireCodeActivity.TERMINAL_NAME);
            	newTerminalNonce = ((NonceParcel) result.getParcelableExtra(AcquireCodeActivity.NONCE)).getNonce();            	       
            	
            	// Is Pico already paired with this Terminal?
                final Intent intent = new Intent(getActivity(), TerminalsIntentService.class);
                intent.putExtra(TerminalsIntentService.COMMITMENT, newTerminalCommitment);
                intent.setAction(TerminalsIntentService.IS_TERMINAL_PRESENT_ACTION);
                getActivity().startService(intent);
            } else {
            	LOGGER.debug("scan cancelled");
            }
        }
    }
    
	
	/*
	 * Terminals list
	 */	
	
	
	private void updateList() {
		// Update the list of Terminals displayed by the UI
        final Intent intent = new Intent(getActivity(), TerminalsIntentService.class);
        intent.setAction(TerminalsIntentService.UPDATE_TERMINALS_ACTION);
        getActivity().startService(intent);        
	}
		
	/*
	 * Adding a terminal
	 */
	
	
	public void startAddTerminal() {
		LOGGER.debug("Starting add terminal process...");		
		
		final Intent intent = new Intent(getActivity(), AcquireCodeActivity.class);
		intent.putExtra(AcquireCodeActivity.TERMINAL_PAIRING_ALLOWED, true);
		startActivityForResult(intent, SCAN_CODE);
	}
	
	public void startAddTerminalSetup() {
		LOGGER.debug("Starting add terminal process...");
		
		final Intent intent = new Intent(getActivity(), AcquireCodeActivity.class);
		intent.putExtra(AcquireCodeActivity.TERMINAL_PAIRING_ALLOWED, true);
		intent.putExtra(AcquireCodeActivity.NO_MENU, true);
		startActivityForResult(intent, SCAN_CODE);
	}	

	@Override
	public void onAddConfirm() {
		// Show the progress dialog
		final ProgressDialogFragment progressDialog =
				ProgressDialogFragment.newInstance(R.string.add_terminal__progress);
		progressDialog.show(getActivity().getFragmentManager(), ProgressDialogFragment.TAG);
		
    	// Add the Terminal
        final Intent intent = new Intent(getActivity(), TerminalsIntentService.class);
        intent.putExtra(TerminalsIntentService.NAME, newTerminalName);
        intent.putExtra(TerminalsIntentService.COMMITMENT, newTerminalCommitment);
        intent.putExtra(TerminalsIntentService.ADDRESS, newTerminalAddress);
        intent.putExtra(TerminalsIntentService.NONCE, new NonceParcel(newTerminalNonce));
        intent.setAction(TerminalsIntentService.ADD_TERMINAL_ACTION);
        getActivity().startService(intent);
	}

	@Override
	public void onAddCancel() {
		LOGGER.debug("Adding terminal cancelled");
	}
		
	/*
	 * Delete terminal dialog
	 */
	
	
	@Override
	public void onDeleteOk(final ArrayList<ParcelableTerminal> terminals) {
		if (actionMode.isPresent()) {
			// Show the progress dialog
			final ProgressDialogFragment progressDialog =
					ProgressDialogFragment.newInstance(R.string.delete_terminal__progress);
			progressDialog.show(getActivity().getFragmentManager(), ProgressDialogFragment.TAG);
			
	    	// Delete the Terminal(s)
	        final Intent intent = new Intent(getActivity(), TerminalsIntentService.class);  
	        intent.putParcelableArrayListExtra(TerminalsIntentService.TERMINALS, terminals);
	        intent.setAction(TerminalsIntentService.DELETE_TERMINAL_ACTION);
	        getActivity().startService(intent);
	        
			actionMode.get().finish();
		}
	}

	@Override
	public void onDeleteCancel() {
		// Do nothing
	}
}