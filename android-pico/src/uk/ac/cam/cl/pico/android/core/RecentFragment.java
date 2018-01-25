/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.core;

import uk.ac.cam.cl.pico.android.R;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class RecentFragment extends Fragment {

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Fragments have to call this method when they implement onCreateOptionsMenu
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.recent, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
    public View onCreateView(
    		LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the fragment layout
        final View view = inflater.inflate(R.layout.fragment_recent, container, false);
        
        // Dynamically add sub-fragment(s) before returning
        final FragmentTransaction t = getChildFragmentManager().beginTransaction();
        t.add(R.id.fragment_recent__current_frame, new CurrentSessionListFragment()).commit();
        
        return view;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_scan) {
			// TODO
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
