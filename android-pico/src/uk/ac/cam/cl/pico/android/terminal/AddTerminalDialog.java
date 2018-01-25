/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.terminal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.android.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import com.google.common.base.Optional;

public final class AddTerminalDialog extends DialogFragment {
	
	public static interface AddTerminalListener {
		public void onAddConfirm();
		public void onAddCancel();
	}
	
	private static final Logger LOGGER =
			LoggerFactory.getLogger(AddTerminalDialog.class.getSimpleName());
	
	public static final String TERMINAL_NAME = "terminalName";
	
	public static AddTerminalDialog getInstance(final String terminalName,
			final Fragment targetFragment) {
		final AddTerminalDialog dialog = new AddTerminalDialog();
		
		final Bundle args = new Bundle();
		args.putString(TERMINAL_NAME, terminalName);
		dialog.setArguments(args);
		
		// Optionally set target fragment for callbacks
		if (targetFragment != null) {
			dialog.setTargetFragment(targetFragment, 0);
		}
		
		return dialog;
	}
	
	public static AddTerminalDialog getInstance(final String terminalName) {
		return getInstance(terminalName, null);
	}
	
	private Optional<AddTerminalListener> listener = Optional.absent();
	
	/**
	 * Public no-args constructor required for all fragments
	 */
	public AddTerminalDialog() {
	
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		LOGGER.debug("onAttach {}", hashCode());
		
		// Set the listener for this dialog. If the target fragment is set and implements the 
		// listener interface use that, otherwise if the parent activity implement the listener 
		// interface use that, otherwise log a warning.
		final Fragment targetFragment = getTargetFragment();
		if (targetFragment != null && targetFragment instanceof AddTerminalListener) {
			listener = Optional.of((AddTerminalListener) targetFragment);
			LOGGER.debug("using target fragment as listener");
		} else if (activity instanceof AddTerminalListener) {
			listener = Optional.of((AddTerminalListener) activity);
			LOGGER.debug("using acitivty as listener");
		} else {
			LOGGER.warn("neither target fragment or activity are valid listeners");
		}
	}	
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LOGGER.debug("onCreateDialog");
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.dialog_terminal_pairing__title);
		
		// Message of the dialog
		final String name = getArguments().getString(TERMINAL_NAME);
		final String message = getActivity().getString(
				R.string.dialog_terminal_pairing__message, name);
		builder.setMessage(message);
		
		// Buttons both handled by the parent activity
		builder.setPositiveButton(
				R.string.dialog_terminal_pairing__positive,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (listener.isPresent()) {
							listener.get().onAddConfirm();
						} else {
							LOGGER.warn("positive button clicked, but no listener set");
						}
					}
				});
		
		builder.setNegativeButton(
				R.string.dialog_terminal_pairing__negative,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (listener.isPresent()) {
							listener.get().onAddCancel();
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
		// Prevent leaking a reference to a destroyed activity
		listener = Optional.absent();
		
		super.onDetach();
	}
}