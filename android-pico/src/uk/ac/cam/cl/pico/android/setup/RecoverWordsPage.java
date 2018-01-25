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

/**
 * TODO
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 */
public class RecoverWordsPage extends Page {
	
	public static final class UserSecretPageFragment extends CustomPageFragment {	
		
		public static Fragment create(final String key) {
			final UserSecretPageFragment fragment = new UserSecretPageFragment();
		    final Bundle args = new Bundle();
	        args.putString(ARG_KEY, key);
	        fragment.setArguments(args);
	        return fragment;
		}
		
	    @Override
	    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
	            final Bundle savedInstanceState) {

	        final View view = inflater.inflate(R.layout.fragment_recovery_words, container, false);
	        ((TextView) view.findViewById(android.R.id.title)).setText(mPage.getTitle());
	        return view;
	    }
	}
	
    public RecoverWordsPage(final ModelCallbacks callbacks, final String title) {
        super(callbacks, title);
    }

    @Override
    public Fragment createFragment() {
    	return UserSecretPageFragment.create(getKey());
    }

    @Override
    public void getReviewItems(final ArrayList<ReviewItem> dest) {
    	// No items to review
    }

    @Override
    public boolean isCompleted() {
        return true;
    }
}