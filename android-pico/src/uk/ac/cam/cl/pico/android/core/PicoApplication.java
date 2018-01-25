/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.core;

import java.security.Security;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Application;
import android.content.Context;
import uk.ac.cam.cl.pico.android.crypto.PrngFixes;
import uk.ac.cam.cl.pico.android.data.SafeSession;
import uk.ac.cam.cl.pico.comms.CombinedVerifierProxy;

/**
 * Initialisation of services, crypto, ... on starting the application.
 * 
 * @author Graeme Jenkinson <gcj21@cl.cam.ac.uk>
 * 
 */
final public class PicoApplication extends Application {
	private static Map<SafeSession,CombinedVerifierProxy> proxies = new HashMap<SafeSession, CombinedVerifierProxy>();
    private final static Logger LOGGER =
            LoggerFactory.getLogger(PicoApplication.class.getSimpleName());
    private static Context mContext;
    
    static {
        // Install Spongycastle as the first security provider
        LOGGER.debug("Installing Spongycastle security provider");
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);

        // Apply Android PRNG fix (if applicable depending on API version).
        // Note: Greater than 4.2 is not thought to be affected as SecureRandom
        // was re-implemented.
        LOGGER.debug("Applying fixes to SecureRandom (if necessary)");
        PrngFixes.apply();      
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        LOGGER.debug("Application started (onCreate called)");   
    }
    
    public static Context getContext(){
        return mContext;
    }
    
    public static void addProxy(SafeSession session, CombinedVerifierProxy proxy){
    	proxies.put(session,proxy);
    }
    
    public static CombinedVerifierProxy getProxy(SafeSession session){
    	return proxies.get(session);
    }
    
    public static CombinedVerifierProxy removeProxy(SafeSession session){
    	return proxies.remove(session);
    }
}