package com.github.mrramych.shakalbot;

import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.status.ErrorStatus;

import java.io.IOException;

public class Appender<E> extends ConsoleAppender<E> {

    @Override
    protected void subAppend(Object event) {
        if (!isStarted()) {
            return;
        }
        try {
            // this step avoids LBCLASSIC-139
            if (event instanceof DeferredProcessingAware) {
                ((DeferredProcessingAware) event).prepareForDeferredProcessing();
            }
            // the synchronization prevents the OutputStream from being closed while we
            // are writing. It also prevents multiple threads from entering the same
            // converter. Converters assume that they are in a synchronized block.
            // lock.lock();

            byte[] byteArray = this.encoder.encode((E) event);

            for (int i = 0; i < byteArray.length - 1; i++) {
                if (byteArray[i] == '\n') {
                    byteArray[i] = '\r';
                }
            }

            lock.lock();
            try {
                getOutputStream().write(byteArray);
                if (isImmediateFlush()) {
                    getOutputStream().flush();
                }
            } finally {
                lock.unlock();
            }
        } catch (IOException ioe) {
            // as soon as an exception occurs, move to non-started state
            // and add a single ErrorStatus to the SM.
            this.started = false;
            addStatus(new ErrorStatus("IO failure in appender", this, ioe));
        }
    }


}
