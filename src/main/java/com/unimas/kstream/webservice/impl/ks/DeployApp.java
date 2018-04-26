package com.unimas.kstream.webservice.impl.ks;

import com.unimas.kstream.KsServer;
import com.unimas.kstream.bean.AppInfo;
import com.unimas.kstream.bean.KJson;
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
        String service_id = bodyObj.get("service_id");
        String error = WSUtils.unModify(service_id);
        if (error == null) {
            Path dir = KsServer.app_dir.resolve(service_id);
            if (Files.notExists(dir, LinkOption.NOFOLLOW_LINKS)) Files.createDirectory(dir);
            MysqlOperator mysqlOperator = KsServer.getMysqlOperator();
            try {
                StringBuilder input_ids = new StringBuilder();
                List<Map<String, String>> main = mysqlOperator.query(
                        "select service_name,main_json,operation_order from ksapp where service_id=?",
                        service_id);
                List<Map<String, String>> inputs = mysqlOperator.query(
                        "select input_id,input_json from ksinput where service_id=?",
                        service_id);
                List<Map<String, String>> operations = mysqlOperator.query(
                        "select operation_id,operation_json from ksoperations where service_id=?",
                        service_id);
                List<Map<String, String>> output = mysqlOperator.query(
                        "select output_json from ksoutput where service_id=?",
                        service_id);
                if (main.isEmpty() || inputs.isEmpty() || output.isEmpty()) {
                    error = KsServer.caches.get(service_id).getName() + " input is empty";
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
                        writeFile(dir.resolve("main.properties"), "main", m.get("main_json"), service_id,
                                m.get("service_name"), ks_source, m.get("operation_order"));
                    }
                    mysqlOperator.fixUpdate("update ksapp set service_status='stop' where service_id=?",
                            service_id);
                    KsServer.caches.get(service_id).setStatus(AppInfo.Status.STOP);
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
            String msg = "{\"success\":true,\"error\":\"" + error + "\"}";
            outputStream.write(msg.getBytes("utf-8"));
        }
    }

    //type,json,other...
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
                ma.forEach((k, v) -> properties.put(k.replaceAll("_", "."), v));
                break;
            case "input":
                Map<String, String> in = KJson.readStringValue(param[1]);
                properties.put("ks.name", param[2]);
                in.forEach((k, v) -> properties.put(k.replaceAll("_", "."), v));
                break;
            default:
                Map<String, String> m = KJson.readStringValue(param[1]);
                m.forEach((k, v) -> properties.put(k.replaceAll("_", "."), v));
                break;
        }
        try (FileOutputStream output = new FileOutputStream(path.toFile())) {
            properties.store(output, null);
        }
    }

}
