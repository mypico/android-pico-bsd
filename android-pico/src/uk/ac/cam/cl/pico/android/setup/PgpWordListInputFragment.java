/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.setup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;

import com.example.android.wizardpager.wizard.model.Page;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.backup.PgpWordListGridAdapter;
import uk.ac.cam.cl.pico.android.util.InvalidWordException;
import uk.ac.cam.cl.pico.android.util.PgpWordListByteString;
import uk.ac.cam.cl.pico.android.util.SpaceTokenizer;
import uk.ac.cam.cl.pico.backup.BackupKey;

/**
 * Fragment for inputting the secret used to encrypt the backup of the Pico pairings and 
 * service database as a set of words from the PGP word list.
 * 
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
public class PgpWordListInputFragment extends CustomPageFragment {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(PgpWordListInputFragment.class.getSimpleName());
    
    private static final String ARG_KEY = "key";          
    private static final int NUM_PGP_WORDS = 12;

    private ArrayList<String> pgpWords = new ArrayList<String>(); 
    
    public static PgpWordListInputFragment newInstance(final String key) {
    	// Verify the method's preconditions
    	checkNotNull(key);

        final PgpWordListInputFragment frag = new PgpWordListInputFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_KEY, key);
        frag.setArguments(args);
        return frag;
    }       
    
    @SuppressLint("InflateParams")
    // "There are of course instances where you can truly justify a null parent during inflation, 
    // but they are few. One such instance occurs when you are inflating a custom layout to be
    // attached to an AlertDialog."
    // http://www.doubleencore.com/2013/05/layout-inflation-as-intended/
	@Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {                     
     
        final View view = inflater.inflate(R.layout.fragment_pgp_word_list_input, null);       
        ((TextView) view.findViewById(android.R.id.title)).setText(mPage.getTitle());
        
        // Add autocomplete for PGP word list
        final MultiAutoCompleteTextView autoCompleteTextView =
        		(MultiAutoCompleteTextView) view.findViewById(
        		R.id.fragment_pgp_word_list_typing__key_field);
        final String[] wordList = getResources().getStringArray(R.array.pgp_word_list);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
        		view.getContext(), android.R.layout.simple_list_item_1, wordList);
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setThreshold(1);
        // Will want to create a space tokenizer
        autoCompleteTextView.setTokenizer(new SpaceTokenizer());
  
        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            
            @Override
            public void afterTextChanged(final Editable s) {
		        // Display the PGP words in a GridView,
		        // currently unfilled in words are number from 1 to NUM_PGP_WORDS
		        final ArrayList<String> gridWords = new ArrayList<String>();
            	pgpWords.clear();     
            	if (s.length() != 0) {            	   	
            		pgpWords.addAll(Arrays.asList(s.toString().trim().split("\\s")));
    		        gridWords.addAll(pgpWords);
            	}
		        for (int i = gridWords.size()+1; i <= NUM_PGP_WORDS; i++) {
		        	gridWords.add(Integer.toString(i));
		        }
		        final GridView myGrid = (GridView) view.findViewById(R.id.pgpwordlistGridView);
		        final PgpWordListGridAdapter gridAdapter = new PgpWordListGridAdapter(gridWords);     
		        myGrid.setAdapter(gridAdapter);
		        
            	try {
        			final byte[] userSecret = new PgpWordListByteString(getActivity())
        				.fromWords(TextUtils.join(" ", pgpWords));
        			if (BackupKey.isValid(userSecret)) {
						// Dismiss the IME once a valid set of 12 PGP words has been entered
						LOGGER.debug("Valid user secret - dismissing the IME");
 
						final InputMethodManager imm =
								(InputMethodManager) getActivity().getSystemService(
										Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(autoCompleteTextView.getWindowToken(), 0);
							
			 	        mPage.getData().putString(Page.SIMPLE_DATA_KEY, TextUtils.join(" ", pgpWords));
	        			mPage.notifyDataChanged();
        			} else {
        	 			 LOGGER.debug("User secret is not valid");
        			}
        		} catch (InvalidWordException e) {
        			 LOGGER.debug("User secret is not valid", e);
        		}
            }
        });
        autoCompleteTextView.setImeOptions(EditorInfo.IME_ACTION_DONE);
          
        return view;
	}                 
    
    @Override
    public void onResume() {
    	super.onResume();
    	
        if (!pgpWords.isEmpty()) {           
            final MultiAutoCompleteTextView autoCompleteTextView =
            		(MultiAutoCompleteTextView) getActivity().findViewById(
            		R.id.fragment_pgp_word_list_typing__key_field);
            
            // Fill in the MultiAutoCompleteTextView with the contents of pgpWords
        	autoCompleteTextView.setText(TextUtils.join(" ", pgpWords) + " ");
        	autoCompleteTextView.setSelection(autoCompleteTextView.length());
        } else {
  			// Fill in the GridView with the number 1 to 12 to indicate the order in which to enter
  			// the PGP words
        	final List<String> gridWords = new ArrayList<String>(Arrays.asList(
        			"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"));
	        final GridView myGrid = (GridView) getActivity().findViewById(R.id.pgpwordlistGridView);
	        final PgpWordListGridAdapter gridAdapter = new PgpWordListGridAdapter(gridWords);     
	        myGrid.setAdapter(gridAdapter);
        }
    }        
}