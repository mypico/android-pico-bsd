/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.setup;

import android.content.Context;

import com.example.android.wizardpager.wizard.model.AbstractWizardModel;
import com.example.android.wizardpager.wizard.model.PageList;

import static uk.ac.cam.cl.pico.android.R.string.activity_setup__backup_from__title;
import static uk.ac.cam.cl.pico.android.R.array.activity_setup__backup_from__choices;
import static uk.ac.cam.cl.pico.android.R.string.activity_setup__restore_backup__title;
import static uk.ac.cam.cl.pico.android.R.array.activity_setup__restore_backup__choices;
import static uk.ac.cam.cl.pico.android.R.string.activity_setup__restore_recovery_words__title;
import static uk.ac.cam.cl.pico.android.R.string.fragment_pgp_word_list_input__title;
import uk.ac.cam.cl.pico.android.core.PicoApplication;

/**
 * TODO
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 *
 */
public final class RestoreBackupWizardModel extends AbstractWizardModel {
	
    public RestoreBackupWizardModel(final Context context) {
        super(context);
    }

    @Override
    protected PageList onNewRootPageList() {
    	final Context picoAppContext = PicoApplication.getContext();
    	
    	return new PageList(				
				new SelectBackupProviderPage(this, picoAppContext.getString(activity_setup__backup_from__title))
					.setChoices(picoAppContext.getResources().getStringArray(activity_setup__backup_from__choices))
					.setRequired(true),
				new RestoreBackupChoicePage(this, picoAppContext.getString(activity_setup__restore_backup__title))
					.setChoices(picoAppContext.getResources().getStringArray(activity_setup__restore_backup__choices))
					.setRequired(true),
				new RestoreRecoveryWordsPage(this, picoAppContext.getString(activity_setup__restore_recovery_words__title)),
    			new PgpWordListInputPage(this, picoAppContext.getString(fragment_pgp_word_list_input__title))
					.setRequired(true));
    }
}