package com.unimas.kstream.web;

import com.unimas.kstream.JettyServer;
import com.unimas.kstream.bean.AppInfo;
import com.unimas.kstream.bean.KJson;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * {@link StoreApp 入口文件统一命名:main.properties}
 */
public class GetTasks extends HttpServlet {
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
        result.put("success", true);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, AppInfo> entry : JettyServer.caches.entrySet()) {
            Path dir = JettyServer.app_dir.resolve(entry.getKey());
            if (Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
                Map<String, Object> map = new HashMap<>();
                Properties mainProp = new Properties();
                try (FileInputStream file = new FileInputStream(dir.resolve("main.properties")
                        .toString())) {
                    mainProp.load(file);
                }
                Map<String, Object> main = this.getJObj(mainProp);
                map.put("main", main);
                String[] sources = String.valueOf(main.get("ks_source")).split(",");
                List<Map<String, Object>> sl = new ArrayList<>(sources.length);
                for (String source : sources) {
                    Map<String, Object> sm = this.getJObj(dir.resolve(source));
                    sl.add(sm);
                }
                map.put("sources", sl);
                String[] operations = String.valueOf(main.get("ks_operation")).split(",");
                List<Map<String, Object>> ol = new ArrayList<>(operations.length);
                for (String operation : operations) {
                    if (operation.isEmpty()) continue;
                    Map<String, Object> om = this.getJObj(dir.resolve(operation));
                    ol.add(om);
                }
                map.put("operations", ol);
                Map<String, Object> output = this.getJObj(dir.resolve(String.valueOf(main.get("ks_output"))));
                map.put("output", output);
                list.add(map);
            }
        }
        result.put("results", list);
//        result.put("results", testData());
        String str = KJson.writeValueAsString(result);
        OutputStream outputStream = resp.getOutputStream();
        outputStream.write(str.getBytes("utf-8"));
    }

    /**
     * 获取单个配置文件内容
     *
     * @param file file path
     * @return json object
     * @throws IOException e
     */
    private Map<String, Object> getJObj(Path file) throws IOException {
        Map<String, Object> result = new HashMap<>(1);
        try (FileInputStream inputStream = new FileInputStream(file.toString() + ".properties")) {
            Properties properties = new Properties();
            properties.load(inputStream);
            properties.keySet().forEach(k -> result.put(((String) k).replaceAll("\\.", "_"), properties.get(k)));
        }
        return result;
    }

    /**
     * 获取单个配置文件内容
     *
     * @param properties file content
     * @return json object
     */
    private Map<String, Object> getJObj(Properties properties) {
        Map<String, Object> result = new HashMap<>(1);
        properties.keySet().forEach(k -> result.put(((String) k).replaceAll("\\.", "_"), properties.get(k)));
        return result;
    }

    private List<Map<String, Object>> testData() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> data = new HashMap<>();
            Map<String, Object> main = new HashMap<>();
            data.put("main", main);
            data.put("sources", new ArrayList());
            data.put("operations", new ArrayList());
            data.put("output", new HashMap<>());
            list.add(data);
        }
        return list;
    }
}
