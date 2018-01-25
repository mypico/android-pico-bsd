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

import android.support.v4.app.Fragment;

import java.util.ArrayList;

import uk.ac.cam.cl.pico.android.backup.SharedPreferencesBackupKey;
import uk.ac.cam.cl.pico.android.core.PicoApplication;
import uk.ac.cam.cl.pico.android.util.PgpWordListByteString;
import uk.ac.cam.cl.pico.backup.BackupKey;

/**
 * A page offering the user a number of mutually exclusive choices.
 */
public class PgpWordListOutputPage extends Page {
	
	// Backup not restored, therefore, generate and display a new backup key
    // Note that the backupKey is persists to the SharedPreferences
    // on creation
	final BackupKey backupKey = SharedPreferencesBackupKey.newRandomInstance();
		
    // Display the user secret as a set of words from the PGP wordlist   
    final String pgpWords =
    		new PgpWordListByteString(PicoApplication.getContext()).toWords(backupKey.getUserSecret());   
    
    public PgpWordListOutputPage(ModelCallbacks callbacks, String title) {
        super(callbacks, title);
    }

    @Override
    public Fragment createFragment() {
    	return PgpWordListOutputFragment.newInstance(getKey(), pgpWords.trim().split("\\s"));
    }

    @Override
    public void getReviewItems(ArrayList<ReviewItem> dest) {
    	dest.add(new ReviewItem(getTitle(), mData.getString(SIMPLE_DATA_KEY), getKey()));
    }

    @Override
    public boolean isCompleted() {
        return true;
    }
}