package com.unimas.kstream;

import com.google.common.collect.ImmutableList;
import com.unimas.kstream.bean.AppInfo;
import com.unimas.kstream.error.KRunException;
import com.unimas.kstream.webservice.RegularlyUpdate;
import com.unimas.kstream.webservice.MysqlOperator;
import com.unimas.kstream.webservice.impl.DeleteApp;
import com.unimas.kstream.webservice.impl.DeployApp;
import com.unimas.kstream.webservice.impl.GetAllAppSys;
import com.unimas.kstream.webservice.impl.GetApp;
import com.unimas.kstream.webservice.impl.GetAppSys;
import com.unimas.kstream.webservice.impl.OrderApp;
import com.unimas.kstream.webservice.impl.StartApp;
import com.unimas.kstream.webservice.impl.StopApp;
import com.unimas.kstream.webservice.impl.StoreApp;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;


public class KsServer {

    private static final Logger logger = LoggerFactory.getLogger(KsServer.class);

    private static final String root_dir = System.getProperty("ks.root.dir");
    public static final Path app_dir = Paths.get(root_dir, "app");
    public static final Path bin_dir = Paths.get(root_dir, "bin");
    public static final ConcurrentHashMap<String, AppInfo> caches = new ConcurrentHashMap<>();

    private static MysqlOperator mysqlOperator = null;
    private final int port;
    private Server server;


    public KsServer() throws Exception {
        Path cf = Paths.get(root_dir, "config", "server.properties");
        if (cf.toFile().exists()) {
            Properties p = new Properties();
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(cf.toFile()), Charset.forName("UTF-8"))) {
                p.load(reader);
            }
            this.port = Integer.parseInt(p.getProperty("port"));
            mysqlOperator = new MysqlOperator(p);
            if (Files.notExists(app_dir, LinkOption.NOFOLLOW_LINKS)) Files.createDirectory(app_dir);
        } else {
            throw new Exception(cf + " is not exist!");
        }
    }

    public static MysqlOperator getMysqlOperator() {
        if (mysqlOperator == null) throw new KRunException("mysql operator is null...");
        return mysqlOperator;
    }

    private void start() throws Exception {
        this.server = new Server(port);
        ServletHandler servletHandler = new ServletHandler();
        servletHandler.addServletWithMapping(OrderApp.class, "/kstream/orderApp");
        servletHandler.addServletWithMapping(StoreApp.class, "/kstream/storeApp");
        servletHandler.addServletWithMapping(DeleteApp.class, "/kstream/deleteApp");
        servletHandler.addServletWithMapping(DeployApp.class, "/kstream/deployApp");
        servletHandler.addServletWithMapping(StartApp.class, "/kstream/startApp");
        servletHandler.addServletWithMapping(StopApp.class, "/kstream/stopApp");
        servletHandler.addServletWithMapping(GetApp.class, "/kstream/getApp");
        servletHandler.addServletWithMapping(GetAppSys.class, "/kstream/getAppSys");
        servletHandler.addServletWithMapping(GetAllAppSys.class, "/kstream/getAllAppSys");
        servletHandler.addFilterWithMapping(CrossOriginFilter.class, "/kstream/*", EnumSet.of(DispatcherType.REQUEST));
        this.server.setHandler(servletHandler);
        this.server.start();
        Files.write(bin_dir.resolve("pid"), ManagementFactory.getRuntimeMXBean()
                .getName().split("@")[0].getBytes("UTF-8"));
        logger.info("jetty server start and bind to port " + port);
        logger.info("init ks applications");
        this.initApp();
        logger.info("jetty server started...");
    }

    /**
     * 初始化app信息
     *
     * @throws IOException e
     */
    private void initApp() throws SQLException, IOException {
        List<Map<String, String>> list = mysqlOperator.query(
                "select service_id,service_name,service_desc,service_status from ksapp");
        for (Map<String, String> map : list) {
            String service_id = map.get("service_id");
            String service_name = map.get("service_name");
            AppInfo appInfo = new AppInfo();
            appInfo.setName(service_name);
            appInfo.setDesc(map.get("service_desc"));
            appInfo.setStatus(map.get("service_status"));
            if (appInfo.getStatus() == AppInfo.Status.RUN) {
                File pf = app_dir.resolve(service_id).resolve("pid").toFile();
                if (pf.exists()) {
                    String pid = com.google.common.io.Files.readFirstLine(pf, Charset.forName("UTF-8"));
                    appInfo.setPid(pid);
                } else {
                    logger.warn("服务 " + service_name + " pid 文件丢失,状态重置为stop");
                    appInfo.setStatus(AppInfo.Status.STOP);
                }
            }
            caches.put(service_id, appInfo);
        }
    }

    private void stop() {
        try {
            if (mysqlOperator != null) mysqlOperator.close();
            if (this.server != null) this.server.stop();
            Files.delete(bin_dir.resolve("pid"));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length < 1) {
            System.err.printf("command error...\n%s", "USAGE:KsServer start|stop");
            System.exit(-1);
        }
        if (!ImmutableList.of("start", "stop").contains(args[0])) {
            System.err.printf("param %s is not defined\n%s", args[1], "USAGE:KsServer start|stop");
            System.exit(-1);
        }
        Logger logger = LoggerFactory.getLogger(KsServer.class);
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            if ("start".equals(args[0])) {
                final KsServer ksServer = new KsServer();
                final RegularlyUpdate bst = new RegularlyUpdate();
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    ksServer.stop();
                    bst.interrupt();
                    latch.countDown();
                }));
                ksServer.start();
                bst.start();
                latch.await();
            } else {
                StopProcess.stop(bin_dir.resolve("pid"));
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            System.exit(-1);
        }
        System.exit(0);
    }
}
