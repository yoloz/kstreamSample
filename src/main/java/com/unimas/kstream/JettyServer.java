package com.unimas.kstream;

import com.google.common.collect.ImmutableList;
import com.unimas.kstream.bean.AppInfo;
import com.unimas.kstream.web.BackStatusThread;
import com.unimas.kstream.web.DeleteTask;
import com.unimas.kstream.web.GetGeneral;
import com.unimas.kstream.web.GetTasks;
import com.unimas.kstream.web.GetVersion;
import com.unimas.kstream.web.StartApp;
import com.unimas.kstream.web.StopApp;
import com.unimas.kstream.web.StoreApp;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * 跨域访问参见 http://www.eclipse.org/jetty/documentation/current/cross-origin-filter.html
 * web的package.json中配置proxy,jetty跨域可不用
 */
public class JettyServer {

    private static final Logger logger = LoggerFactory.getLogger(JettyServer.class);

    public enum Status {
        //运行,启动异常,停止,启动中
        RUN("run"), ODD("odd"), STOP("stop"), START("start");
        private String value;

        Status(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

    }

    private static final String root_dir = System.getProperty("ks.root.dir");
    public static final Path app_dir = Paths.get(root_dir, "app");
    public static final Path bin_dir = Paths.get(root_dir, "bin");
    public static final ConcurrentHashMap<String, AppInfo> caches = new ConcurrentHashMap<>();


    private final int port;
    private final Path web_dir;
    private Server server;

    private JettyServer() throws Exception {
        Path cf = Paths.get(root_dir, "config", "server.properties");
        web_dir = Paths.get(root_dir, "web");
        if (!web_dir.toFile().exists()) throw new Exception(web_dir + " is not exist!");
        if (cf.toFile().exists()) {
            Properties p = new Properties();
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(cf.toFile()), Charset.forName("UTF-8"))) {
                p.load(reader);
            }
            port = Integer.parseInt(p.getProperty("port"));
            if (Files.notExists(app_dir, LinkOption.NOFOLLOW_LINKS)) Files.createDirectory(app_dir);
        } else {
            throw new Exception(cf + " is not exist!");
        }

    }

    private void start() throws Exception {
        this.server = new Server(port);
        WebAppContext appContext = new WebAppContext();
        appContext.setResourceBase(web_dir.toString());
        appContext.addServlet(DefaultServlet.class, "/");
        appContext.addServlet(GetVersion.class, "/getVersion");
        appContext.addServlet(GetGeneral.class, "/getGeneral");
        appContext.addServlet(GetTasks.class, "/getTasks");
        appContext.addServlet(DeleteTask.class, "/deleteTask");
        appContext.addServlet(StoreApp.class, "/storeTask");
        appContext.addServlet(StartApp.class, "/startTask");
        appContext.addServlet(StopApp.class, "/stopTask");
//        appContext.addFilter(CrossOriginFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        this.server.setHandler(appContext);
        this.server.start();
        Files.write(web_dir.resolve("pid"), ManagementFactory.getRuntimeMXBean()
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
    private void initApp() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(app_dir)) {
            for (Path dir : stream) {
                if (Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
                    Properties properties = new Properties();
                    try (FileInputStream file = new FileInputStream(dir.resolve("main.properties")
                            .toString())) {
                        properties.load(file);
                    }
                    String app_id = properties.getProperty("application.id");
                    AppInfo appInfo = new AppInfo();
                    appInfo.setName(properties.getProperty("application.name"));
                    File pf = dir.resolve("pid").toFile();
                    if (pf.exists()) {
                        String pid = com.google.common.io.Files.readFirstLine(pf, Charset.forName("UTF-8"));
                        appInfo.setPid(pid);
                        appInfo.setStatus(Status.RUN);
                    }
                    caches.put(app_id, appInfo);
                }
            }
        }
    }

    private void stop() {
        try {
            if (this.server != null) this.server.stop();
            Files.delete(Paths.get(root_dir, "web").resolve("pid"));
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length < 1) {
            System.err.printf("command error...\n%s", "USAGE:JettyServer start|stop");
            System.exit(-1);
        }
        if (!ImmutableList.of("start", "stop").contains(args[0])) {
            System.err.printf("param %s is not defined\n%s", args[1], "USAGE:JettyServer start|stop");
            System.exit(-1);
        }
        Logger logger = LoggerFactory.getLogger(JettyServer.class);
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            if ("start".equals(args[0])) {
                final JettyServer jettyServer = new JettyServer();
                final BackStatusThread bst = new BackStatusThread();
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    jettyServer.stop();
                    bst.interrupt();
                    latch.countDown();
                }));
                jettyServer.start();
                bst.start();
                latch.await();
            } else {
                StopProcess.stop(Paths.get(root_dir, "web").resolve("pid"));
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            System.exit(-1);
        }
        System.exit(0);
    }
}
