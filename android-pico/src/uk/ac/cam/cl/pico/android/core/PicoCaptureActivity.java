/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.core;

import uk.ac.cam.cl.pico.android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.zxing.Result;
import com.google.zxing.client.android.CaptureActivity;

/**
 * Extends the com.google.zxing.client.android.CaptureActivity class to allow
 * the QR-code capture UI to be modified.
 * 
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 * @author David Llewellyn-Jones <dl551@cam.ac.uk>
 * 
 */
public final class PicoCaptureActivity extends CaptureActivity {

	// Allow us to keep track of whether the device is connected
	// and the widget to display if we're not
	private boolean connectionRequired = true;
	private View noConnection;
	private ConnectivityReceiver broadcastReceiver;
	// This replicates the private value in
	// com.google.zxing.client.android.CaptureActivity
	private static final long BULK_MODE_SCAN_DELAY_MS = 1000L;

	/**
	 * Receives notifications from Android about connectivity changes These are
	 * registered dynamically in the code against the two intents
	 * android.net.conn.CONNECTIVITY_CHANGE and
	 * android.net.conn.WIFI_STATE_CHANGED which require the permissions
	 * android.permission.ACCESS_NETWORK_STATE to be added to the manifest.
	 * 
	 * See
	 * http://www.vogella.com/tutorials/AndroidBroadcastReceiver/article.html
	 * and
	 * http://www.androidhive.info/2012/07/android-detect-internet-connection
	 * -status/ and
	 * http://viralpatel.net/blogs/android-internet-connection-status
	 * -network-change/
	 * 
	 * @author David Llewellyn-Jones <dl551@cam.ac.uk>
	 * 
	 */
	private class ConnectivityReceiver extends BroadcastReceiver {
		private View connectionSymbol = null;
		private boolean currentlyConnected;

		/**
		 * Set the UI widget that will be hidden if there's a data connection
		 * available or shown if there's no data connection
		 * 
		 * @param connectionSymbol
		 *            The widget to show/hide depending on the connection
		 */
		public void setConectionView(View connectionSymbol) {
			this.connectionSymbol = connectionSymbol;
			updateWidgetVisibility();
		}

		/**
		 * Returns whether or not there's a currently available data connection
		 * 
		 * @return true if there's a connection available, false otherwise
		 */
		public boolean isConnected() {
			return currentlyConnected;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.content.BroadcastReceiver#onReceive(android.content.Context,
		 * android.content.Intent)
		 */
		@Override
		public void onReceive(Context context, Intent intent) {
			// Get session info from intent and check not null
			currentlyConnected = getConnectivityStatus(context);

			Log.d(ConnectivityReceiver.class.getSimpleName(),
					"Connectivity state changed to " + currentlyConnected);
			// Alter the visibility of the currently set widget based on the
			// state of the network
			updateWidgetVisibility();
		}

		/**
		 * Updates the visibility of the registered widget depending on the
		 * connectivity state
		 */
		private void updateWidgetVisibility() {
			if (connectionSymbol != null) {
				if (currentlyConnected == true) {
					connectionSymbol.setVisibility(View.INVISIBLE);
				} else {
					connectionSymbol.setVisibility(View.VISIBLE);
				}
			}
		}

		/**
		 * Determines whether there's an active cata connection (WiFi or mobile)
		 * 
		 * @param context
		 *            The Context in which the receiver is running.
		 * @return true if there's a WiFi or mobile data connection; false o/w
		 */
		private boolean getConnectivityStatus(Context context) {
			boolean connected = false;
			ConnectivityManager conman = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);

			// Check whether there's any kind of network
			NetworkInfo activeNetwork = conman.getActiveNetworkInfo();
			if (null != activeNetwork) {
				// Check the type of connection we have
				switch (activeNetwork.getType()) {
				case ConnectivityManager.TYPE_WIFI:
					// Intentional fallthrough
				case ConnectivityManager.TYPE_MOBILE:
					connected = true;
					break;
				default:
					// Not really necessary, but we do it for clarity
					connected = false;
					break;
				}
			}
			return connected;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.google.zxing.client.android.CaptureActivity#onCreate(android.os.Bundle
	 * )
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		// Capture network change messages
		broadcastReceiver = new ConnectivityReceiver();

		// Insert the "need network connectivity" warning into the existing UI
		// The triangle warning image used is from Wikipedia, in the public domain
		// See https://en.wikipedia.org/w/index.php?title=File:Ambox_warning_pn.svg&oldid=453640296
		Window insertPoint = getWindow();
		LayoutInflater inflater = getLayoutInflater();
		View warningView = inflater.inflate(R.layout.activity_capture_overlay,
				(ViewGroup) insertPoint.findViewById(R.id.result_view), false);
		noConnection = warningView.findViewById(R.id.noconnection);
		((LinearLayout) noConnection.getParent()).removeView(noConnection);
		insertPoint.addContentView(noConnection, new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.zxing.client.android.CaptureActivity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();

		// Set up the appropriate widget to show/hide
		// It will be made visible if the network becomes unavailable
		noConnection.setVisibility(android.widget.ImageView.INVISIBLE);

		if (connectionRequired) {
			// Register to receive info about when connectivity changes
			// This will be unregistered in onPause()
			// The manifest has android.permission.ACCESS_NETWORK_STATE
			// permission requested for this
			IntentFilter filter = new IntentFilter();
			filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
			filter.addAction("android.net.conn.WIFI_STATE_CHANGED");
			this.registerReceiver(broadcastReceiver, filter);

			// This will also set the appropriate visibility
			broadcastReceiver.setConectionView(noConnection);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.zxing.client.android.CaptureActivity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();

		if (connectionRequired) {
			// Unregister the receiver listening for when the connection changes
			// This was registered in onResume()
			this.unregisterReceiver(broadcastReceiver);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.google.zxing.client.android.CaptureActivity#onCreateOptionsMenu(android
	 * .view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		if (getIntent().getBooleanExtra(AcquireCodeActivity.NO_MENU, false)) {
			return false;
		} else {
			// Call the superclass's implementation to call the base
			// implementation.
			super.onCreateOptionsMenu(menu);

			// But now remove all of the items added by CaptureActivity
			menu.removeItem(com.google.zxing.client.android.R.id.menu_share);
			menu.removeItem(com.google.zxing.client.android.R.id.menu_history);
			menu.removeItem(com.google.zxing.client.android.R.id.menu_settings);
			menu.removeItem(com.google.zxing.client.android.R.id.menu_help);

			// Add our own items
			getMenuInflater().inflate(R.menu.pico_capture, menu);

			// Return true to ensure menu is displayed
			return true;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.google.zxing.client.android.CaptureActivity#handleDecode(com.google
	 * .zxing.Result, android.graphics.Bitmap, float)
	 */
	@Override
	public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
		if ((!connectionRequired) || broadcastReceiver.isConnected()) {
			// Decode the QR-code as expected
			super.handleDecode(rawResult, barcode, scaleFactor);
		} else { // Not connected
			if (connectionRequired) {
				// If we're not connected, warn the user and restart the code
				// checking, but don't process what we just saw
				Toast.makeText(
						getApplicationContext(),
						"Connect to a Wi-Fi or mobile network, then scan again",
						Toast.LENGTH_SHORT).show();
				restartPreviewAfterDelay(BULK_MODE_SCAN_DELAY_MS);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.google.zxing.client.android.CaptureActivity#onOptionsItemSelected
	 * (android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		final int itemId = item.getItemId();
		if (itemId == R.id.action_control_panel) {
			// Start control panel activity
			final Intent intent = new Intent(this, PicoStatusActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
}