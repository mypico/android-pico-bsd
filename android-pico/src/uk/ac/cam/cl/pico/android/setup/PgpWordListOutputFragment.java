/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.setup;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;
import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.example.android.wizardpager.wizard.model.Page;
import com.example.android.wizardpager.wizard.ui.PageFragmentCallbacks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.backup.PgpWordListGridAdapter;

/**
 * Fragment for outputting the secret used to encrypt the backup of the Pico pairings and 
 * service database as a set of words from the PGP word list.
 * 
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
public class PgpWordListOutputFragment extends Fragment {

    private static final String ARG_KEY = "key";
	protected static final String PGP_WORDS_KEY = "pgpWords";	           
	
    private PageFragmentCallbacks mCallbacks;
    private String mKey;
    private Page mPage;
    
    public static PgpWordListOutputFragment newInstance(final String key, final String[] pgpWords) {
    	// Verify the method's preconditions
    	checkNotNull(pgpWords);

        final PgpWordListOutputFragment frag = new PgpWordListOutputFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_KEY, key);
        args.putStringArray(PGP_WORDS_KEY, pgpWords);
        frag.setArguments(args);
        return frag;
    }       
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        mKey = args.getString(ARG_KEY);
        mPage = mCallbacks.onGetPage(mKey);
        mPage.getData().putString(Page.SIMPLE_DATA_KEY,
        		TextUtils.join(" ", args.getStringArray(PGP_WORDS_KEY)));  
    }
    
    @SuppressLint("InflateParams")
    // "There are of course instances where you can truly justify a null parent during inflation, 
    // but they are few. One such instance occurs when you are inflating a custom layout to be
    // attached to an AlertDialog."
    // http://www.doubleencore.com/2013/05/layout-inflation-as-intended/
	@Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {                     
        
        final View view = inflater.inflate(R.layout.fragment_pgp_word_list_output, null);               
        ((TextView) view.findViewById(android.R.id.title)).setText(mPage.getTitle());
        
        final GridView myGrid = (GridView) view.findViewById(R.id.pgpwordlistGridView);
        final String[] pgpWords = getArguments().getStringArray(PGP_WORDS_KEY);   
        myGrid.setAdapter(new PgpWordListGridAdapter(Arrays.asList(pgpWords)));    
        return view;
	}             
    
    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity); 
        
        if (!(activity instanceof PageFragmentCallbacks)) {
            throw new ClassCastException("Activity must implement PageFragmentCallbacks");
        }

        mCallbacks = (PageFragmentCallbacks) activity;
    }
    
    @Override
    public void onDetach() {   
        super.onDetach();        

        // Fragment detached from Activity, set callback empty
        mCallbacks = null;
    }
    
    public boolean isSumbitable() {
    	return true;
    }
}