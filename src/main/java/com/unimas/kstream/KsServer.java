package com.unimas.kstream;

import com.google.common.collect.ImmutableList;
import com.unimas.kstream.process.KServerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

/**
 * main
 */
public class KsServer {

    public static final String app_dir = System.getProperty("ks.app.dir");

    public static void main(String[] args) {
        parseArgs(args);
        Logger logger = LoggerFactory.getLogger(KsServer.class);
        try {
            if ("stop".equals(args[1])) {
                Path pf = Paths.get(args[0]).getParent().resolve("pid");
                if (pf.toFile().exists()) {
                    StopProcess.stop(pf);
                } else logger.warn(pf + " is lost or is not running");
            } else {
                CountDownLatch latch = new CountDownLatch(1);
                KServerImpl kServerImpl = new KServerImpl(args[0], latch, "test".equals(args[1]));
                Runtime.getRuntime().addShutdownHook(new Thread(kServerImpl::close));
                kServerImpl.start();
                latch.await();
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            StopProcess.stop(Paths.get(KsServer.app_dir, "pid"));
            System.exit(-1);
        }
        System.exit(0);
    }

    private static void parseArgs(String[] args) {
        if (args == null || args.length < 2) {
            System.err.printf("command error...\n%s", "USAGE:KsServer main.properties start|test|stop");
            System.exit(-1);
        }
        if (!Paths.get(args[0]).toFile().exists()) {
            System.err.printf("file %s does not exist...", args[0]);
            System.exit(-1);
        }
        if (!ImmutableList.of("start", "test", "stop").contains(args[1])) {
            System.err.printf("param %s is not defined\n%s", args[1], "USAGE:KsServer main.properties start|test|stop");
            System.exit(-1);
        }
    }
}
