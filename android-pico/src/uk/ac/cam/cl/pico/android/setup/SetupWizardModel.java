/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.setup;

import android.content.Context;

import com.example.android.wizardpager.wizard.model.AbstractWizardModel;
import com.example.android.wizardpager.wizard.model.BranchPage;
import com.example.android.wizardpager.wizard.model.PageList;

import static uk.ac.cam.cl.pico.android.R.string.activity_setup__welcome__title;
import static uk.ac.cam.cl.pico.android.R.string.activity_setup__new_user__title;
import static uk.ac.cam.cl.pico.android.R.string.activity_setup__pico_lens__title;
import static uk.ac.cam.cl.pico.android.R.string.activity_setup__new_user__setup;
import static uk.ac.cam.cl.pico.android.R.string.activity_setup__new_user__add_trusted_computer;
import static uk.ac.cam.cl.pico.android.R.string.activity_setup__pico_backup__title;
import static uk.ac.cam.cl.pico.android.R.string.activity_setup__backup_to__title;
import static uk.ac.cam.cl.pico.android.R.array.activity_setup__backup_to__choices;
import static uk.ac.cam.cl.pico.android.R.string.activity_setup__recovery_words__title;
import static uk.ac.cam.cl.pico.android.R.string.fragment_pgp_word_list_output__title;
import static uk.ac.cam.cl.pico.android.R.string.activity_setup__new_user__returning_user_restore;
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
public final class SetupWizardModel extends AbstractWizardModel {
	
    public SetupWizardModel(final Context context) {
        super(context);
    }

    @Override
    protected PageList onNewRootPageList() {
    	final Context picoAppContext = PicoApplication.getContext();
    	
    	return new PageList(	
				new WelcomePage(this, picoAppContext.getString(activity_setup__welcome__title)),
				new BranchPage(this, picoAppContext.getString(activity_setup__new_user__title))
                .addBranch(picoAppContext.getString(activity_setup__new_user__setup),
                	new PicoLensPage(this, picoAppContext.getString(activity_setup__pico_lens__title)),
                	new AddTrustedComputerPage(this, picoAppContext.getString(activity_setup__new_user__add_trusted_computer))
                		.setRequired(true),
					new BackupDescriptionPage(this, picoAppContext.getString(activity_setup__pico_backup__title)),
					new SelectBackupProviderPage(this, picoAppContext.getString(activity_setup__backup_to__title))
						.setChoices(picoAppContext.getResources().getStringArray(activity_setup__backup_to__choices))
						.setRequired(true),
					new RecoverWordsPage(this, picoAppContext.getString(activity_setup__recovery_words__title)),
					new PgpWordListOutputPage(this, picoAppContext.getString(fragment_pgp_word_list_output__title)))
				.addBranch(picoAppContext.getString(activity_setup__new_user__returning_user_restore),
					new SelectBackupProviderPage(this, picoAppContext.getString(activity_setup__backup_from__title))
						.setChoices(picoAppContext.getResources().getStringArray(activity_setup__backup_from__choices))
						.setRequired(true),
					new RestoreBackupChoicePage(this, picoAppContext.getString(activity_setup__restore_backup__title))
						.setChoices(picoAppContext.getResources().getStringArray(activity_setup__restore_backup__choices))
						.setRequired(true),
					new RestoreRecoveryWordsPage(this, picoAppContext.getString(activity_setup__restore_recovery_words__title)),
	    			new PgpWordListInputPage(this, picoAppContext.getString(fragment_pgp_word_list_input__title))
						.setRequired(true))
	    		.setRequired(true));
    }
}