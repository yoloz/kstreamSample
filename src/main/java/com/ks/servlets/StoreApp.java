package com.ks.servlets;

import com.ks.JettyServer;
import com.ks.bean.AppInfo;
import com.ks.bean.KJson;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 应用目录名:application_id
 * 入口文件统一命名:main.properties
 * 输出文件统一命名:output.properties
 */
public class StoreApp extends HttpServlet {

    /**
     * Called by the server (via the <code>service</code> method)
     * to allow a servlet to handle a POST request.
     * <p>
     * The HTTP POST method allows the client to send
     * data of unlimited length to the Web server a single time
     * and is useful when posting information such as
     * credit card numbers.
     *
     * <p>When overriding this method, read the request data,
     * write the response headers, get the response's writer or output
     * stream object, and finally, write the response data. It's best
     * to include content type and encoding. When using a
     * <code>PrintWriter</code> object to return the response, set the
     * content type before accessing the <code>PrintWriter</code> object.
     *
     * <p>The servlet container must write the headers before committing the
     * response, because in HTTP the headers must be sent before the
     * response body.
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
     * <p>This method does not need to be either safe or idempotent.
     * Operations requested through POST can have side effects for
     * which the user can be held accountable, for example,
     * updating stored data or buying items online.
     *
     * <p>If the HTTP POST request is incorrectly formatted,
     * <code>doPost</code> returns an HTTP "Bad Request" message.
     *
     * @param req  an {@link HttpServletRequest} object that
     *             contains the request the client has made
     *             of the servlet
     * @param resp an {@link HttpServletResponse} object that
     *             contains the response the servlet sends
     *             to the client
     * @throws IOException if an input or output error is
     *                     detected when the servlet handles
     *                     the request
     * @see ServletOutputStream
     * @see ServletResponse#setContentType
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(req.getInputStream()))) {
            String tmp;
            while ((tmp = buffer.readLine()) != null) {
                body.append(tmp);
            }
        }
        Map<String, Object> bodyObj = KJson.readValue(body.toString());
        this.store(bodyObj);
        OutputStream outputStream = resp.getOutputStream();
        outputStream.write("{\"success\":true}".getBytes("utf-8"));
    }

    /**
     * store file
     *
     * @param data data
     * @throws IOException e
     */
    @SuppressWarnings("unchecked")
    private void store(Map<String, Object> data) throws IOException {
        Map<String, Object> main = (Map<String, Object>) data.get("main");
        String app_id = String.valueOf(main.get("application_id"));
        String app_name = String.valueOf(main.get("application_name"));
        Path dir = JettyServer.app_dir.resolve(app_id);
        if (Files.notExists(dir, LinkOption.NOFOLLOW_LINKS)) Files.createDirectory(dir);
//        String fileName = String.valueOf(main.get("application_name"));
        String fileName = "main";
        Properties properties = new Properties();
        for (String k : main.keySet()) {
            Object value = main.get(k);
            value = value == null ? "" : String.valueOf(value);
            properties.put(k.replaceAll("_", "."), value);
        }
        String temp = this.handleArray(dir, "source", (List<Map<String, Object>>) data.get("sources"));
        properties.put("ks.source", temp == null ? "" : temp);
        temp = this.handleArray(dir, "operation", (List<Map<String, Object>>) data.get("operations"));
        properties.put("ks.operation", temp == null ? "" : temp);
        properties.put("ks.output",
                this.handleObject(dir, "output", (Map<String, Object>) data.get("output")));
        this.storeFile(dir.resolve(fileName).toString(), properties);
        if (!JettyServer.caches.containsKey(app_id)) {
            AppInfo appInfo = new AppInfo();
            appInfo.setName(app_name);
            JettyServer.caches.put(app_id, appInfo);
        } else {
            JettyServer.caches.get(app_id).setName(app_name);
        }
    }

    /**
     * 处理Object类数据
     *
     * @param dir  application id dir
     * @param type output
     * @param data data
     * @return fileName
     * @throws IOException e
     */
    private String handleObject(Path dir, String type, Map<String, Object> data) throws IOException {
        Properties properties = new Properties();
        String fileName = null;
        if ("output".equals(type)) {
            fileName = "output";//String.valueOf(data.get("expandWin_store_name"));
        }
        if (fileName == null) throw new IOException("file name is null...");
        for (String k : data.keySet()) {
            Object value = data.get(k);
            value = value == null ? "" : String.valueOf(value);
            if (k.equals("output_targets")) {
                String temp = ((String) value).substring(1, ((String) value).length() - 1);
                value = replaceBlank(temp);
            }
            properties.put(k.replaceAll("_", "."), value);
        }
        this.storeFile(dir.resolve(fileName).toString(), properties);
        return fileName;
    }

    /**
     * 处理数组类数据
     *
     * @param dir  application id dir
     * @param type source,operation
     * @param list data
     * @return fileName
     * @throws IOException e
     */
    private String handleArray(Path dir, String type, List<Map<String, Object>> list) throws IOException {
        if (list.isEmpty()) return null;
        StringBuilder result = new StringBuilder();
        for (Map<String, Object> object : list) {
            Properties properties = new Properties();
            String fileName = null;
            if ("source".equals(type)) {
                fileName = String.valueOf(object.get("ks_name"));
            } else if ("operation".equals(type)) {
                fileName = String.valueOf(object.get("operation_name"));
            }
            if (fileName == null) throw new IOException("file name is null...");
            result.append(fileName).append(",");
            for (String k : object.keySet()) {
                Object value = object.get(k);
                value = value == null ? "" : String.valueOf(value);
                properties.put(k.replaceAll("_", "."), value);
            }
            this.storeFile(dir.resolve(fileName).toString(), properties);
        }
        return result.substring(0, result.length() - 1);
    }

    /**
     * store file
     *
     * @param file       file path
     * @param properties properties
     * @throws IOException e
     */
    private void storeFile(String file, Properties properties) throws IOException {
        try (FileOutputStream output = new FileOutputStream(file + ".properties")) {
            properties.store(output, null);
        }
    }

    /**
     * 去除空白字符
     *
     * @param str
     * @return
     */
    private String replaceBlank(String str) {
        if (str == null) return str;
        Pattern p = Pattern.compile("\\s*|\t|\r|\n");
        Matcher m = p.matcher(str);
        return m.replaceAll("");
    }
}
