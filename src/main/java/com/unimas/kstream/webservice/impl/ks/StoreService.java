package com.unimas.kstream.webservice.impl.ks;

import com.unimas.kstream.KsServer;
import com.unimas.kstream.bean.KJson;
import com.unimas.kstream.bean.ObjectId;
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
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Map;

public class StoreService extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(StoreService.class);

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
        logger.debug("storeService==>" + body);
        Map<String, String> bodyObj = KJson.readStringValue(body);
        String id = bodyObj.get("service_id");
        String name = bodyObj.get("service_name");
        String desc = bodyObj.get("service_desc");
        String error = null;
        MysqlOperator mysqlOperator = KsServer.getMysqlOperator();
        try {
            if (id == null || id.isEmpty()) {
                id = ObjectId.get().toString();
                mysqlOperator.fixUpdate(
                        "insert into ksservice(service_id,service_name,service_desc)values(?,?,?)",
                        id, name, desc);
            } else {
                mysqlOperator.update(null, null,
                        "update ksservice set service_name=?,service_desc=? where service_id=?",
                        name, desc, id);
            }
            if (KsServer.caches.containsKey(id)) {
                ServiceInfo info = KsServer.caches.get(id);
                info.setName(name);
                info.setDesc(desc);
            } else {
                ServiceInfo info = new ServiceInfo();
                info.setName(name);
                info.setDesc(desc);
                KsServer.caches.put(id, info);
            }
        } catch (SQLException e) {
            error = "保存失败[数据库异常]";
            logger.error(error, e);
        }
        OutputStream outputStream = resp.getOutputStream();
        String result;
        if (error == null) {
            result = "{\"success\":true,\"service_id\":\"" + id + "\"}";
            outputStream.write(result.getBytes("utf-8"));
        } else {
            result = "{\"success\":false,\"error\":\"" + error + "\"}";
            outputStream.write(result.getBytes("utf-8"));
        }
    }
}
