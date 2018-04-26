package com.unimas.kstream.webservice.impl.ks;


import com.unimas.kstream.KsServer;
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
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.Map;

public class DeleteApp extends HttpServlet {

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
        logger.debug("deleteApp==>" + body);
        Map<String, String> bodyObj = KJson.readStringValue(body);
        String service_id = bodyObj.get("service_id");
        String type = bodyObj.get("type");
        String type_id = bodyObj.get("type_id");
        String error = WSUtils.unModify(service_id);
        if (error == null) {
            String sql = null;
            String status = null;
            Object[] params = null;
            switch (type) {
                case "main":
                    sql = "delete from ksapp where service_id=?";
                    params = new String[]{service_id};
                    break;
                case "input":
                    status = "update ksapp set service_status='init' where service_id='" + service_id + "'";
                    sql = "delete from ksinput where service_id=? and input_id=?";
                    params = new String[]{service_id, type_id};
                    break;
                case "operation":
                    status = "update ksapp set service_status='init' where service_id='" + service_id + "'";
                    sql = "delete from ksoperations where service_id=? and operation_id=?";
                    params = new String[]{service_id, type_id};
                    break;
                case "output":
                    status = "update ksapp set service_status='init' where service_id='" + service_id + "'";
                    sql = "delete from ksoutput where service_id=?";
                    params = new String[]{service_id};
                    break;
                default:
                    error = "deleteApp=>type:" + type + " 不支持";
            }
            if (error == null) {
                try {
                    MysqlOperator mysqlOperator = KsServer.getMysqlOperator();
                    mysqlOperator.update(status, null, sql, params);
                    if ("main".equals(type)) {
                        KsServer.caches.remove(service_id);
                        Path dir = KsServer.app_dir.resolve(service_id);
                        Files.walkFileTree(dir, new EmptyDir());
                    } else WSUtils.initCacheStatus(service_id);
                } catch (SQLException e) {
                    error = "删除失败:" + e.getMessage();
                    logger.error(error, e);
                }
            }
        }
        OutputStream outputStream = resp.getOutputStream();
        String result;
        if (error == null) {
            outputStream.write("{\"success\":true}".getBytes("utf-8"));
        } else {
            result = "{\"success\":true,\"error\":\"" + error + "\"}";
            outputStream.write(result.getBytes("utf-8"));
        }
    }


    private class EmptyDir extends SimpleFileVisitor<Path> {
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
}
