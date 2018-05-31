package com.unimas.kska;

import com.google.common.collect.ImmutableList;
import com.unimas.kska.bean.AppInfo;
import com.unimas.kska.bean.KJson;
import com.unimas.kska.bean.ServiceInfo;
import com.unimas.kska.error.KRunException;
import com.unimas.kska.kafka.KaJMX;
import com.unimas.kska.kafka.KskaClient;
import com.unimas.kska.webservice.WSUtils;
import com.unimas.kska.webservice.impl.ds.DeleteDS;
import com.unimas.kska.webservice.impl.ds.GetDS;
import com.unimas.kska.webservice.impl.ka.CreateTopic;
import com.unimas.kska.webservice.impl.ka.DeleteTopic;
import com.unimas.kska.webservice.impl.ka.GetAddr;
import com.unimas.kska.webservice.impl.ka.GetLocalIp;
import com.unimas.kska.webservice.impl.ks.GetAppTopic;
import com.unimas.kska.webservice.impl.ks.GetFields;
import com.unimas.kska.webservice.impl.ks.UDFile;
import com.unimas.kska.webservice.RegularlyUpdate;
import com.unimas.kska.webservice.MysqlOperator;
import com.unimas.kska.webservice.impl.ds.FilterName;
import com.unimas.kska.webservice.impl.ds.GetAllDS;
import com.unimas.kska.webservice.impl.ds.GetMpDS;
import com.unimas.kska.webservice.impl.ds.StoreSource;
import com.unimas.kska.webservice.impl.ka.GetAllTopic;
import com.unimas.kska.webservice.impl.ka.GetTopic;
import com.unimas.kska.webservice.impl.ka.LogEndOffset;
import com.unimas.kska.webservice.impl.ka.SetAddr;
import com.unimas.kska.webservice.impl.ks.DeleteApp;
import com.unimas.kska.webservice.impl.ks.DeleteService;
import com.unimas.kska.webservice.impl.ks.DeployApp;
import com.unimas.kska.webservice.impl.ks.GetAllApp;
import com.unimas.kska.webservice.impl.ks.GetAllService;
import com.unimas.kska.webservice.impl.ks.GetAppConf;
import com.unimas.kska.webservice.impl.ks.GetTopics;
import com.unimas.kska.webservice.impl.ks.OrderApp;
import com.unimas.kska.webservice.impl.ks.StartApp;
import com.unimas.kska.webservice.impl.ks.StopApp;
import com.unimas.kska.webservice.impl.ks.StoreApp;
import com.unimas.kska.webservice.impl.ks.StoreService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
    public static final Path upload_dir = Paths.get(root_dir, "upload");
    public static final Path download_dir = Paths.get(root_dir, "download");
    public static final ConcurrentHashMap<String, ServiceInfo> caches = new ConcurrentHashMap<>();

    public static String wfUrl;

    private static MysqlOperator mysqlOperator = null;
    private static KaJMX kaJMX = null;
    private static KskaClient ksKaClient = null;
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
            wfUrl = p.getProperty("wf.url", "http://localhost:7914");
            mysqlOperator = new MysqlOperator(p);
            if (Files.notExists(app_dir, LinkOption.NOFOLLOW_LINKS)) Files.createDirectory(app_dir);
        } else {
            throw new Exception(cf + " is not exist!");
        }
    }

    private void start() throws Exception {
        this.server = new Server(port);
        ServletHandler servletHandler = new ServletHandler();
        servletHandler.addServletWithMapping(StoreService.class, "/cii/ks/storeService");
        servletHandler.addServletWithMapping(DeleteService.class, "/cii/ks/deleteService");
        servletHandler.addServletWithMapping(GetAllService.class, "/cii/ks/getAllService");
        servletHandler.addServletWithMapping(OrderApp.class, "/cii/ks/orderApp");
        servletHandler.addServletWithMapping(StoreApp.class, "/cii/ks/storeApp");
        servletHandler.addServletWithMapping(DeleteApp.class, "/cii/ks/deleteApp");
        servletHandler.addServletWithMapping(DeployApp.class, "/cii/ks/deployApp");
        servletHandler.addServletWithMapping(StartApp.class, "/cii/ks/startApp");
        servletHandler.addServletWithMapping(StopApp.class, "/cii/ks/stopApp");
        servletHandler.addServletWithMapping(GetAppConf.class, "/cii/ks/getAppConf");
        servletHandler.addServletWithMapping(GetAllApp.class, "/cii/ks/getAllApp");
        servletHandler.addServletWithMapping(GetTopics.class, "/cii/ks/getTopics");
        servletHandler.addServletWithMapping(UDFile.class, "/cii/ks/uDFile");
        servletHandler.addServletWithMapping(GetFields.class, "/cii/ks/getFields");
        servletHandler.addServletWithMapping(GetAppTopic.class, "/cii/ks/getAppTopic");

        servletHandler.addServletWithMapping(SetAddr.class, "/cii/ka/setAddr");
        servletHandler.addServletWithMapping(GetAddr.class, "/cii/ka/getAddr");
        servletHandler.addServletWithMapping(GetAllTopic.class, "/cii/ka/getAllTopic");
        servletHandler.addServletWithMapping(GetTopic.class, "/cii/ka/getTopic");
        servletHandler.addServletWithMapping(LogEndOffset.class, "/cii/ka/logEndOffset");
        servletHandler.addServletWithMapping(GetLocalIp.class, "/cii/ka/getLocalIp");
        servletHandler.addServletWithMapping(CreateTopic.class, "/cii/ka/createTopic");
        servletHandler.addServletWithMapping(DeleteTopic.class, "/cii/ka/deleteTopic");

        servletHandler.addServletWithMapping(GetAllDS.class, "/cii/ds/getAllDS");
        servletHandler.addServletWithMapping(GetMpDS.class, "/cii/ds/getMpDS");
        servletHandler.addServletWithMapping(FilterName.class, "/cii/ds/filterName");
        servletHandler.addServletWithMapping(StoreSource.class, "/cii/ds/storeSource");
        servletHandler.addServletWithMapping(DeleteDS.class, "/cii/ds/deleteDS");
        servletHandler.addServletWithMapping(GetDS.class, "/cii/ds/getDS");

        servletHandler.addFilterWithMapping(CrossOriginFilter.class, "/cii/*", EnumSet.of(DispatcherType.REQUEST));
        this.server.setHandler(servletHandler);
        this.server.start();
        Files.write(bin_dir.resolve("pid"), ManagementFactory.getRuntimeMXBean()
                .getName().split("@")[0].getBytes("UTF-8"));
        logger.info("jetty server start and bind to port " + port);
        logger.info("init ks applications");
        this.initApp();
        logger.info("init zk and jmx addr");
        this.initKaAddr();
        logger.info("jetty server started...");
    }

    private void initKaAddr() {
        try {
            List<Map<String, String>> list = mysqlOperator.query(
                    "select ds_json from ciisource where ds_id=?", "11111111111111");
            if (!list.isEmpty()) {
                Map<String, String> _m = list.get(0);
                if (_m != null && _m.containsKey("ds_json")) {
                    Map<String, String> ds_json = KJson.readStringValue(_m.get("ds_json"));
                    if (ds_json.containsKey("zk_url") && !ds_json.get("zk_url").isEmpty())
                        ksKaClient = new KskaClient(ds_json.get("zk_url"));
                    if (ds_json.containsKey("jmx_url") && !ds_json.get("jmx_url").isEmpty())
                        kaJMX = new KaJMX(ds_json.get("jmx_url"));
                } else logger.error("查询出的平台KAFKA数据错误!");

            }
        } catch (SQLException | IOException e) {
            logger.error("初始化平台KAFKA地址出错", e);
        }
    }

    /**
     * 初始化app信息
     *
     * @throws IOException e
     */
    private void initApp() throws SQLException, IOException {
        List<Map<String, String>> list = mysqlOperator.query(
                "select service_id,service_name,service_desc from ksservice");
        for (Map<String, String> services : list) {
            ServiceInfo serviceInfo = new ServiceInfo();
            String service_id = services.get("service_id");
            List<Map<String, String>> applist = mysqlOperator.query(
                    "select app_id,app_name,app_desc,app_status,zk_url from ksapp where service_id=?",
                    service_id);
            serviceInfo.setName(services.get("service_name"));
            serviceInfo.setDesc(services.get("service_desc"));
            for (Map<String, String> apps : applist) {
                AppInfo appInfo = new AppInfo();
                String app_id = apps.get("app_id");
                appInfo.setId(app_id);
                String app_name = apps.get("app_name");
                appInfo.setName(app_name);
                appInfo.setDesc(apps.get("app_desc"));
                appInfo.setStatus(Integer.parseInt(apps.get("app_status")));
                appInfo.setZkUrl(apps.get("zk_url"));
                if (appInfo.getStatus() == AppInfo.Status.RUN) {
                    File pf = app_dir.resolve(app_id).resolve("pid").toFile();
                    if (pf.exists()) {
                        String pid = com.google.common.io.Files.readFirstLine(pf, Charset.forName("UTF-8"));
                        appInfo.setPid(pid);
                    } else {
                        logger.warn("任务 " + app_name + " pid 文件丢失,状态重置为stop");
                        WSUtils.updateMysqlStatus(app_id, AppInfo.Status.STOP);
                        appInfo.setStatus(AppInfo.Status.STOP);
                    }
                }
                serviceInfo.addAppInfo(appInfo);
            }
            caches.put(service_id, serviceInfo);
        }
    }

    private void stop() {
        try {
            if (mysqlOperator != null) mysqlOperator.close();
            if (kaJMX != null) kaJMX.close();
            if (ksKaClient != null) ksKaClient.close();
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


    //======================================
    //**************************************
    //======================================
    public static MysqlOperator getMysqlOperator() {
        if (mysqlOperator == null) throw new KRunException("数据库连接为空");
        return mysqlOperator;
    }

    public static KaJMX getKaJMX() {
        if (kaJMX == null) throw new KRunException("jmx连接为空,请重新保存平台kafka");
        return kaJMX;
    }


    public static KskaClient getKsKaClient() {
        if (ksKaClient == null) throw new KRunException("zookeeper连接为空,请重新保存平台kafka");
        return ksKaClient;
    }

    public static void setKaJMX(String url) throws IOException {
        if (kaJMX != null) kaJMX.close();
        kaJMX = new KaJMX(url);
    }

    public static void setKsKaClient(String url) {
        if (ksKaClient != null) ksKaClient.close();
        ksKaClient = new KskaClient(url);
    }

    public static void overWrite(String zkUrl, String jmxUrl) throws IOException {
        Path cf = Paths.get(root_dir, "config", "server.properties");
        if (cf.toFile().exists()) {
            Properties p = new Properties();
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(cf.toFile()), Charset.forName("UTF-8"))) {
                p.load(reader);
            }
            p.put("zk.url", zkUrl);
            p.put("jmx.url", jmxUrl);
            try (FileOutputStream output = new FileOutputStream(cf.toFile())) {
                p.store(output, null);
            }
        }
    }
}
