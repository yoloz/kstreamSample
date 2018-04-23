package com.unimas.kstream.webservice;

import com.unimas.kstream.KsServer;
import com.unimas.kstream.bean.AppInfo;
import com.unimas.kstream.bean.MutablePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

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

    public static synchronized String getUid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    //service_id,service_name,service_desc
    public static void initCacheStatus(String... param) {
        if (param.length == 1) KsServer.caches.get(param[0]).setStatus(AppInfo.Status.INIT);
        else {
//            assert param.length == 3;
            if (KsServer.caches.containsKey(param[0])) {
                AppInfo appInfo = KsServer.caches.get(param[0]);
                appInfo.setName(param[1]);
                appInfo.setDesc(param[2]);
                appInfo.setStatus(AppInfo.Status.INIT);
            } else {
                AppInfo appInfo = new AppInfo();
                appInfo.setName(param[1]);
                appInfo.setDesc(param[2]);
                KsServer.caches.put(param[0], appInfo);
            }
        }
    }

    public static String unModify(String service_id) {
        if (service_id == null) return null;
        if (KsServer.caches.containsKey(service_id)) {
            AppInfo.Status status = KsServer.caches.get(service_id).getStatus();
            if (status == AppInfo.Status.START || status == AppInfo.Status.RUN) {
                return KsServer.caches.get(service_id).getName() + " 运行或启动中,当前操作拒绝执行!";
            }
        }
        return null;
    }

    public static String unStart(String service_id) {
        if (service_id == null) return null;
        if (KsServer.caches.containsKey(service_id)) {
            AppInfo.Status status = KsServer.caches.get(service_id).getStatus();
            if (status == AppInfo.Status.INIT || status == AppInfo.Status.ODD) {
                return KsServer.caches.get(service_id).getName() + " 未部署或启动异常,请检查确认!";
            }
        }
        return null;
    }

    public static String getRunTime(String service_id) {
        Path pf = KsServer.app_dir.resolve(service_id).resolve("pid");
        if (!pf.toFile().exists()) return "—";
        try {
            FileTime fileTime = (FileTime) Files.getAttribute(pf, "lastModifiedTime");
            OffsetDateTime ft = OffsetDateTime.ofInstant(fileTime.toInstant(), ZoneOffset.systemDefault());
            OffsetDateTime now = OffsetDateTime.now();
            long differ = (now.toInstant().toEpochMilli() - ft.toInstant().toEpochMilli()) / 1000;
            long day = differ / (24 * 3600);
            long hour = (differ - (day * 24 * 3600)) / 3600;
            long min = (differ - (day * 24 * 3600) - (hour * 3600)) / 60;
            long sec = differ - (day * 24 * 3600) - (hour * 3600) - (min * 60);
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

    public static MutablePair<String, String> getSysInfo(String pid) throws IOException {
        if (pid == null || pid.isEmpty()) return MutablePair.of("—", "—");
        String[] cmd = {
                "/bin/sh",
                "-c",
                "top -b -n 1 -p " + pid + " |grep java"
        };
        Process process = Runtime.getRuntime().exec(cmd);
        try (BufferedReader buffR = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = buffR.readLine();
            if (line != null && !line.isEmpty()) {
                String[] tops = line.split("\\s+");
                if (tops.length == 13) return MutablePair.of(tops[9], tops[10]);
                else return MutablePair.of(tops[8], tops[9]);
            }
        }
        return MutablePair.of("—", "—");
    }
}
