package com.unimas.kstream.webservice;

import com.google.common.io.Files;
import com.unimas.kstream.KsServer;
import com.unimas.kstream.bean.AppInfo;
import com.unimas.kstream.bean.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 后台每1分钟检查一次caches的运行状态是否正常及更新运行信息
 */
public class RegularlyUpdate extends Thread {

    private final Logger logger = LoggerFactory.getLogger(RegularlyUpdate.class);

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
        while (!isInterrupted()) {
            final StringBuilder pidBuilder = new StringBuilder();
            for (ServiceInfo s : KsServer.caches.values()) {
                if (s.getAppInfoMap() != null) s.getAppInfoMap().forEach((id, app) -> {
                            File pf = KsServer.app_dir.resolve(id).resolve("pid").toFile();
                            switch (app.getStatus()) {
                                case RUN:
                                    if (!pf.exists()) {
                                        logger.warn(id + " status run but pid file is not exit,change status to stop");
                                        app.setStatus(AppInfo.Status.STOP);
                                        app.setPid("");
                                        app.setRuntime("—");
                                    } else app.setRuntime(getRunTime(id));
                                    break;
                                default:
                                    if (pf.exists()) {
                                        logger.warn(id + " status " + app.getStatus().getValue() +
                                                " but pid file is exit,change status to run");
                                        app.setStatus(AppInfo.Status.RUN);
                                        try {
                                            app.setPid(Files.readFirstLine(pf, Charset.forName("UTF-8")));
                                        } catch (IOException e) {
                                            logger.error("读取文件:" + pf.toString() + "失败", e);
                                        }
                                        app.setRuntime(getRunTime(id));
                                    } else app.setRuntime("—");
                            }
                            if (!app.getPid().isEmpty()) pidBuilder.append(app.getPid()).append(",");
                        }
                );
            }
            this.updateSysInfo(pidBuilder.toString());
            try {
                Thread.sleep(60 * 1000);
            } catch (InterruptedException ignored) {
                break;
            }
        }
    }

    /**
     * 更新 app 运行信息
     * [, pid, user, PR, NI, VIRT, RES, SHR, S, %CPU, %MEM, TIME+, COMMAND]
     * [pid, user, PR, NI, VIRT, RES, SHR, S, %CPU, %MEM, TIME+, COMMAND]
     * <p>
     * -b 顺序输出,而不是刷新输出
     * <p>
     * -n 刷新次数
     * <p>
     * -p 监视某个进程或某几个进程中间用逗号隔开
     *
     * @param pids pid list
     */
    private void updateSysInfo(String pids) {
        if (pids.isEmpty()) return;
        String[] cmd = {
                "/bin/sh",
                "-c",
                "top -b -n 1 -p " + pids.substring(0, pids.length() - 1)
                        + " |grep java"
        };
        BufferedReader buffR = null;
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            buffR = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = buffR.readLine()) != null) {
                String[] tops = line.split("\\s+");
                KsServer.caches.values().forEach(s -> s.getAppInfoMap().forEach((k, app) -> {
                    if (app.getPid().equals(tops[0])) {
                        app.setCpu(tops[8]);
                        app.setMem(tops[9]);
                    } else if (app.getPid().equals(tops[1])) {
                        app.setCpu(tops[9]);
                        app.setMem(tops[10]);
                    }
                }));
            }
        } catch (IOException e) {
            logger.error("fail to get " + pids + " info", e);
        } finally {
            try {
                if (buffR != null) buffR.close();
            } catch (IOException ignored) {
            }
        }
    }

    private String getRunTime(String app_id) {
        final int oneDaySec = 24 * 3600;
        Path pf = KsServer.app_dir.resolve(app_id).resolve("pid");
        if (!pf.toFile().exists()) return "—";
        try {
            FileTime fileTime = (FileTime) java.nio.file.Files.getAttribute(pf, "lastModifiedTime");
            OffsetDateTime ft = OffsetDateTime.ofInstant(fileTime.toInstant(), ZoneOffset.systemDefault());
            OffsetDateTime now = OffsetDateTime.now();
            long differ = (now.toInstant().toEpochMilli() - ft.toInstant().toEpochMilli()) / 1000;
            long day = differ / oneDaySec;
            long hour = (differ - (day * oneDaySec)) / 3600;
            long min = (differ - (day * oneDaySec) - (hour * 3600)) / 60;
            long sec = differ - (day * oneDaySec) - (hour * 3600) - (min * 60);
            StringBuilder builder = new StringBuilder();
            if (day > 0) builder.append(day).append("d");
            if (hour > 0) builder.append(hour).append("h");
            if (min > 0) builder.append(min).append("m");
            if (sec > 0) builder.append(sec).append("s");
            if (builder.length() == 0) return "—";
            else return builder.toString();
        } catch (IOException e) {
            return "—";
        }
    }
}
