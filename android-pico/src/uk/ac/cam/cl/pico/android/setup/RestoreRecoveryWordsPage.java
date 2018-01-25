/**
 * Copyright Pico project, 2016
 */

/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * A page offering the user a number of mutually exclusive choices.
 */
public class RestoreRecoveryWordsPage extends Page {
	
	public static final class RestoreUserSecretFragment extends CustomPageFragment {	
		
		public static Fragment create(final String key) {
			final RestoreUserSecretFragment fragment = new RestoreUserSecretFragment();
		    final Bundle args = new Bundle();
	        args.putString(ARG_KEY, key);
	        fragment.setArguments(args);
	        return fragment;
		}
		
	    @Override
	    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
	            final Bundle savedInstanceState) {

	        final View view = inflater.inflate(R.layout.fragment_restore_recovery_words, container, false);
	        ((TextView) view.findViewById(android.R.id.title)).setText(mPage.getTitle());
	        return view;
	    }
	}
	
    public RestoreRecoveryWordsPage(final ModelCallbacks callbacks, final String title) {
        super(callbacks, title);
    }

    @Override
    public Fragment createFragment() {
    	return RestoreUserSecretFragment.create(getKey());
    }

    @Override
    public void getReviewItems(ArrayList<ReviewItem> dest) {
    }

    @Override
    public boolean isCompleted() {
        return true;
    }
}