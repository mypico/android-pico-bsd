/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.backup;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;

import uk.ac.cam.cl.pico.android.R;

/**
 * Dialog fragment for outputting the secret used to encrypt the backup of the Pico pairings and 
 * service database as a set of words from the PGP word list.
 * 
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
public class PgpWordListOutputDialogFragment extends DialogFragment {

	public static final String TAG = "PgpWordListOutputFragment"; 
	
	protected static final String PGP_WORDS_KEY = "pgpWords";	           
	
    public static PgpWordListOutputDialogFragment newInstance(final String[] pgpWords) {
    	// Verify the method's preconditions
    	checkNotNull(pgpWords);

        final PgpWordListOutputDialogFragment frag = new PgpWordListOutputDialogFragment();
        final Bundle args = new Bundle();
        args.putStringArray(PGP_WORDS_KEY, pgpWords);
        frag.setArguments(args);
        return frag;
    }       
    
    @SuppressLint("InflateParams")
    // "There are of course instances where you can truly justify a null parent during inflation, 
    // but they are few. One such instance occurs when you are inflating a custom layout to be
    // attached to an AlertDialog."
    // http://www.doubleencore.com/2013/05/layout-inflation-as-intended/
	@Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
                
        // Get the layout inflater
       	final LayoutInflater inflater = getActivity().getLayoutInflater();        
        final View view = inflater.inflate(R.layout.fragment_pgp_word_list_output, null);               
        ((TextView) view.findViewById(android.R.id.title)).setText(
        		getString(R.string.fragment_pgp_word_list_output__title));
        
        final GridView myGrid = (GridView) view.findViewById(R.id.pgpwordlistGridView);
        final String[] pgpWords = getArguments().getStringArray(PGP_WORDS_KEY);   
        myGrid.setAdapter(new PgpWordListGridAdapter(Arrays.asList(pgpWords)));

        return new AlertDialog.Builder(getActivity()).setView(view).create();
	}           
    
    /**
     * Prevents DialogFragment being dismissed on screen rotation.
     * see https://code.google.com/p/android/issues/detail?id=17423
     */
    @Override
    public void onDestroyView() {
      if (getDialog() != null && getRetainInstance()) {
        getDialog().setDismissMessage(null);
      }
      super.onDestroyView();
    }
}