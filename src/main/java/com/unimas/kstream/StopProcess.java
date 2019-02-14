package com.unimas.kstream;

import com.google.common.io.Files;
import com.unimas.kstream.bean.ExportInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

/**
 * stop process by pid file
 */
public class StopProcess {

    private static final Logger logger = LoggerFactory.getLogger(StopProcess.class);


    public static void stop(Path path) {
        if (!path.toFile().exists()) return;
        try {
            String pid = Files.readFirstLine(path.toFile(), Charset.forName("UTF-8"));
            if (stopImpl(pid) == 0) java.nio.file.Files.delete(path);
            else {
                for (int i = 0; i < 3; i++) {
                    logger.warn("try to stop " + pid + " " + (i + 2) + " times");
                    if (stopImpl(pid) == 0) {
                        java.nio.file.Files.delete(path);
                        break;
                    }
                }
            }
            if (path.toFile().exists()) logger.error("stop " + pid + " failure...");
        } catch (IOException e) {
            logger.error("fail to stop " + path.toString(), e);
        }
    }


    private static int stopImpl(String pid) {
        ExportInputStream info = null;
        ExportInputStream error = null;
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c",
                    "kill -15 " + pid});
            String prefix = "pid=" + pid;
            info = new ExportInputStream(process.getInputStream(), logger,
                    ExportInputStream.Level.INFO, prefix);
            info.start();
            error = new ExportInputStream(process.getErrorStream(), logger,
                    ExportInputStream.Level.ERROR, prefix);
            error.start();
            return process.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (info != null) info.interrupt();
            if (error != null) error.interrupt();
        }
        return -1;
    }
}
