/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.setup;

import com.example.android.wizardpager.wizard.model.ModelCallbacks;
import com.example.android.wizardpager.wizard.model.Page;
import com.example.android.wizardpager.wizard.model.ReviewItem;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import uk.ac.cam.cl.pico.android.R;

final public class PicoLensPage extends Page {
	
	public static final class PicoLensPageFragment extends CustomPageFragment {	
		
		public static Fragment create(final String key) {
			final PicoLensPageFragment fragment = new PicoLensPageFragment();
		    final Bundle args = new Bundle();
	        args.putString(ARG_KEY, key);
	        fragment.setArguments(args);
	        return fragment;
		}
		
	    @Override
	    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
	            final Bundle savedInstanceState) {

	        final View view = inflater.inflate(R.layout.fragment_pico_lens, container, false);
	        ((TextView) view.findViewById(android.R.id.title)).setText(mPage.getTitle());
	        return view;
	    }
	}
	
    public PicoLensPage(final ModelCallbacks callbacks, final String title) {
        super(callbacks, title);
    }

    @Override
    public Fragment createFragment() {
    	return PicoLensPageFragment.create(getKey());
    }

    @Override
    public void getReviewItems(ArrayList<ReviewItem> dest) {
    }

    @Override
    public boolean isCompleted() {
        return true;
    }
}