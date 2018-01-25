/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.core;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class ReattachTask<Params, Progress, Result> 
		extends AsyncTask<Params, Progress, Result> {
	
	public static class ReattachFragment extends Fragment {
		
		// Require public no-args constructor
		public ReattachFragment() {}
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
		}
		
		@Override
		public String toString() {
			return "reattachTaskFragment" + hashCode();
		}
	}
	
	private Optional<ReattachFragment> fragment = Optional.absent();
	
	protected ReattachTask() {
		super();
	}
	
	protected ReattachTask(Activity activity) {
		super();
		attach(activity);
	}
	
	public void attach(Activity activity) {
		checkNotNull(activity, "activity cannot be null");
		
		final ReattachFragment f = new ReattachFragment();
		activity.getFragmentManager().beginTransaction().add(f, f.toString()).commit();
		fragment = Optional.of(f);
	}
	
	protected Activity getActivity() {
		if (fragment.isPresent()) {
			return fragment.get().getActivity();
		} else {
			throw new IllegalStateException("task has not been attached");
		}
	}
}