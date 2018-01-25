/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.util;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;

/**
 * Displays progress of addition and removal of terminals.
 * The ProgressDialog is displayed within a Fragment allowing configurations changes to
 * be handled easily.
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
public final class ProgressDialogFragment extends DialogFragment {
	 
	public static final String TAG = "ProgressDialogTag";
	private static final String PROGRESS_FRAGMENT_MESSAGE = "message";    	    	
	
	/**
	 * Static factory method for constructing the ProgressDialog.
	 * @return
	 */
	public static ProgressDialogFragment newInstance(final int message) {
		final ProgressDialogFragment frag = new ProgressDialogFragment();
		
		// Supply index input as an argument.
        final Bundle args = new Bundle();
        args.putInt(PROGRESS_FRAGMENT_MESSAGE, message);
        frag.setArguments(args);
		return frag;
	}
	 	
	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final ProgressDialog dialog = new ProgressDialog(getActivity());
		dialog.setMessage(getString(getArguments().getInt(PROGRESS_FRAGMENT_MESSAGE)));
		dialog.setIndeterminate(true);
		dialog.setCancelable(false);    
		dialog.setCanceledOnTouchOutside(false);
		return dialog;
	}  
} 