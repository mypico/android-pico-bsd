/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.setup;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.android.wizardpager.wizard.model.AbstractWizardModel;
import com.example.android.wizardpager.wizard.model.ModelCallbacks;
import com.example.android.wizardpager.wizard.model.Page;
import com.example.android.wizardpager.wizard.ui.PageFragmentCallbacks;
import com.example.android.wizardpager.wizard.ui.ReviewFragment;
import com.example.android.wizardpager.wizard.ui.StepPagerStrip;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.android.R;
import static uk.ac.cam.cl.pico.android.R.array.activity_setup__backup_from__choices;
import static uk.ac.cam.cl.pico.android.R.array.activity_setup__backup_to__choices;
import static uk.ac.cam.cl.pico.android.R.array.activity_setup__restore_backup__choices;
import uk.ac.cam.cl.pico.android.backup.BackupFactory;
import uk.ac.cam.cl.pico.android.backup.BackupProviderFragment;
import uk.ac.cam.cl.pico.android.backup.BackupProviderFragment.RestoreOption;
import uk.ac.cam.cl.pico.android.backup.IBackupProvider;
import uk.ac.cam.cl.pico.android.backup.OnConfigureBackupListener;
import uk.ac.cam.cl.pico.android.backup.OnQueryBackupListener;
import uk.ac.cam.cl.pico.android.backup.OnRestoreBackupListener;
import uk.ac.cam.cl.pico.android.backup.IBackupProvider.BackupType;
import uk.ac.cam.cl.pico.android.core.AcquireCodeActivity;
import uk.ac.cam.cl.pico.android.data.NonceParcel;
import uk.ac.cam.cl.pico.android.terminal.AddTerminalDialog.AddTerminalListener;
import uk.ac.cam.cl.pico.android.terminal.AddTerminalDialog;
import uk.ac.cam.cl.pico.android.terminal.TerminalsIntentService;
import uk.ac.cam.cl.pico.android.util.InvalidWordException;
import uk.ac.cam.cl.pico.android.util.PgpWordListByteString;

