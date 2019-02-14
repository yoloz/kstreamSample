package com.ks.bean;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * logger 输出 InputStream
 */
public class ExportInputStream extends Thread {

    public enum Level {
        INFO, WARN, ERROR
    }

    private InputStream is;
    private final Logger logger;
    private final Level level;
    private final String prefix;

    public ExportInputStream(InputStream is, Logger logger, Level level, String prefix) {
        this.is = is;
        this.logger = logger;
        this.level = level;
        this.prefix = prefix;
    }

    /**
     * If this thread was constructed using a separate
     * <code>Runnable</code> run object, then that
     * <code>Runnable</code> object's <code>run</code> method is called;
     * otherwise, this method does nothing and returns.
     * <p>
     * Subclasses of <code>Thread</code> should override this method.
     *
     * @see #start()
     * @see #stop()
     */
    @Override
    public void run() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while (!isInterrupted() && (line = br.readLine()) != null) {
                String msg = prefix + "---->" + line;
                switch (level) {
                    case WARN:
                        logger.warn(msg);
                        break;
                    case ERROR:
                        logger.error(msg);
                        break;
                    default:
                        logger.info(msg);
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
