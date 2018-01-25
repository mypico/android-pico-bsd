/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.pairing;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.data.SafeKeyPairing;
import android.content.Intent;
import android.os.Bundle;



final public class ChooseKeyPairingActivity
        extends ChoosePairingActivity
        implements KeyPairingListFragment.Listener {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_key_pairing);
    }
    
    @Override
    public void onSinglePairing(final SafeKeyPairing pairing) {
        // Verify the method's preconditions
        checkNotNull(pairing, "Pairing cannot be null");

        // Automatically authenticate
        authenticate(pairing);
    }

    @Override
    public void onPairingClicked(final SafeKeyPairing pairing) {
        // Verify the method's preconditions
        checkNotNull(pairing, "Pairing cannot be null");

        authenticate(pairing);
    }

    @Override
    public void onNoPairings() {
        showNoPairingsToast();
    }

    @Override
    public void onMultiplePairings(int count) {
        // Verify the method's preconditions
        checkArgument(count > 1, "Multiple pairings expected");
        
        hideSpinner();
    }

    private void authenticate(final SafeKeyPairing pairing) {
        // Verify the method's preconditions
        assert (pairing != null);

        final Intent intent = new Intent(this, AuthenticateActivity.class);
        intent.putExtra(SafeKeyPairing.class.getCanonicalName(), pairing);

        // Also include all of the extras from the received intent to forward the terminal details
        // if they are present and the extra data
        intent.putExtras(getIntent());
        
        // Setting this flag means that the next activity will pass any
        // result back to the result target of this activity.
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        startActivity(intent);
        finish();
    }
}