/**
 * SetupActivity provides Wizards for setting up Pico and restoring a backup.
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
 public class SetupActivity extends FragmentActivity implements
        PageFragmentCallbacks, ReviewFragment.Callbacks, ModelCallbacks,
        OnConfigureBackupListener, OnRestoreBackupListener,  OnQueryBackupListener,
        AddTerminalListener {
	
    private static final Logger LOGGER = LoggerFactory
            .getLogger(SetupActivity.class.getSimpleName());
    private static final String ACTIVITY_SETUP_FRAGMENT_TAG =
    		"ActivitySetupDialogFragment";
    private static final String SETUP_ACTIVITY_MODEL = "SetupModel";

    public final static int SETUP_RESULT_CODE = 0x01;
    public final static int RESTORE_BACKUP_RESULT_CODE = 0x02;
    
    protected AbstractWizardModel mWizardModel;
    
    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    private boolean mEditingAfterReview;
    private boolean mConsumePageSelectedEvent;
    private Button mNextButton;
    private Button mPrevButton;
    private List<Page> mCurrentPageSequence;
    private StepPagerStrip mStepPagerStrip;
    private IBackupProvider backupProvider;
   
	private static final String ADD_TERMINAL_DIALOG = "ADD_TERMINAL_DIALOG";
	private static final String TERMINAL_NAME = "TERMINAL_NAME";
	private static final String TERMINAL_COMMITMENT = "TERMINAL_COMMITMENT";	
	private static final String TERMINAL_ADDRESS = "TERMINAL_ADDRESS";	
	private static final String NONCE = "NONCE";	
	private static final int SCAN_CODE = 0;

	private Uri terminalAddress;
	private String terminalName;
	private byte[] commitment;
	private NonceParcel nonce;
	
    private final IntentFilter intentFilter = new IntentFilter();
    private final ResponseReceiver responseReceiver = new ResponseReceiver();
    
    {
    	// Initialise the SetupActivity's WizardModel
    	mWizardModel = new SetupWizardModel(this);

    	// Create an IntentFilter for the actions returned by the TerminalsIntentService
        intentFilter.addAction(TerminalsIntentService.IS_TERMINAL_PRESENT_ACTION);
        intentFilter.addAction(TerminalsIntentService.ADD_TERMINAL_ACTION);
    }
    
    private void nextPage() {
        mPager.setCurrentItem(mPager.getCurrentItem() + 1);
    }
    
    private void previousPage() {
        mPager.setCurrentItem(mPager.getCurrentItem() - 1);
    }
    
    private void reviewPage() {
        mPager.setCurrentItem(mPagerAdapter.getCount() - 1);
    }        
        
    /**
     * Displays progress of addition and removal of terminals.
     * The ProgressDialog is displayed within a Fragment allowing configurations changes to
     * be handled easily.
     * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
     *
     */
    public static final class ProgressDialogFragment extends DialogFragment {
    	 
    	private static final String TAG = "ProgressDialogTag";
    	private static final String PROGRESS_FRAGMENT_MESSAGE = "message";    	    	
    	
    	/**
    	 * Static factory method for constructing the ProgressDialog.
    	 * @return
    	 */
    	public static ProgressDialogFragment newInstance(final int message) {
    		final ProgressDialogFragment frag = new ProgressDialogFragment();
    		
    		// Supply index input as an argument.
            final Bundle args = new Bundle();
            args.putInt(PROGRESS_FRAGMENT_MESSAGE, message);
            frag.setArguments(args);
    		return frag;
    	}
    	 	
    	@Override
    	public Dialog onCreateDialog(final Bundle savedInstanceState) {
    		final ProgressDialog dialog = new ProgressDialog(getActivity());
    		dialog.setMessage(getString(getArguments().getInt(PROGRESS_FRAGMENT_MESSAGE)));
    		dialog.setIndeterminate(true);
    		dialog.setCancelable(false);    
    		dialog.setCanceledOnTouchOutside(false);
    		return dialog;
    	}  
    } 
    
    /**
     * Broadcast receiver for receiving status updates from the TerminalsIntentService.
     * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
     */
    private class ResponseReceiver extends BroadcastReceiver {
    	
        @Override
        public void onReceive(final Context context, final Intent intent) {
        	if (intent.getAction().equals(TerminalsIntentService.IS_TERMINAL_PRESENT_ACTION)) {
        		if (!intent.hasExtra(TerminalsIntentService.EXCEPTION)) {
	        		if (intent.getBooleanExtra(TerminalsIntentService.IS_TERMINAL_PRESENT_ACTION, false)) {
	        			terminalIsPresent();
	        		} else {
	        			terminalIsNotPresent();
	        		}	        
        		} else {
        	 		final Bundle extras = intent.getExtras();
            		final Throwable exception =
            				(Throwable) extras.getSerializable(TerminalsIntentService.EXCEPTION);
               		LOGGER.error("Exception raise by IntentService {}", exception);
               		addTerminalFailure();
        		}
        	} else if (intent.getAction().equals(TerminalsIntentService.ADD_TERMINAL_ACTION)) {
        		if (!intent.hasExtra(TerminalsIntentService.EXCEPTION)) {
        			terminalAddedSuccessfully();	        	
        		} else {
        	 		final Bundle extras = intent.getExtras();
            		final Throwable exception =
            				(Throwable) extras.getSerializable(TerminalsIntentService.EXCEPTION);
               		LOGGER.error("Exception raise by IntentService {}", exception);
               		addTerminalFailure();
        		}
        	} else {
        		LOGGER.error("Unrecognised action {}", intent.getAction());
        	}
        }        
    }     
    
    void terminalAddedSuccessfully() {
    	LOGGER.info("Paired with terminal successfuly");
    	
        // Remove the ProgressDialog fragment - if attached
        final DialogFragment progressFragment = (DialogFragment)
        		getSupportFragmentManager().findFragmentByTag(ProgressDialogFragment.TAG);      
        if (progressFragment != null) {
        	progressFragment.dismiss();
        }
        
    	final String message = getResources().getString(R.string.terminal_added_successfully);	
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
		
	   	final Page page = mCurrentPageSequence.get(mPager.getCurrentItem());
    	if (page instanceof AddTrustedComputerPage) {
    		final AddTrustedComputerPage atcPage = (AddTrustedComputerPage) page;
    		atcPage.setValue(AddTrustedComputerPage.TRUSTED_COMPUTER_KEY, terminalName);
    		atcPage.notifyDataChanged();
    	}
    	
		// Advance the Wizard to the next page
		nextPage();	
    }
    
	void addTerminalFailure() {
    	LOGGER.error("Error pairing with terminal");
		
        // Remove the ProgressDialog fragment - if attached
        final DialogFragment progressFragment = (DialogFragment)
        		getSupportFragmentManager().findFragmentByTag(ProgressDialogFragment.TAG);      
        if (progressFragment != null) {
        	progressFragment.dismiss();
        }
        
    	final String message = getResources().getString(R.string.failure_adding_terminal);	
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();      
	}	
		
	void terminalIsPresent() {
		LOGGER.info("{} is already paired with Pico", terminalName);
		terminalAddedSuccessfully();
	}
	
	void terminalIsNotPresent() {
		LOGGER.debug("{} is not already paired with Pico", terminalName);
		
    	// Launch confirmation dialog
    	AddTerminalDialog.getInstance(terminalName)
    		.show(getSupportFragmentManager(), ADD_TERMINAL_DIALOG);
	}	
	    
	@Override
	public void onAddConfirm() {
		// Show the progress dialog
		final ProgressDialogFragment progressDialog =
				ProgressDialogFragment.newInstance(R.string.add_terminal__progress);
		progressDialog.show(getSupportFragmentManager(), ProgressDialogFragment.TAG);
		
    	// Add the Terminal
        final Intent intent = new Intent(this, TerminalsIntentService.class);
        intent.putExtra(TerminalsIntentService.NAME, terminalName);
        intent.putExtra(TerminalsIntentService.COMMITMENT, commitment);
        intent.putExtra(TerminalsIntentService.ADDRESS, terminalAddress);
        intent.putExtra(TerminalsIntentService.NONCE, nonce);
        intent.setAction(TerminalsIntentService.ADD_TERMINAL_ACTION);
        startService(intent);
	}

	@Override
	public void onAddCancel() {
		LOGGER.debug("Adding terminal cancelled");
	}
	
    @Override
	public void onActivityResult(
			final int requestCode, final int resultCode, final Intent result) {
        if (requestCode == SCAN_CODE) {
            if (resultCode == Activity.RESULT_OK) {            
                
            	// Unpack results
              	terminalName = result.getStringExtra(AcquireCodeActivity.TERMINAL_NAME);
            	commitment = result.getByteArrayExtra(AcquireCodeActivity.TERMINAL_COMMITMENT);
            	terminalAddress = (Uri) result.getParcelableExtra(AcquireCodeActivity.TERMINAL_ADDRESS);
            	nonce = ((NonceParcel) result.getParcelableExtra(AcquireCodeActivity.NONCE));            	       
            	            	            
            	// Is Pico already paired with this Terminal?
                final Intent intent = new Intent(this, TerminalsIntentService.class);
                intent.putExtra(TerminalsIntentService.COMMITMENT, commitment);
                intent.setAction(TerminalsIntentService.IS_TERMINAL_PRESENT_ACTION);
                startService(intent);
            } else {
            	LOGGER.debug("scan cancelled");
            }
        }
    }   
    
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        if (savedInstanceState != null) {
            // Restore value of members from saved state
            terminalName = savedInstanceState.getString(TERMINAL_NAME);
            commitment = savedInstanceState.getByteArray(TERMINAL_COMMITMENT);
            terminalAddress = (Uri) savedInstanceState.getParcelable(TERMINAL_ADDRESS);
            nonce = (NonceParcel) savedInstanceState.getParcelable(NONCE);
        }
        
        setContentView(R.layout.activity_setup);
               
        LOGGER.debug("onCreate setupActivity");                       	     
        
        // Register the BraodcastReceiver, this must be unregistered in the onDestroy lifecycle
        // method
        LocalBroadcastManager.getInstance(this).registerReceiver(responseReceiver, intentFilter);
        
        
 	    final android.app.Fragment fragment = getFragmentManager()
          		.findFragmentByTag(BackupProviderFragment.TAG); 	     
 	    if (fragment != null) {
	        LOGGER.debug("BackupProvider fragment attached");
	        backupProvider = (IBackupProvider) fragment;
	       	
	        setRestoreOptions(mWizardModel.findByKey(
	        		getString(R.string.activity_setup__new_user__returning_user_restore) + ":" +
	      					getString(R.string.activity_setup__backup_from__title)));
	        setRestoreOptions(mWizardModel.findByKey(
	        		getString(R.string.activity_setup__backup_from__title)));
	    }  
 	    
        if (savedInstanceState != null) {
            mWizardModel.load(savedInstanceState.getBundle(SETUP_ACTIVITY_MODEL));
        }
        mWizardModel.registerListener(this);

        mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mStepPagerStrip = (StepPagerStrip) findViewById(R.id.strip);
        mStepPagerStrip.setOnPageSelectedListener(new StepPagerStrip.OnPageSelectedListener() {
        	
            @Override
            public void onPageStripSelected(int position) {
                position = Math.min(mPagerAdapter.getCount() - 1, position);
                if (mPager.getCurrentItem() != position) {
                    mPager.setCurrentItem(position);
                }
            }
        });

        mNextButton = (Button) findViewById(R.id.next_button);
        mPrevButton = (Button) findViewById(R.id.prev_button);

        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
        	
            @Override
            public void onPageSelected(final int position) {
                mStepPagerStrip.setCurrentPage(position);

                if (mConsumePageSelectedEvent) {
                    mConsumePageSelectedEvent = false;
                    return;
                }

                mEditingAfterReview = false;
                updateBottomBar();
            }
        });
        
        mNextButton.setOnClickListener(new View.OnClickListener() {
        	
            @Override
            public void onClick(final View view) {

            	if (mPager.getCurrentItem() == mCurrentPageSequence.size()) {
                    // Setup was successfully completed, return the OK to the calling Activity
                    setResult(RESULT_OK);
                    finish();
            	} else {
	            	final Page page = mCurrentPageSequence.get(mPager.getCurrentItem());
	               	if (page instanceof AddTrustedComputerPage) {   
	               		if (!page.isCompleted()) {
	               			LOGGER.debug("Starting add terminal process...");
	               			
	               			final Intent intent = new Intent(SetupActivity.this, AcquireCodeActivity.class);
	               			intent.putExtra(AcquireCodeActivity.TERMINAL_PAIRING_ALLOWED, true);
	               			intent.putExtra(AcquireCodeActivity.NO_MENU, true);
	               			startActivityForResult(intent, SCAN_CODE);
	               		} else {
	               			if (mEditingAfterReview) {
	                         	// Return to the review page
	        	            	reviewPage();
	               			} else {
	                        	// Advance to the next page
		                    	nextPage();
	               			}	               			
	               		}
	               		
	               	} else 	if (page instanceof SelectBackupProviderPage) {
	              		// Configure the user's selected backup provider            		
	            		LOGGER.debug("Configuring the user's backup provider");
	            		
	            		if (page.getData().getString(Page.SIMPLE_DATA_KEY).equals(
	            				getResources().getStringArray(activity_setup__backup_to__choices)[0]) ||
	            			page.getData().getString(Page.SIMPLE_DATA_KEY).equals(
	            				getResources().getStringArray(activity_setup__backup_from__choices)[0])) {
	            			if (BackupFactory.restoreBackupType() == BackupType.DROPBOX && mEditingAfterReview) {
	                        	// Return to the review page
	            				reviewPage();
	            			} else {
	                			LOGGER.debug("Configuring the user's Dropbox account");
	            				BackupFactory.newBackup(BackupType.DROPBOX, SetupActivity.this);
	            			}
	            		} else if (page.getData().getString(Page.SIMPLE_DATA_KEY).equals(
	            						getResources().getStringArray(activity_setup__backup_to__choices)[1]) ||
	            					page.getData().getString(Page.SIMPLE_DATA_KEY).equals(
	    	            				getResources().getStringArray(activity_setup__backup_from__choices)[1])) {
	            			LOGGER.debug("Configuring the user's Google Drive account");
	            			if (BackupFactory.restoreBackupType() == BackupType.GOOGLEDRIVE && mEditingAfterReview) {
	                        	// Return to the review page
	            				reviewPage();
	            			} else {
	                			LOGGER.debug("Configuring the user's Google Drive account");
		        				BackupFactory.newBackup(BackupType.GOOGLEDRIVE, SetupActivity.this);
	            			}
		        		} else if (page.getData().getString(Page.SIMPLE_DATA_KEY).equals(
		        						getResources().getStringArray(activity_setup__backup_to__choices)[2]) ||
		        					page.getData().getString(Page.SIMPLE_DATA_KEY).equals(
				        				getResources().getStringArray(activity_setup__backup_from__choices)[2])) {
	            			if (BackupFactory.restoreBackupType() == BackupType.ONEDRIVE && mEditingAfterReview) {
	                        	// Return to the review page
	            				reviewPage();
	            			} else {
		            			LOGGER.debug("Configuring the user's Microsoft OneDrive account");
			    				BackupFactory.newBackup(BackupType.ONEDRIVE, SetupActivity.this);
			    			}
			    		} else if (	page.getData().getString(Page.SIMPLE_DATA_KEY).equals(
				        				getResources().getStringArray(activity_setup__backup_to__choices)[3]) ||
				        			page.getData().getString(Page.SIMPLE_DATA_KEY).equals(
						        		getResources().getStringArray(activity_setup__backup_from__choices)[3])) {
	            			if (BackupFactory.restoreBackupType() == BackupType.SDCARD && mEditingAfterReview) {
	                        	// Return to the review page
	            				reviewPage();
	            			} else {
		            			LOGGER.debug("Configuring the user's SD Card");
			    				BackupFactory.newBackup(BackupType.SDCARD, SetupActivity.this);
			    			}			    			
			    		} else {
			    			LOGGER.error("Backup provider {} not recognized", page.getData().getString(Page.SIMPLE_DATA_KEY)); 
	            		}
	            	} else if (page instanceof RestoreBackupChoicePage) {          		
	            		if (page.getData().getString(Page.SIMPLE_DATA_KEY).equals(
	            				getResources().getStringArray(activity_setup__restore_backup__choices)[0])) {
			    			
	            	        // Restore the latest backup
	            	        backupProvider.restoreLatestBackup();   
	            		} else if (page.getData().getString(Page.SIMPLE_DATA_KEY).equals(
	            				getResources().getStringArray(activity_setup__restore_backup__choices)[1])) {
	            	       
	            			// Restore a user selected backup
	            	        backupProvider.restoreBackup();   
	            		} else {
	              			LOGGER.error("Restore option {} not recognized", page.getData().getString(Page.SIMPLE_DATA_KEY));
	            		}
	            	} else if (page instanceof PgpWordListInputPage){
	            		final String pgpWords = page.getData().getString(Page.SIMPLE_DATA_KEY);	            		
	        			final byte[] userSecret;
						try {
							userSecret = new PgpWordListByteString(SetupActivity.this).fromWords(pgpWords);
				      		backupProvider.decryptRestoredBackup(userSecret);
						} catch (InvalidWordException e) {
							onRestoreBackupFailure();
						}	        				     	            		
	            	} else {
	                    if (mEditingAfterReview) {
	                    	// Return to the review page
	                    	reviewPage();
	                    } else {
	                    	// Advance to the next page
	                    	nextPage();
	                    }
	                }
            	}
            }
        });

        mPrevButton.setOnClickListener(new View.OnClickListener() {
        	
            @Override
            public void onClick(final View view) {
            	
            	// Return the Wizard to the previous page
            	previousPage();
            }
        });        
        
        onPageTreeChanged();
        updateBottomBar();
    }

    private void setRestoreOptions(final Page page) {
    	if (page instanceof RestoreBackupChoicePage) {
    		final RestoreBackupChoicePage rbcPage = (RestoreBackupChoicePage) page;
    		
        	// Set the restore options
       		final List<String> choices = new ArrayList<String>();
           	for (final RestoreOption option : backupProvider.getRestoreOptions()) {
           		switch (option) {
           		case RESTORE_LATEST:
           			choices.add(getResources().getStringArray(R.array.activity_setup__restore_backup__choices)[0]);
           			break;
           		case RESTORE_USER_SELECTED:
           			choices.add(getResources().getStringArray(R.array.activity_setup__restore_backup__choices)[1]); 
           			break;
           		default:
           			break;
           		}
           	}
           	rbcPage.setChoices(choices.toArray(new String[choices.size()]));  
    		mWizardModel.onPageTreeChanged();  
    	}
    }
    
    @Override
    public void onPageTreeChanged() {
        mCurrentPageSequence = mWizardModel.getCurrentPageSequence();
        recalculateCutOffPage();
        mStepPagerStrip.setPageCount(mCurrentPageSequence.size() + 1); // + 1 = review step
        mPagerAdapter.notifyDataSetChanged();
        updateBottomBar();
    }

    private void updateBottomBar() {
        int position = mPager.getCurrentItem();
        if (position == mCurrentPageSequence.size()) {
            mNextButton.setText(R.string.activity_setup__setup_completed);
            mNextButton.setBackgroundResource(R.drawable.finish_background);
            mNextButton.setTextAppearance(this, R.style.TextAppearanceFinish);
        } else {
            mNextButton.setText(mEditingAfterReview
                    ? R.string.review
                    : R.string.next);
            mNextButton.setBackgroundResource(R.drawable.selectable_item_background);
            TypedValue v = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.textAppearanceMedium, v, true);
            mNextButton.setTextAppearance(this, v.resourceId);
            final Page page = mCurrentPageSequence.get(position);
            if (page instanceof SubmitPage) {
                mNextButton.setEnabled(((SubmitPage) page).isReadyToSubmit());
            } else {
                mNextButton.setEnabled(position != mPagerAdapter.getCutOffPage());
            }
        }

        mPrevButton.setVisibility(position <= 0 ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        mWizardModel.unregisterListener(this);
    	LocalBroadcastManager.getInstance(this).unregisterReceiver(responseReceiver);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(SETUP_ACTIVITY_MODEL, mWizardModel.save());
        outState.putString(TERMINAL_NAME, terminalName);
        outState.putByteArray(TERMINAL_COMMITMENT, commitment);
        outState.putParcelable(TERMINAL_ADDRESS, terminalAddress);
        outState.putParcelable(NONCE, nonce);
    }

    @Override
    public AbstractWizardModel onGetModel() {
        return mWizardModel;
    }

    @Override
    public void onEditScreenAfterReview(final String key) {
        for (int i = mCurrentPageSequence.size() - 1; i >= 0; i--) {
            if (mCurrentPageSequence.get(i).getKey().equals(key)) {
                mConsumePageSelectedEvent = true;
                mEditingAfterReview = true;
                mPager.setCurrentItem(i);
                updateBottomBar();
                break;
            }
        }
    }

    @Override
    public void onPageDataChanged(final Page page) {
        if (page.isRequired()) {
            if (recalculateCutOffPage()) {
                mPagerAdapter.notifyDataSetChanged();
            }
            updateBottomBar();
        }
    }

    @Override
    public Page onGetPage(final String key) {
        return mWizardModel.findByKey(key);      
    }

    private boolean recalculateCutOffPage() {
        // Cut off the pager adapter at first required page that isn't completed
        int cutOffPage = mCurrentPageSequence.size() + 1;
        for (int i = 0; i < mCurrentPageSequence.size(); i++) {
            Page page = mCurrentPageSequence.get(i);
            if (page.isRequired() && !page.isCompleted()) {
                cutOffPage = i;
                break;
            }
        }

        if (mPagerAdapter.getCutOffPage() != cutOffPage) {
            mPagerAdapter.setCutOffPage(cutOffPage);
            return true;
        }

        return false;
    }
    
    public final class MyPagerAdapter extends FragmentStatePagerAdapter {
        private int mCutOffPage;
        private Fragment mPrimaryItem;

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(final int i) {
            if (i >= mCurrentPageSequence.size()) {
                return new ReviewFragment();
            }
        	return mCurrentPageSequence.get(i).createFragment();
        }

        @Override
        public int getItemPosition(final Object object) {
            // TODO: be smarter about this
            if (object == mPrimaryItem) {
                // Re-use the current fragment (its position never changes)
                return POSITION_UNCHANGED;
            }

            return POSITION_NONE;
        }

        @Override
        public void setPrimaryItem(final ViewGroup container, final int position,
        		final Object object) {
            super.setPrimaryItem(container, position, object);
            mPrimaryItem = (Fragment) object;
        }

        @Override
        public int getCount() {
            if (mCurrentPageSequence == null) {
                return 0;
            }
            return Math.min(mCutOffPage + 1, mCurrentPageSequence.size() + 1);
        }

        public void setCutOffPage(int cutOffPage) {
            if (cutOffPage < 0) {
                cutOffPage = Integer.MAX_VALUE;
            }
            mCutOffPage = cutOffPage;
        }

        public int getCutOffPage() {
            return mCutOffPage;
        }
    }
    
    @Override
	public void onConfigureBackupFailure() {
        LOGGER.error("Error configuring backup provider!");
        
		// Display to the user a dialog informing them that configuring the backup provider failed
		final DialogFragment dg = new DialogFragment() {
        	
            @Override
            public Dialog onCreateDialog(final Bundle savedInstanceState) {
                return new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.activity_setup__configure_backup_failure)
                        .setNegativeButton(R.string.activity_setup__exit_confirm,
                        		new DialogInterface.OnClickListener() {

									@Override
									public void onClick(
											final DialogInterface dialog,
											final int which) {						
								        setResult(RESULT_CANCELED);
								        finish();
									}
                        	})
                        .setPositiveButton(R.string.activity_setup__exit_cancel, null)
                        .create();
            }
        };
        dg.show(getSupportFragmentManager(), ACTIVITY_SETUP_FRAGMENT_TAG);
        
        // Detach the BackupProviderFragment
 	    final android.app.Fragment fragment = getFragmentManager()
          		.findFragmentByTag(BackupProviderFragment.TAG); 	     
 	    if (fragment != null) {
	        LOGGER.debug("Detaching BackupProvider fragment");
	        
	    	final FragmentTransaction ft = getFragmentManager().beginTransaction();
	        ft.detach(fragment);
	        ft.commit();  
	        
	        backupProvider = null;
	    } 
	}  	
	
	@Override
	public void onConfigureBackupCancelled() {
        LOGGER.warn("Configuration of backup provider cancelled!");
    	
        Toast.makeText(this, getString(R.string.activity_setup__configure_backup_cancelled), 
                Toast.LENGTH_LONG).show();
        
        // Detach the BackupProviderFragment
 	    final android.app.Fragment fragment = getFragmentManager()
          		.findFragmentByTag(BackupProviderFragment.TAG); 	     
 	    if (fragment != null) {
	        LOGGER.debug("Detaching BackupProvider fragment");
	        
	    	final FragmentTransaction ft = getFragmentManager().beginTransaction();
	        ft.detach(fragment);
	        ft.commit();  
	        
	        backupProvider = null;
	    } 
	}
	
    @Override
    public void onConfigureBackupSuccess(final IBackupProvider backupProvider) {
    	// Verify the method's preconditions
    	checkNotNull(backupProvider);    	
        
        LOGGER.info("Backup successfully configured!");
        
        // Set the configured backup in the shared preferences
        BackupFactory.persistBackupType(backupProvider.getBackupType());
     
        // Store the configured backup provider
    	this.backupProvider = backupProvider;
        
    	// If a backup is being restored query the backup provider
    	final Page page = mCurrentPageSequence.get(mPager.getCurrentItem());
    	if (page instanceof SelectBackupProviderPage) {
    		 final SelectBackupProviderPage spbPage = (SelectBackupProviderPage) page;
    		 if (page == mWizardModel.findByKey(getString(R.string.activity_setup__new_user__setup) + ":" +
 					getString(R.string.activity_setup__backup_to__title))) {
    			 spbPage.setValue(SelectBackupProviderPage.BACKUP_PROVIDER_KEY,
         				backupProvider.getBackupType().getProviderName());
     			 spbPage.notifyDataChanged();
     			 
 	    		// Advance the Wizard to the next page
 	    		nextPage();
    		 } else {
    			 backupProvider.isEmpty();
    		 }
    	}
    }	  

	@Override
	public void onRestoreBackupStart() {
		LOGGER.trace("Started restoring Pico pairings and services database");
	}
	
	@Override
	public void onRestoreBackupSuccess() {
        LOGGER.info("Restoring backup successful!");
        
		// Advance the Wizard to the next page
		nextPage();
	}
	
	@Override
	public void onRestoreBackupCancelled() {		
        LOGGER.info("Restoring backup cancelled!");       
        
        Toast.makeText(this, getString(R.string.activity_setup__restore_backup_cancelled), 
                Toast.LENGTH_LONG).show();
	}

	@Override
	public void onRestoreBackupFailure() {
        LOGGER.error("Restoring backup failed!");
        
		// Display to the user a dialog informing them that restoring the backup failed
		final DialogFragment dg = new DialogFragment() {
        	
            @Override
            public Dialog onCreateDialog(final Bundle savedInstanceState) {
                return new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.activity_setup__restore_backup_failure)
                        .setNegativeButton(R.string.activity_setup__exit_confirm,
                        		new DialogInterface.OnClickListener() {

									@Override
									public void onClick(
											final DialogInterface dialog,
											final int which) {						
								        setResult(RESULT_CANCELED);
								        finish();
									}
                        	})
                        .setPositiveButton(R.string.activity_setup__exit_cancel, null)
                        .create();
            }
        };
        dg.show(getSupportFragmentManager(), ACTIVITY_SETUP_FRAGMENT_TAG);
	}

	@Override
	public void onQueryBackupIsNotEmpty() {
		LOGGER.debug("Backup provider is not empty");        

        // Set the backup provider
    	final Page page = mCurrentPageSequence.get(mPager.getCurrentItem());
    	if (page instanceof SelectBackupProviderPage) {
    		final SelectBackupProviderPage sbpPage = (SelectBackupProviderPage) page;
    		sbpPage.setValue(SelectBackupProviderPage.BACKUP_PROVIDER_KEY,
       				backupProvider.getBackupType().toString());
    		sbpPage.notifyDataChanged();
    	}
    	
		 // Set the restore options
    	setRestoreOptions(mCurrentPageSequence.get(mPager.getCurrentItem() + 1));
       	       	       	
		// Advance the Wizard to the next page
		nextPage();
	}

	@Override
	public void onQueryBackupIsEmpty() {
		LOGGER.debug("Backup provider is empty");
		
		// Display a dialog to the user informing them that there are no backups to restore
		final DialogFragment dg = new DialogFragment() {
        	
            @Override
            public Dialog onCreateDialog(final Bundle savedInstanceState) {
                return new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.activity_setup__query_backup_is_empty)
                        .setNegativeButton(R.string.activity_setup__exit_confirm,
                        		new DialogInterface.OnClickListener() {

									@Override
									public void onClick(
											final DialogInterface dialog,
											final int which) {						
								        setResult(RESULT_CANCELED);
								        finish();
									}
                        	})
                        .setPositiveButton(R.string.activity_setup__exit_cancel, null)
                        .create();
            }
        };
        dg.show(getSupportFragmentManager(), ACTIVITY_SETUP_FRAGMENT_TAG);
	}

	@Override
	public void onQueryBackupFailure() {
		LOGGER.error("Error querying the backup provider");
		
		// Display a dialog to the user informing them that querying their cloud provider has failed
		final DialogFragment dg = new DialogFragment() {
        	
            @Override
            public Dialog onCreateDialog(final Bundle savedInstanceState) {
                return new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.activity_setup__query_backup_failure)
                        .setNegativeButton(R.string.activity_setup__exit_confirm,
                        		new DialogInterface.OnClickListener() {

									@Override
									public void onClick(
											final DialogInterface dialog,
											final int which) {						
								        setResult(RESULT_CANCELED);
								        finish();
									}
                        	})
                        .setPositiveButton(R.string.activity_setup__exit_cancel, null)
                        .create();
            }
        };
        dg.show(getSupportFragmentManager(), ACTIVITY_SETUP_FRAGMENT_TAG);       
	}

	@Override
	public void onRestoreBackupDownloaded() {
        LOGGER.info("Downloading backup successful!");        
        
    	final Page page = mCurrentPageSequence.get(mPager.getCurrentItem());
    	if (page instanceof RestoreBackupChoicePage) {
    		final RestoreBackupChoicePage restoreBackupChoicePage = (RestoreBackupChoicePage) page;
    		restoreBackupChoicePage.setValue(RestoreBackupChoicePage.RESTORED_KEY, "Success");
    		restoreBackupChoicePage.notifyDataChanged();
    	}
    	
		// Advance the Wizard to the next page
		nextPage();
	}
}