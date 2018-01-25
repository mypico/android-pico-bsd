/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.terminal;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.data.ParcelableTerminal;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import com.google.common.base.Optional;

public final class DeleteTerminalDialog extends DialogFragment {
	
	private static final Logger LOGGER =
			LoggerFactory.getLogger(DeleteTerminalDialog.class.getSimpleName());
	private static final String TERMINALS = "TERMINALS";

	private Optional<DeleteTerminalListener> listener = Optional.absent();
	
	public static interface DeleteTerminalListener {
		public void onDeleteOk(final ArrayList<ParcelableTerminal> terminals);
		public void onDeleteCancel();
	}	
	
	/**
	 * Public no-args constructor required for all fragments
	 */
	public DeleteTerminalDialog() {
	}
	
	public static DeleteTerminalDialog getInstance(final ArrayList<ParcelableTerminal> terminals,
			final Fragment targetFragment) {
		final DeleteTerminalDialog dialog = new DeleteTerminalDialog();
		
		// Arguments for delete dialog
		final Bundle args = new Bundle();
		args.putParcelableArrayList(TERMINALS, terminals);
		
		dialog.setArguments(args);		
		
		// Optionally set target fragment for callbacks
		if (targetFragment != null) {
			dialog.setTargetFragment(targetFragment, 0);
		}
		
		return dialog;
	}
	
	public static DeleteTerminalDialog getInstance(final ArrayList<ParcelableTerminal> terminals) {
		return getInstance(terminals, null);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		// Set the listener for this dialog. If the target fragment is set and implements the 
		// listener interface use that, otherwise if the parent activity implement the listener 
		// interface use that, otherwise log a warning.
		final Fragment targetFragment = getTargetFragment();
		if (targetFragment != null && targetFragment instanceof DeleteTerminalListener) {
			listener = Optional.of((DeleteTerminalListener) targetFragment);
			LOGGER.debug("using target fragment as listener");
		} else if (activity instanceof DeleteTerminalListener) {
			listener = Optional.of((DeleteTerminalListener) activity);
			LOGGER.debug("using activity as listener");
		} else {
			LOGGER.warn("neither target fragment or activity are valid listeners");
		}
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		final ArrayList<ParcelableTerminal> terminals = getArguments().getParcelableArrayList(TERMINALS);
		final int count = terminals.size();
		final int title;
		final String message;
		
		// The title and message of the dialog depends on the number of items being
		// deleted
		if (count == 1) {
			title = R.string.delete_terminal_dialog__title_1;
			message = String.format(
					getActivity().getString(R.string.delete_terminal_dialog__message_1),
					terminals.get(0).getName());
		} else {
			title = R.string.delete_terminal_dialog__title_multi;
			message = String.format(
					getActivity().getString(R.string.delete_terminal_dialog__message_multi), 
					count);
		}
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(title);
		builder.setMessage(message);
				
		// Buttons both handled by the parent activity
		builder.setPositiveButton(
				R.string.delete_terminal_dialog__positive,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (listener.isPresent()) {
							listener.get().onDeleteOk(terminals);
						} else {
							LOGGER.warn("positive button clicked, but no listener set");
						}
					}
				});
		
		builder.setNegativeButton(
				R.string.delete_terminal_dialog__negative,
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