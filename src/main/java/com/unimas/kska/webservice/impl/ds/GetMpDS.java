package com.unimas.kska.webservice.impl.ds;

import com.google.gson.reflect.TypeToken;
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

public class GetMpDS extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(GetMpDS.class);

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
        logger.debug("getMpDS==>" + body);
        Map<String, String> bodyObj = KJson.readStringValue(body);
        String ds_type = bodyObj.get("ds_type");
        String error = null;
        String result = "[]";
        MysqlOperator mysqlOperator = KsServer.getMysqlOperator();
        try {
            List<Map<String, String>> dslist = mysqlOperator.query(
                    "select ds_id,ds_name from ciisource where ds_type=?", ds_type);
            result = KJson.writeValue(dslist,
                    new TypeToken<List<Map<String, String>>>() {
                    }.getType());
        } catch (SQLException e) {
            error = "信息获取失败[数据库错误]";
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
