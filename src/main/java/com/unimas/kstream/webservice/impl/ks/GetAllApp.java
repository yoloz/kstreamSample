package com.unimas.kstream.webservice.impl.ks;

import com.google.gson.reflect.TypeToken;
import com.unimas.kstream.KsServer;
import com.unimas.kstream.bean.AppInfo;
import com.unimas.kstream.bean.KJson;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetAllApp extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(GetAllApp.class);

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
        logger.debug("getAllApp==>" + body);
        Map<String, String> bodyObj = KJson.readStringValue(body);
        String service_id = bodyObj.get("service_id");
        List<Map<String, String>> list = new ArrayList<>(5);
        if (KsServer.caches.containsKey(service_id)) {
            Map<String, AppInfo> appInfoMap = KsServer.caches.get(service_id).getAppInfoMap();
            if (appInfoMap != null) for (AppInfo appInfo : appInfoMap.values()) {
                Map<String, String> map = new HashMap<>();
                map.put("app_id", appInfo.getId());
                map.put("app_name", appInfo.getName());
                map.put("app_desc", appInfo.getDesc());
                map.put("app_status", appInfo.getStatus().getValue());
                map.put("app_cpu", appInfo.getCpu());
                map.put("app_mem", appInfo.getMem());
                map.put("app_time", appInfo.getRuntime());
                list.add(map);
            }
        }
        OutputStream outputStream = resp.getOutputStream();
        String result = "{\"success\":true,\"results\":" + KJson.writeValue(list,
                new TypeToken<List<Map<String, String>>>() {
                }.getType()) + "}";
        outputStream.write(result.getBytes("utf-8"));
    }
}
