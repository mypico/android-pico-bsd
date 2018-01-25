/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.core;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;

import uk.ac.cam.cl.pico.android.data.SafeSession;
import uk.ac.cam.cl.pico.crypto.ContinuousProver;
import uk.ac.cam.cl.pico.data.session.Session;

final class SessionUpdateBroadcaster implements ContinuousProver.ProverStateChangeNotificationInterface {

    private final LocalBroadcastManager localBroadcastManager;
    
    SessionUpdateBroadcaster(final LocalBroadcastManager localBroadcastManager) {
    	// Verify the method's preconditions
        this.localBroadcastManager = checkNotNull(localBroadcastManager);
    }

    private void broadcastInfo(final Session session) {
        PicoServiceImpl.LOGGER.debug("Broadcasting session info update");
        final Intent intent = new Intent(PicoServiceImpl.SESSION_INFO_UPDATE);
        intent.putExtra(SafeSession.class.getCanonicalName(), new SafeSession(session));
        localBroadcastManager.sendBroadcast(intent);
    }

    @Override
    public void sessionPaused(final Session session) {
    	// Verify the method's preconditions
    	checkNotNull(session, "Session cannot be null");
        session.setStatus(Session.Status.PAUSED);
        broadcastInfo(session);
    }

    @Override
    public void sessionContinued(final Session session) {
    	// Verify the method's preconditions
    	checkNotNull(session, "Session cannot be null");
        session.setStatus(Session.Status.ACTIVE);
        broadcastInfo(session);
    }

    @Override
    public void sessionStopped(final Session session) {
    	// Verify the method's preconditions
    	checkNotNull(session, "Session cannot be null");
        session.setStatus(Session.Status.CLOSED);
        broadcastInfo(session);
    }

    @Override
    public void sessionError(final Session session) {
    	// Verify the method's preconditions
    	checkNotNull(session, "Session cannot be null");
        session.setStatus(Session.Status.ERROR);
        broadcastInfo(session);
    }

    @Override
    public void tick(final Session session) {
        session.setLastAuthDate(new Date());
        broadcastInfo(session);
    }
}