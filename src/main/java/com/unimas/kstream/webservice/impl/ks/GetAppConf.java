package com.unimas.kstream.webservice.impl.ks;

import com.google.gson.reflect.TypeToken;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetAppConf extends HttpServlet {

    private Logger logger = LoggerFactory.getLogger(GetAppConf.class);

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
        logger.debug("getAppConf==>" + body);
        Map<String, String> bodyObj = KJson.readStringValue(body);
        String id = bodyObj.get("id");
        String type = bodyObj.get("type");
        String type_id = bodyObj.get("type_id");
        String error = null;
        String result = null;
        try {
            MysqlOperator mysqlOperator = KsServer.getMysqlOperator();
            switch (type) {
                case "main":
                    if (type_id == null || type_id.isEmpty()) {
                        List<Map<String, Object>> list = new ArrayList<>();
                        List<Map<String, String>> main = mysqlOperator.query(
                                "select app_id,app_name,main_json,app_desc,ds_name,ds_id from ksapp where service_id=?",
                                id);
                        for (Map<String, String> map : main) {
                            Map<String, Object> json = KJson.readValue(map.remove("main_json"));
                            json.putAll(map);
                            list.add(json);
                        }
                        result = KJson.writeValue(list, new TypeToken<List<Map<String, Object>>>() {
                        }.getType());
                    } else {
                        Map<String, Object> mo = new HashMap<>(1);
                        List<Map<String, String>> main = mysqlOperator.query(
                                "select app_id,app_name,main_json,app_desc,ds_name,ds_id from ksapp where app_id=?",
                                type_id);
                        if (!main.isEmpty()) {
                            Map<String, String> appMap = main.get(0);
                            Map<String, Object> json = KJson.readValue(appMap.remove("main_json"));
                            mo.putAll(json);
                            mo.putAll(appMap);
                        }
                        result = KJson.writeValueAsString(mo);
                    }
                    break;
                case "input":
                    if (type_id == null || type_id.isEmpty()) {
                        List<Map<String, String>> inputs = mysqlOperator.query(
                                "select input_id,input_json from ksinput where app_id=?",
                                id);
                        List<Map<String, Object>> il = new ArrayList<>();
                        for (Map<String, String> input : inputs) {
                            Map<String, Object> io = KJson.readValue(input.remove("input_json"));
                            io.put("input_id", input.remove("input_id"));
                            il.add(io);
                        }
                        result = KJson.writeValue(il, new TypeToken<List<Map<String, Object>>>() {
                        }.getType());
                    } else {
                        Map<String, Object> min = new HashMap<>(1);
                        List<Map<String, String>> inputs = mysqlOperator.query(
                                "select input_id,input_json from ksinput where input_id=?",
                                type_id);
                        if (!inputs.isEmpty()) {
                            Map<String, String> inMap = inputs.get(0);
                            Map<String, Object> io = KJson.readValue(inMap.remove("input_json"));
                            min.put("input_id", inMap.remove("input_id"));
                            min.putAll(io);
                        }
                        result = KJson.writeValueAsString(min);
                    }
                    break;
                case "operation":
                    if (type_id == null || type_id.isEmpty()) {
                        List<Map<String, String>> operations = mysqlOperator.query(
                                "select operation_id,operation_json from ksoperation where app_id=?",
                                id);
                        List<Map<String, Object>> ol = new ArrayList<>();
                        for (Map<String, String> operation : operations) {
                            Map<String, Object> oo = KJson.readValue(operation.remove("operation_json"));
                            oo.put("operation_id", operation.remove("operation_id"));
                            ol.add(oo);
                        }
                        result = KJson.writeValue(ol, new TypeToken<List<Map<String, Object>>>() {
                        }.getType());
                    } else {
                        Map<String, Object> mop = new HashMap<>(1);
                        List<Map<String, String>> operations = mysqlOperator.query(
                                "select operation_id,operation_json from ksoperation where operation_id=?",
                                type_id);
                        if (!operations.isEmpty()) {
                            Map<String, String> opm = operations.get(0);
                            Map<String, Object> op = KJson.readValue(opm.remove("operation_json"));
                            mop.put("operation_id", opm.remove("operation_id"));
                            mop.putAll(op);
                        }
                        result = KJson.writeValueAsString(mop);
                    }

                    break;
                case "output":
                    Map<String, Object> oo = new HashMap<>(1);
                    List<Map<String, String>> output = mysqlOperator.query(
                            "select output_json from ksoutput where app_id=?",
                            id);
                    if (!output.isEmpty()) oo.putAll(KJson.readValue(output.get(0).remove("output_json")));
                    result = KJson.writeValueAsString(oo);
                    break;
                default:
                    error = "getApp=>type:" + type + " 不支持";
            }
        } catch (SQLException e) {
            error = "获取信息失败:" + e.getMessage();
            logger.error(error, e);
        }
        OutputStream outputStream = resp.getOutputStream();
        String resultS;
        if (error == null) {
            resultS = "{\"success\":true,\"results\":" + result + "}";
            outputStream.write(resultS.getBytes("utf-8"));
        } else {
            resultS = "{\"success\":false,\"error\":\"" + error + "\"}";
            outputStream.write(resultS.getBytes("utf-8"));
        }
    }
}
