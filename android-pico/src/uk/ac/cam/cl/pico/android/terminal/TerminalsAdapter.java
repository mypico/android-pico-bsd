/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.terminal;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.data.ParcelableTerminal;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TerminalsAdapter extends ArrayAdapter<ParcelableTerminal> {
	
	private class ViewHolder {
		TextView name;
	}

	public TerminalsAdapter(Context context, int resource) {
		super(context, resource);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// Get the view holder
		final ViewHolder holder;
		if (convertView == null) {
			// Inflate the layout
			convertView = LayoutInflater.from(
					getContext()).inflate(R.layout.row_terminal, parent, false);
			
			// Store the views in a holder
			holder = new ViewHolder();
			holder.name = (TextView) convertView.findViewById(R.id.row_terminal__name);
			
			// Store the holder with the view
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		// Get the Terminal for this list item position
		final ParcelableTerminal terminal = getItem(position);
		
		// Fill in row details
		holder.name.setText(terminal.getName());
		
		return convertView;
	}
}