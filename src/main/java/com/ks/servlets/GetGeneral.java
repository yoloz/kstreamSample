package com.ks.servlets;

import com.ks.JettyServer;
import com.ks.bean.AppInfo;
import com.ks.bean.KJson;
import com.ks.bean.MutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link StoreApp 入口文件统一命名:main.properties}
 */
public class GetGeneral extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(GetGeneral.class);

    /**
     * Called by the server (via the <code>service</code> method) to
     * allow a servlet to handle a GET request.
     *
     * <p>Overriding this method to support a GET request also
     * automatically supports an HTTP HEAD request. A HEAD
     * request is a GET request that returns no body in the
     * response, only the request header fields.
     *
     * <p>When overriding this method, read the request data,
     * write the response headers, get the response's writer or
     * output stream object, and finally, write the response data.
     * It's best to include content type and encoding. When using
     * a <code>PrintWriter</code> object to return the response,
     * set the content type before accessing the
     * <code>PrintWriter</code> object.
     *
     * <p>The servlet container must write the headers before
     * committing the response, because in HTTP the headers must be sent
     * before the response body.
     *
     * <p>Where possible, set the Content-Length header (with the
     * {@link ServletResponse#setContentLength} method),
     * to allow the servlet container to use a persistent connection
     * to return its response to the client, improving performance.
     * The content length is automatically set if the entire response fits
     * inside the response buffer.
     *
     * <p>When using HTTP 1.1 chunked encoding (which means that the response
     * has a Transfer-Encoding header), do not set the Content-Length header.
     *
     * <p>The GET method should be safe, that is, without
     * any side effects for which users are held responsible.
     * For example, most form queries have no side effects.
     * If a client request is intended to change stored data,
     * the request should use some other HTTP method.
     *
     * <p>The GET method should also be idempotent, meaning
     * that it can be safely repeated. Sometimes making a
     * method safe also makes it idempotent. For example,
     * repeating queries is both safe and idempotent, but
     * buying a product online or modifying data is neither
     * safe nor idempotent.
     *
     * <p>If the request is incorrectly formatted, <code>doGet</code>
     * returns an HTTP "Bad Request" message.
     *
     * @param req  an {@link HttpServletRequest} object that
     *             contains the request the client has made
     *             of the servlet
     * @param resp an {@link HttpServletResponse} object that
     *             contains the response the servlet sends
     *             to the client
     * @throws IOException if an input or output error is
     *                     detected when the servlet handles
     *                     the GET request
     * @see ServletResponse#setContentType
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> result = new HashMap<>(1);
        final List<Map<String, Object>> list = new ArrayList<>(JettyServer.caches.size());
        for (Map.Entry<String, AppInfo> entry : JettyServer.caches.entrySet()) {
            String id = entry.getKey();
            AppInfo app = entry.getValue();
            Map<String, Object> map = new HashMap<>();
            map.put("application_id", id);
            map.put("application_name", app.getName());
            File pf = JettyServer.app_dir.resolve(id).resolve("pid").toFile();
            if (app.getStatus() == AppInfo.Status.START) {
                if (pf.exists()) {
                    logger.info(id + " start success,change status to running");
                    app.setStatus(AppInfo.Status.RUN);
                }
            }
            if (app.getStatus() == AppInfo.Status.RUN) {
                if (!pf.exists()) {
                    logger.error(id + " status run but pid file is not exit,change status to stop");
                    app.setStatus(AppInfo.Status.STOP);
                    app.setPid("");
                }
            }
            map.put("application_status", app.getStatus().getValue());
            if (app.getStatus() == AppInfo.Status.START) {
                map.put("application_cpu", "0");
                map.put("application_mem", "0");
                map.put("application_time", "0");
            } else if (app.getStatus() == AppInfo.Status.RUN) {
                String pid = app.getPid();
                if (pid.isEmpty()) {//点击运行
                    app.setPid(com.google.common.io.Files.readFirstLine(pf, Charset.forName("UTF-8")));
                    MutablePair<String, String> sys = this.getSysInfo(app.getPid());
                    app.setCpu(sys.getLeft());
                    app.setMem(sys.getRight());
                }
                app.setRuntime(getRunTime(pf.toPath()));
                map.put("application_cpu", app.getCpu());
                map.put("application_mem", app.getMem());
                map.put("application_time", app.getRuntime());
            } else {
                map.put("application_cpu", "—");
                map.put("application_mem", "—");
                map.put("application_time", "—");
            }
            list.add(map);
        }
        result.put("success", true);
//        result.put("results", testData());
        result.put("results", list);
        String str = KJson.writeValueAsString(result);
        OutputStream outputStream = resp.getOutputStream();
        outputStream.write(str.getBytes("utf-8"));
    }


    /**
     * 获取运行时间
     *
     * @param pf pid file
     * @return string time
     */
    private String getRunTime(Path pf) {
        try {
            FileTime fileTime = (FileTime) Files.getAttribute(pf, "lastModifiedTime");
            OffsetDateTime ft = OffsetDateTime.ofInstant(fileTime.toInstant(), ZoneOffset.systemDefault());
            OffsetDateTime now = OffsetDateTime.now();
            long mill = now.toInstant().toEpochMilli() - ft.toInstant().toEpochMilli();
            long day = mill / (24 * 3600 * 1000);
            long hour = (mill - (day * 24 * 3600 * 1000)) / (3600 * 1000);
            long min = (mill - (day * 24 * 3600 * 1000) - (hour * 3600 * 1000)) / (60 * 1000);
            long sec = (mill - (day * 24 * 3600 * 1000) - (hour * 3600 * 1000) - (min * 60 * 1000)) / 1000;
            StringBuilder builder = new StringBuilder();
            if (day > 0) builder.append(day).append("d");
            if (hour > 0) builder.append(hour).append("h");
            if (min > 0) builder.append(min).append("m");
            if (sec > 0) builder.append(sec).append("s");
            return builder.toString();
        } catch (IOException e) {
            logger.error("fail to get run time", e);
        }
        return "—";
    }

    /**
     * 根据pid获取cpu,mem使用率
     * 限于linux
     * [, pid, user, PR, NI, VIRT, RES, SHR, S, %CPU, %MEM, TIME+, COMMAND]
     * [pid, user, PR, NI, VIRT, RES, SHR, S, %CPU, %MEM, TIME+, COMMAND]
     * <p>
     * -b 顺序输出,而不是刷新输出
     * <p>
     * -n 刷新次数
     * <p>
     * -p 监视某个进程或某几个进程中间用逗号隔开
     *
     * @param pid pid
     * @return cpu, mem
     */
    private MutablePair<String, String> getSysInfo(String pid) {
        if (pid == null || pid.isEmpty()) return MutablePair.of("—", "—");
        String[] cmd = {
                "/bin/sh",
                "-c",
                "top -b -n 1 -p " + pid + " |grep java"
        };
        BufferedReader buffR = null;
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            buffR = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = buffR.readLine();
            if (line != null && !line.isEmpty()) {
                String[] tops = line.split("\\s+");
                if (tops.length == 13) return MutablePair.of(tops[9], tops[10]);
                else return MutablePair.of(tops[8], tops[9]);
            }
        } catch (IOException e) {
            logger.error("fail to get " + pid + " info", e);
        } finally {
            try {
                if (buffR != null) buffR.close();
            } catch (IOException ignored) {
            }
        }
        return MutablePair.of("—", "—");
    }

    private List<Map<String, Object>> testData() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("application_id", i);
            data.put("application_name", "test" + i);
            data.put("application_status", i % 2 == 0 ? "run" : "");
            data.put("application_cpu", 100 * Math.random());
            data.put("application_mem", 100 * Math.random());
            list.add(data);
        }
        return list;
    }
}
