package com.unimas.kska.integration;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.CountDownLatch;

public class ServerTest {

    private final int port = 12583;
    private Server server;

    private final KsServer ksServer;

    public ServerTest() throws Exception {
        this.ksServer = new KsServer();
    }

    private void start() throws Exception {
        this.server = new Server(port);
        this.server.setHandler(new ProxyHandler());
        this.server.start();
    }

    private void stop() {
        try {
            this.ksServer.close();
            if (this.server != null) this.server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ProxyHandler extends AbstractHandler {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request,
                           HttpServletResponse response) {
            baseRequest.setHandled(true);
            if (target.startsWith("/cii/ks") || target.startsWith("/cii/ka") || target.startsWith("/cii/ds")) {
                ksServer.service(target, baseRequest, response);
            } else {
                System.out.println("==========scb========");
            }
        }

    }

    public static void main(String[] args) {
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            final ServerTest serverTest = new ServerTest();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                serverTest.stop();
                latch.countDown();
            }));
            serverTest.start();
            latch.await();
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        }
        System.exit(0);
    }

}
