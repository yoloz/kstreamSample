package com.unimas.kska.webservice.impl.ds;

import com.unimas.kska.KsServer;
import com.unimas.kska.bean.DSType;
import com.unimas.kska.bean.KJson;
import com.unimas.kska.bean.ObjectId;
import com.unimas.kska.webservice.MysqlOperator;
import com.unimas.kska.webservice.WSUtils;
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

public class StoreSource extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(StoreSource.class);

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
        logger.debug("storeSource==>" + body);
        Map<String, Object> bodyObj = KJson.readValue(body);
        String ds_id = (String) bodyObj.get("ds_id");
        String ds_name = (String) bodyObj.get("ds_name");
        int ds_type = DSType.getType((String) bodyObj.get("ds_type"));
        Map<String, Object> ds_json = (Map<String, Object>) bodyObj.get("ds_json");
        String error = null;
        MysqlOperator mysqlOperator = KsServer.getMysqlOperator();
        try {
            if (ds_id == null || ds_id.isEmpty()) {
                ds_id = ObjectId.get().toString();
                mysqlOperator.fixUpdate(
                        "insert into ciisource(ds_id,ds_name,ds_type,ds_json)values(?,?,?,?)",
                        ds_id, ds_name, ds_type, KJson.writeValueAsString(ds_json));
            } else {
                if ("11111111111111".equals(ds_id)) error = "平台KAFKA这里不可修改!";
                else mysqlOperator.update(null, null,
                        "update ciisource set ds_name=?,ds_type=?,ds_json=? where ds_id=?",
                        ds_name, ds_type, KJson.writeValueAsString(ds_json), ds_id);
            }
        } catch (SQLException e) {
            error = "保存失败[数据库错误]";
            logger.error(error, e);
        }
        OutputStream outputStream = resp.getOutputStream();
        String result;
        if (error == null) {
            result = "{\"success\":true,\"ds_id\":\"" + ds_id + "\"}";
            outputStream.write(result.getBytes("utf-8"));
        } else {
            result = "{\"success\":false,\"error\":\"" + error + "\"}";
            outputStream.write(result.getBytes("utf-8"));
        }
    }
}
