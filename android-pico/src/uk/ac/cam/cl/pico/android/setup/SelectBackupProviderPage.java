/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.setup;

import java.util.ArrayList;

import android.text.TextUtils;

import com.example.android.wizardpager.wizard.model.ModelCallbacks;
import com.example.android.wizardpager.wizard.model.ReviewItem;
import com.example.android.wizardpager.wizard.model.SingleFixedChoicePage;

public class SelectBackupProviderPage extends SingleFixedChoicePage implements SubmitPage {

	public static final String BACKUP_PROVIDER_KEY = "BackupProvider";
	
	public SelectBackupProviderPage(final ModelCallbacks callbacks, final String title) {
		super(callbacks, title);
	}
	
    @Override
    public void getReviewItems(ArrayList<ReviewItem> dest) {
        dest.add(new ReviewItem(getTitle(), mData.getString(BACKUP_PROVIDER_KEY), getKey()));
    }
    
    public SingleFixedChoicePage setValue(final String key, final String value) {
        mData.putString(key, value);
        return this;
    }

    public SingleFixedChoicePage removeKey(final String key) {
        mData.remove(key);
        return this;
    }
    
    @Override
    public boolean isCompleted() {
        return !TextUtils.isEmpty(mData.getString(BACKUP_PROVIDER_KEY));
    }
    
    @Override
    public boolean isReadyToSubmit() {
        return !TextUtils.isEmpty(mData.getString(SIMPLE_DATA_KEY));
    }
}