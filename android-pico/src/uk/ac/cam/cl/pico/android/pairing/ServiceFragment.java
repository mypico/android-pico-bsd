/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.pairing;

import java.util.Locale;

import uk.ac.cam.cl.pico.android.R;
import uk.ac.cam.cl.pico.android.data.SafeService;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

final public class ServiceFragment extends Fragment {

    private static final String TAG = ServiceFragment.class.getSimpleName();

    private View fragmentView;
    private SafeService service;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getActivity().getIntent();
        service = (SafeService) intent.getParcelableExtra(
                SafeService.class.getCanonicalName());
        if (service != null) {
            Log.d(TAG, "Got service info from parent activity's intent");
        } else {
            Log.w(TAG, "Failed to get service info from parent activity's intent");
        }
        Log.d(TAG, "Fragment created");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_service, container, false);
        updateView();
        Log.d(TAG, "Fragment view created");
        return fragmentView;
    }

    private void updateView() {
        if (service != null) {
            String serviceName;
            final Uri serviceAddress;
            String serviceHost;

            if ((serviceName = service.getName()) == null) {
                serviceName = getString(R.string.unknown_service_name);
            }

            if ((serviceAddress = service.getAddress()) == null) {
                serviceHost = getString(R.string.unknown_service_host);
            } else {
                serviceHost = serviceAddress.getHost();
            }

            ((TextView) fragmentView.findViewById(
                    R.id.fragment_service__text1)).setText(serviceName);
            ((TextView) fragmentView.findViewById(
                    R.id.fragment_service__text2)).setText(serviceHost);

            // TODO change the way this works, it needs to be a bit more
            // complicated to allow users to not retrieve logos.
            // ((ImageView) fragmentView.findViewById(
            // R.id.fragment_service__logo)).setImageURI(service.getLogoUri());
            // For now just load a small set of them
            final String serviceNameLower = serviceName.toLowerCase(Locale.UK);
            final int resource;
            if (serviceNameLower.contains("gmail"))
                resource = R.drawable.gmail;
            else if (serviceHost.contains("facebook.com"))
                resource = R.drawable.facebook;
            else if (serviceHost.contains("linkedin.com"))
                resource = R.drawable.linkedin;
            else if (serviceNameLower.contains("wordpress"))
                resource = R.drawable.wordpress;
            else
                resource = R.drawable.generic_website;

            ((ImageView) fragmentView.findViewById(
                    R.id.fragment_service__logo)).setImageResource(resource);
        }
    }
}
