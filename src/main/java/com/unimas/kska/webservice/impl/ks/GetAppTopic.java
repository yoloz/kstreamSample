package com.unimas.kska.webservice.impl.ks;

import com.unimas.kska.KsServer;
import com.unimas.kska.bean.KJson;
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
import java.util.List;
import java.util.Map;

public class GetAppTopic extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(GetAppTopic.class);

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
        logger.debug("getAppTopic==>" + body);
        Map<String, String> bodyObj = KJson.readStringValue(body);
        String app_id = bodyObj.get("app_id");
        String error = null;
        StringBuilder result = new StringBuilder("[");
        MysqlOperator mysqlOperator = KsServer.getMysqlOperator();
        try {
            List<Map<String, String>> inputs = mysqlOperator.query(
                    "select input_json from ksinput where app_id=?",
                    app_id);
            if (!inputs.isEmpty()) {
                for (int i = 0; i < inputs.size(); i++) {
                    Map<String, String> _v = KJson.readStringValue(inputs.get(i).get("input_json"));
                    if (_v.containsKey("ks_topics")) {
                        result.append(_v.get("ks_topics"));
                        if (i != inputs.size() - 1) result.append(",");
                    } else error = "任务[" + app_id + "]数据源ks_topics未配置!";
                }
                result.append("]");
            } else error = "任务[" + app_id + "]数据源未配置!";
        } catch (SQLException e) {
            error = "查询表[ksinput]出错!";
            logger.error(error, e);
        }
        OutputStream outputStream = resp.getOutputStream();
        if (error == null) {
            String r = "{\"success\":true,\"results\":" + result + "}";
            outputStream.write(r.getBytes("utf-8"));
        } else {
            String r = "{\"success\":false,\"error\":\"" + error + "\"}";
            outputStream.write(r.getBytes("utf-8"));
        }
    }
}
