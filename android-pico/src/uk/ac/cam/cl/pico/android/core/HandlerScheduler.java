/**
 * Copyright Pico project, 2016
 */

package uk.ac.cam.cl.pico.android.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.pico.crypto.ContinuousProver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.util.Printer;

/**
 * HandlerScheduler manages the scheduling for continuous authentication.
 * <p>
 * Whenever a ContinuousProver reauthenticates successfully, it is sent a reauthentication time by
 * the service. It then calls setTimer of its Scheduler.
 * <p>
 * The Scheduler takes responsibility to ensure .prover is called exactly once and within the
 * specified time (in milliseconds). It could be expanded to e.g. reauthenticate earlier to
 * synchronise with other sessions to save power.
 * 
 * <p>
 * It uses the singleton pattern, so all ContinuousProvers share a single HandlerScheduler.
 * 
 * @author ms955
 * 
 */
final class HandlerScheduler extends Handler implements ContinuousProver.SchedulerInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(HandlerScheduler.class
            .getSimpleName());
    private static HandlerScheduler instance;

    private HandlerScheduler(Looper looper) {
        super(looper);
        looper.setMessageLogging(new Printer() {
            @Override
            public void println(String x) {
                LOGGER.trace(x);
            }
        });
    }

    private class UpdateRunnable implements Runnable {

        private final ContinuousProver prover;

        public UpdateRunnable(ContinuousProver prover) {
            this.prover = prover;
        }

        @Override
        public void run() {
            prover.updateVerifier();
        }
    }

    // Synchronised to prevent race conditions leading to two schedulings, perhaps could lock per
    // prover, as that might be faster.
    @Override
    synchronized public void setTimer(int milliseconds, ContinuousProver prover) {
        clearTimer(prover);

        // Enqueue new callback
        LOGGER.debug("Scheduling reauthentication in {} milliseconds", milliseconds);
        postAtTime(new UpdateRunnable(prover), prover, SystemClock.uptimeMillis() + milliseconds);
    }

    @Override
    synchronized public void clearTimer(ContinuousProver prover) {
        // Remove any existing queue callbacks for this prover:
        LOGGER.debug("Clearing any existing scheduled reauthentications");
        removeCallbacksAndMessages(prover);
    }

    public static HandlerScheduler getInstance() {
        if (instance == null) {
            // Start a new thread for the continuous auth event loop
            HandlerThread thread = new HandlerThread(
                    "Continuous Auth Thread",
                    Process.THREAD_PRIORITY_BACKGROUND
                    );
            thread.start();
            instance = new HandlerScheduler(thread.getLooper());
        }

        return instance;
    }
}
