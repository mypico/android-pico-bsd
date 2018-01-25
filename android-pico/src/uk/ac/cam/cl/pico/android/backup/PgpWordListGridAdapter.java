/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.backup;

import java.util.List;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

/**
 * GridAdaptor used to display items from the PGP word list in a custom dialog.
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
public final class PgpWordListGridAdapter extends BaseAdapter {

    private final List<String> items;

    private final int color1;
    private final int color2;
    
    /**
     * Default constructor
     * @param items to fill data to
     */
    public PgpWordListGridAdapter(final List<String> items) {
    	this(items, Color.LTGRAY, Color.DKGRAY);
    }

    /**
     * Constructor
     * @param items to fill data to
     * @param color1 first color
     * @param color2 aleternate color
     */
    PgpWordListGridAdapter(final List<String> items, final int color1, final int color2) {
        this.items = items;
        this.color1 = color1;
        this.color2 = color2;
    }
    
    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(final int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public View getView(final int position, final View convertView,
            final ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(
                    android.R.layout.simple_list_item_1, parent, false);
        }

        final TextView text = (TextView) view.findViewById(android.R.id.text1);
        // Apply alternate color1, color2 rows
        final GridView grid = (GridView) parent;
        if ((position/grid.getNumColumns()) % 2 == 0) {
        	text.setBackgroundColor(color1);
        	text.setTextColor(color2);
        } else {
        	text.setBackgroundColor(color2);        	
        	text.setTextColor(color1);        	
        }
        text.setText(items.get(position));

        return view;
    }
}    