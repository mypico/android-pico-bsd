/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.core;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.data.SafeSession;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

final class SessionArrayAdapter extends ArrayAdapter<SafeSession> {

    @SuppressLint("SimpleDateFormat")
    private static final DateFormat lastAuthFormat =
            new SimpleDateFormat("d MMM yyyy, HH:mm:ss");

    public SessionArrayAdapter(Context context, int resource) {
        super(context, resource);
    }

    public SessionArrayAdapter(Context context, int resource, List<SafeSession> sessions) {
        super(context, resource, sessions);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Inflate the view and get references to the inner views
        LayoutInflater inflater =
                (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.list_session_info, parent, false);
        TextView pairingNameView = (TextView) view.findViewById(
                R.id.list_session_info__pairing_name);
        TextView lastAuthView = (TextView) view.findViewById(
                R.id.list_session_info__last_auth);
        ImageView statusIconView = (ImageView) view.findViewById(
                R.id.list_session_info__status_icon);

        // Get the session info for this list item
        SafeSession sessionInfo = getItem(position);

        // Insert relevant values into the view
        pairingNameView.setText(sessionInfo.getSafePairing().getDisplayName());
        lastAuthView.setText(lastAuthFormat.format(sessionInfo.getLastAuthDate()));
        switch (sessionInfo.getStatus()) {
            case ACTIVE:
                statusIconView.setImageResource(android.R.drawable.presence_online);
                break;
            case PAUSED:
                statusIconView.setImageResource(android.R.drawable.presence_away);
                break;
            case ERROR:
                statusIconView.setImageResource(android.R.drawable.presence_busy);
                break;
            case CLOSED:
            default:
                statusIconView.setImageResource(android.R.drawable.presence_invisible);
                break;
        }

        return view;
    }
}
