/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.setup;

/**
 * SetupActivity provides Wizards for setting up Pico and restoring a backup.
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
 public class RestoreBackupActivity extends SetupActivity {
	
    {
    	mWizardModel = new RestoreBackupWizardModel(this);
    }
}