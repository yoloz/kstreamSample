package com.unimas.kstream.web;

import com.unimas.kstream.JettyServer;
import com.unimas.kstream.bean.ExportInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

/**
 * {@link StoreApp 入口文件统一命名:main.properties}
 */
class TaskQuote extends Thread {

    private final Logger logger = LoggerFactory.getLogger(TaskQuote.class);

    private final String appId;
    private ExportInputStream error;
    private ExportInputStream info;


    TaskQuote(String app_id) {
        this.appId = app_id;
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
        logger.info("application " + appId + " begin running=============");
        Path taskFile = JettyServer.app_dir.resolve(appId).resolve("main.properties");
        String[] commands = new String[]{JettyServer.bin_dir.resolve("ks-app-start.sh").toString(), taskFile.toString(), "start"};
        try {
            Process process = Runtime.getRuntime().exec(commands, null, taskFile.getParent().toFile());
            String prefix = "appId=" + appId;
            error = new ExportInputStream(process.getErrorStream(), logger,
                    ExportInputStream.Level.ERROR, prefix);
            error.start();
            info = new ExportInputStream(process.getInputStream(), logger,
                    ExportInputStream.Level.INFO, prefix);
            info.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                JettyServer.caches.get(appId).setStatus(JettyServer.Status.START);
            } else {
                logger.warn(appId + " process code " + exitCode);
                JettyServer.caches.get(appId).setStatus(JettyServer.Status.ODD);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.error(appId, e.getMessage());
            this.interruptTask();
        }
    }

    private void interruptTask() {
//        this.interrupt();
        if (error != null) error.interrupt();
        if (info != null) info.interrupt();
    }
}
