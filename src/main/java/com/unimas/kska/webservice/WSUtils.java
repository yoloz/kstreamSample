package com.unimas.kska.webservice;

import com.unimas.kska.KsServer;
import com.unimas.kska.bean.AppInfo;
import com.unimas.kska.bean.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class WSUtils {

    private static final Logger logger = LoggerFactory.getLogger(WSUtils.class);

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
            if (appInfoMap != null && appInfoMap.containsKey(app_id)) {
                appInfoMap.get(app_id).setStatus(status);
                break;
            }
        }
    }

    public static void updateMysqlStatus(String app_id, AppInfo.Status status) {
        try {
            KsServer.getMysqlOperator().fixUpdate("update ksapp set app_status=? where app_id=?",
                    status.getType(), app_id);
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
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

    public static void zipAction(String action, Path... paths) throws IOException {
        FileSystemProvider provider = null;
        for (FileSystemProvider p : FileSystemProvider.installedProviders()) {
            if ("jar".equals(p.getScheme())) {
                provider = p;
                break;
            }
        }
        if (provider == null) throw new IOException("ZIP filesystem provider is not installed");
        Map<String, String> env = new HashMap<>(1);
        if ("compress".equals(action)) env.put("create", "true");
        try (FileSystem fs = provider.newFileSystem(paths[0], env)) {
            if ("extract".equals(action)) {
                extract(fs, "/");
            } else {
                for (int i = 1; i < paths.length; i++) compress(fs, paths[i]);
            }
        }
    }

    private static void extract(FileSystem fs, String path) throws IOException {
        Path src = fs.getPath(path);
        if (Files.isDirectory(src)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(src)) {
                for (Path child : ds) extract(fs, child.toString());
            }
        } else {
            if (path.startsWith("/")) path = path.substring(1);
            Path dst = KsServer.app_dir.resolve(path);
            Path parent = dst.getParent();
            if (parent != null && Files.notExists(parent)) Files.createDirectories(parent);
            Files.copy(src, dst, REPLACE_EXISTING);
        }
    }

    private static void compress(FileSystem fs, Path src) throws IOException {
        if (Files.isDirectory(src)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(src)) {
                for (Path child : ds) compress(fs, child);
            }
        } else {
            String relative = src.toString().replaceFirst(KsServer.app_dir.toString(), "");
            Path dst = fs.getPath(relative);
            Path parent = dst.getParent();
            if (parent != null && Files.notExists(parent)) Files.createDirectories(parent);
            Files.copy(src, dst, REPLACE_EXISTING);
        }
    }


    public static void main(String[] args) throws IOException {
//        zipAction("compress",
//                Paths.get("/home/ylzhang/projects/kstream/app/app.zip"),
//                Paths.get("/home/ylzhang/projects/kstream/app/bin"),
//                Paths.get("/home/ylzhang/projects/kstream/app/config"));
        zipAction("extract", Paths.get("/home/ylzhang/projects/kstream/app/app.zip"));
    }
}
