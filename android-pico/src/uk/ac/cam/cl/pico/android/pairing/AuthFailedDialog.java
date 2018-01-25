/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.pairing;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Parcelable;

public class AuthFailedDialog extends DialogFragment {
	
	public interface AuthFailedSource extends Parcelable {
		
	    /**
	     * Return a dialog to be displayed when an authentication has failed.
	     * 
	     * @param context activity context to use when constructing the dialog
	     * @return dialog to display when an authentication has failed
	     */
		Dialog getAuthFailedDialog(Activity context);
	}
	
	protected static final String SOURCE =
			AuthFailedDialog.class.getCanonicalName() + "source";
	
	/**
	 * Public no-args constructor required for all fragments.
	 */
	public AuthFailedDialog() {}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {		
		// Get the dialog source to create an appropriate dialog for us:
		AuthFailedSource source = 
				(AuthFailedSource) getArguments().getParcelable(SOURCE);
		if (source != null) {
			return source.getAuthFailedDialog(getActivity());
		} else {
			return null;
		}
	}
}