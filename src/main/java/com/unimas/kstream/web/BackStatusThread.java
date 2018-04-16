package com.unimas.kstream.web;

import com.unimas.kstream.JettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 后台每1分钟检查一次caches的运行状态是否正常及添加运行信息
 */
public class BackStatusThread extends Thread {

    private final Logger logger = LoggerFactory.getLogger(BackStatusThread.class);

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
     * <p>
     * -b 顺序输出,而不是刷新输出
     * <p>
     * -n 刷新次数
     * <p>
     * -p 监视某个进程或某几个进程中间用逗号隔开
     */
    @Override
    public void run() {
        while (!isInterrupted()) {
            final StringBuilder pidBuilder = new StringBuilder();
            JettyServer.caches.forEach((id, app) -> {
                switch (app.getStatus()) {
                    case RUN:
                        if (!isExist(id)) {
                            logger.error(id + " status run but pid file is not exit,change status to stop");
                            app.setStatus(JettyServer.Status.STOP);
                            app.setPid("");
                        }
                        break;
                    default:
                        if (isExist(id)) {
                            logger.error(id + " status " + app.getStatus().getValue() +
                                    " but pid file is exit,change status to run");
                            app.setStatus(JettyServer.Status.RUN);
                        }
                }
                if (!app.getPid().isEmpty()) pidBuilder.append(app.getPid()).append(",");
            });
            this.updateAppInfo(pidBuilder.toString());
            try {
                Thread.sleep(60 * 1000);
            } catch (InterruptedException ignored) {
                break;
            }
        }
    }

    /**
     * [, pid, user, PR, NI, VIRT, RES, SHR, S, %CPU, %MEM, TIME+, COMMAND]
     * [pid, user, PR, NI, VIRT, RES, SHR, S, %CPU, %MEM, TIME+, COMMAND]
     * 更新 app 运行信息
     *
     * @param pids pid list
     */
    private void updateAppInfo(String pids) {
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
                JettyServer.caches.forEach((k, app) -> {
                    if (app.getPid().equals(tops[0])) {
                        app.setCpu(tops[8]);
                        app.setMem(tops[9]);
                    } else if (app.getPid().equals(tops[1])) {
                        app.setCpu(tops[9]);
                        app.setMem(tops[10]);
                    }
                });
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

    private boolean isExist(String app_id) {
        return JettyServer.app_dir.resolve(app_id).resolve("pid").toFile().exists();
    }
}
