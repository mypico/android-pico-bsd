/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.core;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.backup.BackupProviderFragment;
import uk.ac.cam.cl.pico.android.backup.PgpWordListOutputDialogFragment;
import uk.ac.cam.cl.pico.android.backup.SharedPreferencesBackupKey;
import uk.ac.cam.cl.pico.android.core.PicoServiceImpl.PicoServiceBinder;
import uk.ac.cam.cl.pico.android.data.SafeSession;
import uk.ac.cam.cl.pico.android.pairing.PairingsFragment;
import uk.ac.cam.cl.pico.android.setup.RestoreBackupActivity;
import uk.ac.cam.cl.pico.android.setup.SetupActivity;
import uk.ac.cam.cl.pico.android.terminal.TerminalsFragment;
import uk.ac.cam.cl.pico.android.util.PgpWordListByteString;
import uk.ac.cam.cl.pico.backup.BackupKey;
import uk.ac.cam.cl.pico.backup.BackupKeyRestoreStateException;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;

/**
 * TODO
 * 
 * @author TODO
 *
 */
final public class PicoStatusActivity extends TabsActivity
        implements CurrentSessionListFragment.Listener, ServiceConnection {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(PicoStatusActivity.class.getSimpleName());

    public static final int START_AUTHENTICATION_CODE = 1;
    public static final int CONFIGURE_BACKUP_PROVIDER_CODE = 2;
    
    @Override
    protected List<Class<? extends android.support.v4.app.Fragment>> getFragments() {
    	final List<Class<? extends android.support.v4.app.Fragment>> fragments = 
    			new LinkedList<Class<? extends android.support.v4.app.Fragment>>();
    	fragments.add(CurrentSessionListFragment.class);
    	fragments.add(PairingsFragment.class);
    	fragments.add(TerminalsFragment.class);
    	return fragments;
    }

    @Override
    protected List<CharSequence> getTitles() {
    	final List<CharSequence> fragments = new LinkedList<CharSequence>();
    	//fragments.add(getString(R.string.title_fragment_recent));
    	fragments.add(getString(R.string.title_fragment_recent));
    	fragments.add(getString(R.string.title_fragment_pairings));
    	fragments.add(getString(R.string.title_fragment_terminals));
    	return fragments;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.pico_status, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
		if (itemId == R.id.action_restore_backup) {
			final Intent restorBackupIntent = new Intent(this, RestoreBackupActivity.class);
			startActivityForResult(restorBackupIntent, SetupActivity.SETUP_RESULT_CODE);
			return true;
		} else if (itemId == R.id.action_display_user_secret) {
			LOGGER.debug("Displaying the user secret (used to encrypt/decrypt) backups");
			// Note that the backupKey is persists to the SharedPreferences
			// on creation
			try {
				final BackupKey backupKey = SharedPreferencesBackupKey.restoreInstance();
				
				// Display the user secret as a set of words from the PGP wordlist   
				final String pgpWords =
				    new PgpWordListByteString(this).toWords(backupKey.getUserSecret());           
				final DialogFragment newFragment =
				    PgpWordListOutputDialogFragment.newInstance(pgpWords.split("\\s"));
				newFragment.setRetainInstance(true);
				newFragment.show(getFragmentManager(), PgpWordListOutputDialogFragment.TAG);   
			} catch (BackupKeyRestoreStateException e) {
		    	LOGGER.error("BackupKey was not persisted");			    
		    }
			return true;
		} else if (itemId == R.id.action_scan) {
			final Intent intent = new Intent(this, AcquireCodeActivity.class);
			startActivity(intent);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        super.onActivityResult(requestCode, resultCode, result);
        
        // Forward to the fragment onActivity.
        // This is required startIntentSenderForResult() - used in the Google Drive API does not
        // forward the result to the fragment that called it
        final FragmentManager fragmentManager = getFragmentManager();
        final Fragment fragment = fragmentManager.findFragmentByTag(BackupProviderFragment.TAG);       
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, result);
        }
        
        if (requestCode == SetupActivity.SETUP_RESULT_CODE) {
            if (resultCode == RESULT_OK) {
            	LOGGER.info("Backup successfully restored");
            }
        }
    }
    
    /*
     * These methods implement the ServiceConnection interface and deal with binding and unbinding 
     * the PicoService during the activity lifecycle. 
     */
    private PicoService picoService;
    private boolean bound;

    @Override
    public void onStart() {
    	super.onStart();
    	// Bind to the Pico service
    	Intent intent = new Intent(this, PicoServiceImpl.class);
    	bindService(intent, this, Context.BIND_AUTO_CREATE);
    }   
    
    @Override
    public void onStop() {
    	super.onStop();
    	// Unbind from the Pico service
    	if (bound) {
	    unbindService(this);
	    bound = false;
    	}
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        picoService = ((PicoServiceBinder) binder).getService();
        bound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        bound = false;
    }
    
    /*
     * These methods handle events coming from the nested session list fragments.
     */
    @Override
    public void onSessionResume(SafeSession session) {
		if (bound) {
		    picoService.resumeSession(session);
		} else {
		    LOGGER.warn("pico service not bound, cannot resume session");
		}
    }

    @Override
    public void onSessionPause(SafeSession session) {
		if (bound) {
		    picoService.pauseSession(session);
		} else {
		    LOGGER.warn("pico service not bound, cannot pause session");
		}
    }

    @Override
    public void onSessionClose(SafeSession session) {
		if (bound) {
		    picoService.closeSession(session);
		} else {
		    LOGGER.warn("pico service not bound, cannot close session");
		}
    }
}
