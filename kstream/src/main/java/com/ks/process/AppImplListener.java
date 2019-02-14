package com.ks.process;

import com.ks.KsServer;
import org.apache.kafka.streams.KafkaStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * 监视kafkaStreams state error,然后重启.
 * 避免启动异常被处理，故等待一段时间才调度
 * 暂时存在如果JettyServer重启则会丢失独立的kafakStream 信息[可运行独立的中间进程管理器]；
 */
public class AppImplListener {

    private final Logger logger = LoggerFactory.getLogger(AppImplListener.class);


    private static class App {

        App(long createMill, AppImpl appImpl) {
            this.createMill = createMill;
            this.appImpl = appImpl;
        }

        private long createMill;
        private AppImpl appImpl;

        long getCreateMill() {
            return createMill;
        }

        AppImpl getAppImpl() {
            return appImpl;
        }

    }

    private static Map<String, App> apps = new ConcurrentHashMap<>(5);

    private ScheduledExecutorService scheduledService;
    private ScheduledFuture<?> scheduledFuture;

    public AppImplListener() {
        scheduledService = Executors.newScheduledThreadPool(1);
        scheduledFuture = scheduledService.scheduleWithFixedDelay(
                this::listener, 5, 60, TimeUnit.MINUTES);
    }

    public static void addApp(String mainPath, AppImpl appImpl) {
        App app = new App(System.currentTimeMillis(), appImpl);
        apps.put(mainPath, app);
    }

    public static void removeApp(String mainPath) {
        apps.remove(mainPath);
    }

    private void listener() {
        apps.forEach((k, v) -> {
            if (System.currentTimeMillis() - v.getCreateMill() > 600_000) //10 minutes
                try {
                    if (v.getAppImpl().kafkaStreams.state() == KafkaStreams.State.ERROR) {
                        logger.warn("任务[" + k + "] error,停止任务并重启");
                        KsServer.main(new String[]{k, "stop"});
                        Thread.sleep(10_000);
                        KsServer.main(new String[]{k, "start"});
                    }
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                }
        });
    }

    public void close() {
        if (scheduledFuture != null) scheduledFuture.cancel(true);
        if (scheduledService != null) {
            scheduledService.shutdown();
            try {
                scheduledService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {//ignore
            }
            scheduledService.shutdownNow();
        }
    }
}
