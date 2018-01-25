/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.setup;

import com.example.android.wizardpager.wizard.model.ModelCallbacks;
import com.example.android.wizardpager.wizard.model.Page;
import com.example.android.wizardpager.wizard.model.ReviewItem;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import uk.ac.cam.cl.pico.android.R;

final public class AddTrustedComputerPage extends Page implements SubmitPage  {
	
	public static final String TRUSTED_COMPUTER_KEY = "TrustedComputer";
		
	public static final class AddTrustedComputerFragment extends CustomPageFragment {	
		
		public static Fragment create(final String key) {
			final AddTrustedComputerFragment fragment = new AddTrustedComputerFragment();
		    final Bundle args = new Bundle();
	        args.putString(ARG_KEY, key);
	        fragment.setArguments(args);
	        return fragment;
		}
		
	    @Override
	    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
	            final Bundle savedInstanceState) {

	        final View view = inflater.inflate(R.layout.fragment_add_trusted_computer, container, false);
	        ((TextView) view.findViewById(android.R.id.title)).setText(mPage.getTitle());
	        return view;
	    }
	}
	
    public AddTrustedComputerPage(final ModelCallbacks callbacks, final String title) {
        super(callbacks, title);
    }

    public Page setValue(final String key, final String value) {
        mData.putString(key, value);
        return this;
    }
    
    @Override
    public Fragment createFragment() {
    	return AddTrustedComputerFragment.create(getKey());
    }

    @Override
    public void getReviewItems(ArrayList<ReviewItem> dest) {
        dest.add(new ReviewItem(getTitle(), mData.getString(TRUSTED_COMPUTER_KEY), getKey()));
    }

    @Override
    public boolean isCompleted() {
        return !TextUtils.isEmpty(mData.getString(TRUSTED_COMPUTER_KEY));
    }
    
    @Override
    public boolean isReadyToSubmit() {
        return true;
    }
    
}