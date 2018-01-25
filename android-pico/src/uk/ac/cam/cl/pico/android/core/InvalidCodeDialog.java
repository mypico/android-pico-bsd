/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.core;

import uk.ac.cam.cl.pico.android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class InvalidCodeDialog extends DialogFragment {
	
	public interface InvalidCodeCallbacks {
		void onScanAnother();
		void onCancel();
	}
	
	private static final String MESSAGE =
			InvalidCodeDialog.class.getCanonicalName() + "message";
	private static final String MESSAGE_RES_ID =
			InvalidCodeDialog.class.getCanonicalName() + "messageResId";
	private static final int DEFAULT_MESSAGE_RES_ID = R.string.default_invalid_code_message;
	
	public static InvalidCodeDialog getInstance(int messageResId) {
		InvalidCodeDialog instance = new InvalidCodeDialog();
		
		Bundle args = new Bundle();
		args.putInt(MESSAGE_RES_ID, messageResId);
		instance.setArguments(args);
		
		return instance;
	}
	
	public static InvalidCodeDialog getInstance(String message) {
		InvalidCodeDialog instance = new InvalidCodeDialog();
		
		Bundle args = new Bundle();
		args.putString(MESSAGE, message);
		instance.setArguments(args);
		
		return instance;
	}
	
	/**
	 * Public no-args constructor required for all fragments.
	 */
	public InvalidCodeDialog() {
		super();
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		final String message = getArguments().getString(MESSAGE);
		if (message != null) {
			builder.setMessage(message);
		} else {
			final int messageResId = getArguments().getInt(MESSAGE_RES_ID, DEFAULT_MESSAGE_RES_ID);
			builder.setMessage(messageResId);
		}
			
		builder.setPositiveButton(
					getActivity().getString(R.string.scan_another),
					new DialogInterface.OnClickListener() {
				
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (getActivity() instanceof InvalidCodeCallbacks) {
								((InvalidCodeCallbacks) getActivity()).onScanAnother();
							}
						}
					})
			.setNegativeButton(
					getActivity().getString(R.string.cancel),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (getActivity() instanceof InvalidCodeCallbacks) {
								((InvalidCodeCallbacks) getActivity()).onCancel();
							}
						}
					});
		
		return builder.create();
	}
}
