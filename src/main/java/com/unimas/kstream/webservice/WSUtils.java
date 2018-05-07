package com.unimas.kstream.webservice;

import com.unimas.kstream.KsServer;
import com.unimas.kstream.bean.AppInfo;
import com.unimas.kstream.bean.ServiceInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

public class WSUtils {

    /**
     * 读取inputstream内容
     *
     * @param input {@link InputStream}
     * @return string {@link String}
     */
    public static String readInputStream(InputStream input) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
            String tmp;
            while ((tmp = buffer.readLine()) != null) {
                body.append(tmp);
            }
        }
        return body.toString();
    }


    public static class EmptyDir extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    }

    public static void updateCacheStatus(String app_id, AppInfo.Status status) {
        for (ServiceInfo serviceInfo : KsServer.caches.values()) {
            Map<String, AppInfo> appInfoMap = serviceInfo.getAppInfoMap();
            if (appInfoMap.containsKey(app_id)) {
                appInfoMap.get(app_id).setStatus(status);
                break;
            }
        }
    }

    /**
     * @param ids service_id[app_id]
     * @return error msg
     */
    public static String unModify(String... ids) {
        if (ids == null || ids.length == 0) return null;
        String service_id = ids[0];
        if (service_id != null) {
            if (KsServer.caches.containsKey(service_id)) {
                ServiceInfo serviceInfo = KsServer.caches.get(service_id);
                Map<String, AppInfo> appInfoMap = serviceInfo.getAppInfoMap();
                if (appInfoMap == null) return null;
                if (ids.length == 2) {
                    String app_id = ids[1];
                    AppInfo appInfo = appInfoMap.get(app_id);
                    if (appInfo != null) {
                        AppInfo.Status status = appInfo.getStatus();
                        if (status == AppInfo.Status.START || status == AppInfo.Status.RUN) {
                            return appInfo.getName() + " 运行或启动中,当前操作拒绝执行!";
                        }
                    } else return "服务id " + service_id + "下的任务id" + app_id + "不存在!";
                } else {
                    for (String app_id : appInfoMap.keySet()) {
                        AppInfo appInfo = appInfoMap.get(app_id);
                        AppInfo.Status status = appInfo.getStatus();
                        if (status == AppInfo.Status.START || status == AppInfo.Status.RUN) {
                            return appInfo.getName() + " 运行或启动中,当前操作拒绝执行!";
                        }
                    }
                }
            }
        } else { //app_id can not be null
            String app_id = ids[1];
            for (ServiceInfo serviceInfo : KsServer.caches.values()) {
                Map<String, AppInfo> appInfoMap = serviceInfo.getAppInfoMap();
                if (appInfoMap == null) return null;
                if (appInfoMap.containsKey(app_id)) {
                    AppInfo appInfo = appInfoMap.get(app_id);
                    AppInfo.Status status = appInfo.getStatus();
                    if (status == AppInfo.Status.START || status == AppInfo.Status.RUN) {
                        return appInfo.getName() + " 运行或启动中,当前操作拒绝执行!";
                    }
                }
            }
        }
        return null;
    }


    public static String unStart(String app_id) {
        if (app_id == null) return null;
        for (ServiceInfo serviceInfo : KsServer.caches.values()) {
            Map<String, AppInfo> appInfoMap = serviceInfo.getAppInfoMap();
            if (appInfoMap != null && appInfoMap.containsKey(app_id)) {
                AppInfo appInfo = serviceInfo.getAppInfoMap().get(app_id);
                AppInfo.Status status = appInfo.getStatus();
                if (status == AppInfo.Status.INIT || status == AppInfo.Status.ODD) {
                    return appInfo.getName() + " 未部署或启动异常,请检查确认!";
                }
            }
        }
        return null;
    }
}
