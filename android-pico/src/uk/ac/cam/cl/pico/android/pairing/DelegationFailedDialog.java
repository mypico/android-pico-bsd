/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.pairing;

import uk.ac.cam.cl.pico.android.data.ParcelableAuthToken;
import uk.ac.cam.cl.pico.crypto.AuthToken;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Parcelable;

public class DelegationFailedDialog extends DialogFragment {

	/**
     * Return a dialog to be displayed when a delegation has failed.
     * 
     * @param context activity context to use when constructing the dialog
     * @param token delegation token
     * @return dialog to display when a delegation has failed
     */
	public interface DelegationFailedSource extends Parcelable {
		Dialog getDelegationFailedDialog(Activity context, AuthToken token);
	}
	
	protected static final String SOURCE =
			AuthFailedDialog.class.getCanonicalName() + "pairing";
	protected static final String TOKEN =
			AuthFailedDialog.class.getCanonicalName() + "token";
	
	/**
	 * Public no-args constructor required for all fragments.
	 */
	public DelegationFailedDialog() {}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {		
		// Get the dialog source to create an appropriate dialog for us:
		DelegationFailedSource source =
				(DelegationFailedSource) getArguments().getParcelable(SOURCE);
		return source.getDelegationFailedDialog(
				getActivity(), (ParcelableAuthToken) getArguments().getParcelable(TOKEN));
	}
}