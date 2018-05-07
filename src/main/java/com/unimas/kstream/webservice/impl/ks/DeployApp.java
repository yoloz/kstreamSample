package com.unimas.kstream.webservice.impl.ks;

import com.unimas.kstream.KsServer;
import com.unimas.kstream.bean.AppInfo;
import com.unimas.kstream.bean.KJson;
import com.unimas.kstream.bean.ServiceInfo;
import com.unimas.kstream.webservice.MysqlOperator;
import com.unimas.kstream.webservice.WSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class DeployApp extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(DeleteApp.class);

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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String body = WSUtils.readInputStream(req.getInputStream());
        logger.debug("deployApp==>" + body);
        Map<String, String> bodyObj = KJson.readStringValue(body);
        String app_id = bodyObj.get("app_id");
        String error = WSUtils.unModify(null, app_id);
        if (error == null) {
            Path dir = KsServer.app_dir.resolve(app_id);
            if (Files.notExists(dir, LinkOption.NOFOLLOW_LINKS)) Files.createDirectory(dir);
            MysqlOperator mysqlOperator = KsServer.getMysqlOperator();
            try {
                StringBuilder input_ids = new StringBuilder();
                List<Map<String, String>> main = mysqlOperator.query(
                        "select app_name,main_json,operation_order from ksapp where app_id=?",
                        app_id);
                List<Map<String, String>> inputs = mysqlOperator.query(
                        "select input_id,input_json from ksinput where app_id=?",
                        app_id);
                List<Map<String, String>> operations = mysqlOperator.query(
                        "select operation_id,operation_json from ksoperation where app_id=?",
                        app_id);
                List<Map<String, String>> output = mysqlOperator.query(
                        "select output_json from ksoutput where app_id=?",
                        app_id);
                if (main.isEmpty() || inputs.isEmpty() || output.isEmpty()) {
                    error = "任务数据源或输出为空,请配置后再部署";
                } else {
                    for (Map<String, String> m : inputs) {
                        String _id = m.get("input_id");
                        input_ids.append(_id).append(",");
                        writeFile(dir.resolve(_id + ".properties"), "input", m.get("input_json"), _id);
                    }
                    for (Map<String, String> m : operations) {
                        String _id = m.get("operation_id");
                        writeFile(dir.resolve(_id + ".properties"), "operation", m.get("operation_json"));
                    }
                    for (Map<String, String> m : output) {
                        writeFile(dir.resolve("output.properties"), "output", m.get("output_json"));
                    }
                    String ks_source = input_ids.substring(0, input_ids.length() - 1);
                    for (Map<String, String> m : main) {
                        writeFile(dir.resolve("main.properties"), "main", m.get("main_json"), app_id,
                                m.get("app_name"), ks_source, m.get("operation_order"));
                    }
                    mysqlOperator.fixUpdate("update ksapp set app_status=1 where app_id=?", app_id);
                    WSUtils.updateCacheStatus(app_id, AppInfo.Status.STOP);
                }
            } catch (SQLException e) {
                error = "部署失败:" + e.getMessage();
                logger.error(error, e);
            }
        }
        OutputStream outputStream = resp.getOutputStream();
        if (error == null) {
            outputStream.write("{\"success\":true}".getBytes("utf-8"));
        } else {
            String msg = "{\"success\":false,\"error\":\"" + error + "\"}";
            outputStream.write(msg.getBytes("utf-8"));
        }
    }

    //type,json,other...
    @SuppressWarnings("unchecked")
    private void writeFile(Path path, String... param) throws IOException {
        Properties properties = new Properties();
        switch (param[0]) {
            case "main":
                Map<String, String> ma = KJson.readStringValue(param[1]);
                properties.put("application.id", param[2]);
                properties.put("application.name", param[3]);
                properties.put("ks.source", param[4]);
                properties.put("ks.operation", param[5]);
                properties.put("ks.output", "output");
                ma.forEach((k, v) -> properties.put(k.replaceAll("_", "."), v == null ? "" : v));
                break;
            case "input":
                Map<String, String> in = KJson.readStringValue(param[1]);
                properties.put("ks.name", param[2]);
                in.forEach((k, v) -> properties.put(k.replaceAll("_", "."), v == null ? "" : v));
                break;
            case "output":
                Map<String, Object> out = KJson.readValue(param[1]);
                out.forEach((k, v) -> {
                    String value;
                    if (k.equals("output_targets")) {
                        List<String> targets = (List<String>) v;
                        StringBuilder builder = new StringBuilder();
                        for (int i = 0; i < targets.size(); i++) {
                            builder.append(targets.get(i));
                            if (i != targets.size() - 1) builder.append(",");
                        }
                        value = builder.toString();
                    } else value = v == null ? "" : String.valueOf(v);
                    properties.put(k.replaceAll("_", "."), value == null ? "" : value);
                });
                break;
            default:
                Map<String, String> m = KJson.readStringValue(param[1]);
                m.forEach((k, v) -> properties.put(k.replaceAll("_", "."), v == null ? "" : v));
                break;
        }
        try (FileOutputStream output = new FileOutputStream(path.toFile())) {
            properties.store(output, null);
        }
    }

}
