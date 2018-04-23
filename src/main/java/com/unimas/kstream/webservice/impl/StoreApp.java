package com.unimas.kstream.webservice.impl;

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
import java.sql.SQLException;
import java.util.Map;

/**
 * 应用目录名:application_id
 * 入口文件统一命名:main.properties
 * 输出文件统一命名:output.properties
 */
public class StoreApp extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(StoreApp.class);


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
        String body = WSUtils.readInputStream(req.getInputStream());
        logger.debug("storeApp==>" + body);
        Map<String, Object> bodyObj = KJson.readValue(body);
        String service_id = String.valueOf(bodyObj.get("service_id"));
        String type = String.valueOf(bodyObj.get("type"));
        Map<String, Object> value = (Map<String, Object>) bodyObj.get("value");
        String error = WSUtils.unModify(service_id);
        String id = null;
        if (error == null) {
            try {
                switch (type) {
                    case "main":
                        id = storeMain(service_id, value);
                        break;
                    case "input":
                        id = storeInput(service_id, value);
                        break;
                    case "operation":
                        id = storeOperation(service_id, value);
                        break;
                    case "output":
                        storeOutput(service_id, value);
                        break;
                    default:
                        error = "storeApp=>type:" + type + " 不支持";
                }
            } catch (SQLException e) {
                error = "保存失败:" + e.getMessage();
                logger.error(error, e);
            }
        }
        OutputStream outputStream = resp.getOutputStream();
        String result;
        if (error == null) {
            if (id == null) result = "{\"success\":true}";
            else result = "{\"success\":true,\"id\":\"" + id + "\"}";
            outputStream.write(result.getBytes("utf-8"));
        } else {
            result = "{\"success\":true,\"error\":\"" + error + "\"}";
            outputStream.write(result.getBytes("utf-8"));
        }
    }


    private String storeMain(String service_id, Map<String, Object> value) throws IOException, SQLException {
        MysqlOperator mysqlOperator = KsServer.getMysqlOperator();
        String service_name = String.valueOf(value.remove("service_name"));
        String service_desc = String.valueOf(value.remove("service_desc"));
        if (service_id == null || service_id.isEmpty()) {
            service_id = WSUtils.getUid();
            mysqlOperator.fixUpdate(
                    "insert into ksapp(service_id,service_name,service_desc,main_json)values(?,?,?,?)",
                    service_id, service_name, service_desc, KJson.writeValueAsString(value));
        } else {
            String status = "update ksapp set service_status='init' where service_id='" + service_id + "'";
            mysqlOperator.update(status, null,
                    "update ksapp set service_name=?,service_desc=?,main_json=? where service_id=?",
                    service_name, service_desc, KJson.writeValueAsString(value), service_id);
        }
        WSUtils.initCacheStatus(service_id, service_name, service_desc);
        return service_id;
    }

    private String storeInput(String service_id, Map<String, Object> value) throws IOException, SQLException {
        MysqlOperator mysqlOperator = KsServer.getMysqlOperator();
        String status = "update ksapp set service_status='init' where service_id='" + service_id + "'";
        String input_id = String.valueOf(value.remove("input_id"));
        if (input_id == null || input_id.isEmpty()) {
            input_id = System.currentTimeMillis() + "";
            mysqlOperator.update(status, null,
                    "insert into ksinput(service_id,input_id,input_json)values(?,?,?)",
                    service_id, input_id, KJson.writeValueAsString(value));
        } else {
            mysqlOperator.update(status, null,
                    "update ksinput set input_json=? where service_id=? and input_id=?",
                    KJson.writeValueAsString(value), service_id, input_id);
        }
        WSUtils.initCacheStatus(service_id);
        return input_id;
    }

    private String storeOperation(String service_id, Map<String, Object> value) throws IOException, SQLException {
        MysqlOperator mysqlOperator = KsServer.getMysqlOperator();
        String status = "update ksapp set service_status='init' where service_id='" + service_id + "'";
        String operation_id = String.valueOf(value.remove("operation_id"));
        if (operation_id == null || operation_id.isEmpty()) {
            operation_id = System.currentTimeMillis() + "";
            mysqlOperator.update(status, null,
                    "insert into ksoperations(service_id,operation_id,operation_json)values(?,?,?)",
                    service_id, operation_id, KJson.writeValueAsString(value));
        } else {
            mysqlOperator.update(status, null,
                    "update ksoperations set operation_json=? where service_id=? and operation_id=?",
                    KJson.writeValueAsString(value), service_id, operation_id);
        }
        WSUtils.initCacheStatus(service_id);
        return operation_id;
    }

    private void storeOutput(String service_id, Map<String, Object> value) throws IOException, SQLException {
        MysqlOperator mysqlOperator = KsServer.getMysqlOperator();
        String status = "update ksapp set service_status='init' where service_id='" + service_id + "'";
        String delete = "delete from ksoutput where service_id='" + service_id + "'";
        mysqlOperator.update(status, delete,
                "insert into ksoutput(service_id,output_json)values(?,?)",
                service_id, KJson.writeValueAsString(value));
        WSUtils.initCacheStatus(service_id);
    }
}
