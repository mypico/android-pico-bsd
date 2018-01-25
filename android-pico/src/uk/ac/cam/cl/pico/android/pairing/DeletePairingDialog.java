/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.pairing;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import com.google.common.base.Optional;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.data.SafePairing;

public final class DeletePairingDialog extends DialogFragment {
	
	private static final Logger LOGGER =
			LoggerFactory.getLogger(DeletePairingDialog.class.getSimpleName());
	private static final String PAIRINGS = "PAIRINGS";

	private Optional<DeletePairingListener> listener = Optional.absent();
	
	public static interface DeletePairingListener {
		public void onDeleteOk(final ArrayList<SafePairing> pairings);
		public void onDeleteCancel();
	}	
	
	/**
	 * Public no-args constructor required for all fragments
	 */
	public DeletePairingDialog() {
	}
	
	public static DeletePairingDialog getInstance(final ArrayList<SafePairing> pairings,
			final Fragment targetFragment) {
		final DeletePairingDialog dialog = new DeletePairingDialog();
		
		// Arguments for delete dialog
		final Bundle args = new Bundle();
		args.putParcelableArrayList(PAIRINGS, pairings);
		
		dialog.setArguments(args);		
		
		// Optionally set target fragment for callbacks
		if (targetFragment != null) {
			dialog.setTargetFragment(targetFragment, 0);
		}
		
		return dialog;
	}
	
	public static DeletePairingDialog getInstance(final ArrayList<SafePairing> pairings) {
		return getInstance(pairings, null);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		// Set the listener for this dialog. If the target fragment is set and implements the 
		// listener interface use that, otherwise if the parent activity implement the listener 
		// interface use that, otherwise log a warning.
		final Fragment targetFragment = getTargetFragment();
		if (targetFragment != null && targetFragment instanceof DeletePairingListener) {
			listener = Optional.of((DeletePairingListener) targetFragment);
			LOGGER.debug("using target fragment as listener");
		} else if (activity instanceof DeletePairingListener) {
			listener = Optional.of((DeletePairingListener) activity);
			LOGGER.debug("using activity as listener");
		} else {
			LOGGER.warn("neither target fragment or activity are valid listeners");
		}
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		final ArrayList<SafePairing> pairings = getArguments().getParcelableArrayList(PAIRINGS);
		final int count = pairings.size();
		final int title;
		final String message;
		
		// The title and message of the dialog depends on the number of items being
		// deleted
		if (count == 1) {
			title = R.string.delete_pairing_dialog__title_1;
			message = String.format(
					getActivity().getString(R.string.delete_pairing_dialog__message_1),
					pairings.get(0).getName());
		} else {
			title = R.string.delete_pairing_dialog__title_multi;
			message = String.format(
					getActivity().getString(R.string.delete_pairing_dialog__message_multi), 
					count);
		}
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(title);
		builder.setMessage(message);
				
		// Buttons both handled by the parent activity
		builder.setPositiveButton(
				R.string.delete_pairing_dialog__positive,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (listener.isPresent()) {
							listener.get().onDeleteOk(pairings);
						} else {
							LOGGER.warn("positive button clicked, but no listener set");
						}
					}
				});
		
		builder.setNegativeButton(
				R.string.delete_pairing_dialog__negative,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (listener.isPresent()) {
							listener.get().onDeleteCancel();
						} else {
							LOGGER.warn("negative button clicked, but no listener set");
						}
					}
				});
		
		// Create and return the dialog
		return builder.create();
	}
	
	@Override
	public void onDetach() {
		// Prevent leak of reference to listener
		listener = Optional.absent();
		
		super.onDetach();
	}
}