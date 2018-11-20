package com.unimas.kska.webservice.impl.ka;

import com.google.gson.reflect.TypeToken;
import com.unimas.kska.KsServer;
import com.unimas.kska.bean.KJson;
import com.unimas.kska.kafka.KaJMX;
import com.unimas.kska.kafka.KskaClient;
import com.unimas.kska.webservice.WSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class GetAllTopic extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(GetAllTopic.class);

    /**
     * Called by the server (via the <code>service</code> method) to
     * allow a servlet to handle a GET request.
     *
     * <p>Overriding this method to support a GET request also
     * automatically supports an HTTP HEAD request. A HEAD
     * request is a GET request that returns no body in the
     * response, only the request header fields.
     *
     * <p>When overriding this method, read the request data,
     * write the response headers, get the response's writer or
     * output stream object, and finally, write the response data.
     * It's best to include content type and encoding. When using
     * a <code>PrintWriter</code> object to return the response,
     * set the content type before accessing the
     * <code>PrintWriter</code> object.
     *
     * <p>The servlet container must write the headers before
     * committing the response, because in HTTP the headers must be sent
     * before the response body.
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
     * <p>The GET method should be safe, that is, without
     * any side effects for which users are held responsible.
     * For example, most form queries have no side effects.
     * If a client request is intended to change stored data,
     * the request should use some other HTTP method.
     *
     * <p>The GET method should also be idempotent, meaning
     * that it can be safely repeated. Sometimes making a
     * method safe also makes it idempotent. For example,
     * repeating queries is both safe and idempotent, but
     * buying a product online or modifying data is neither
     * safe nor idempotent.
     *
     * <p>If the request is incorrectly formatted, <code>doGet</code>
     * returns an HTTP "Bad Request" message.
     *
     * @param req  an {@link HttpServletRequest} object that
     *             contains the request the client has made
     *             of the servlet
     * @param resp an {@link HttpServletResponse} object that
     *             contains the response the servlet sends
     *             to the client
     * @throws IOException if an input or output error is
     *                     detected when the servlet handles
     *                     the GET request
     * @see ServletResponse#setContentType
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String body = WSUtils.readInputStream(req.getInputStream());
        logger.debug("getAllTopic==>" + body);
        List<Map<String, String>> topics = new ArrayList<>();
        String error = null;
        KskaClient kaClient = null;
        KaJMX jmxClient = null;
        try {
            kaClient = KsServer.getKsKaClient();
        } catch (Throwable e) {
            error = e.getMessage();
            logger.error(error, e);
        }
        try {
            jmxClient = KsServer.getKaJMX();
        } catch (Throwable e) {
            error = e.getMessage();
            logger.error(error, e);
        }
        if (error == null && kaClient != null && jmxClient != null) try {
            List<Map<String, String>> _topics = kaClient.getAllTopics();
            for (Map<String, String> m : _topics) {
                Map<String, String> _map = new HashMap<>(m);
                Map<String, Object> offsets = jmxClient.getLogEndOffset(m.get("topic"));
                _map.put("total", String.valueOf(offsets.get("total")));
                topics.add(_map);
            }
        } catch (Throwable e) {
            error = "请检查zookeeper地址端口是否正确";
            logger.error(error, e);
        }
        OutputStream outputStream = resp.getOutputStream();
        if (error == null) {
            String result = "{\"success\":true,\"results\":" + KJson.writeValue(topics,
                    new TypeToken<List<Map<String, String>>>() {
                    }.getType()) + "}";
            outputStream.write(result.getBytes("utf-8"));
        } else {
            String result = "{\"success\":false,\"error\":\"" + error + "\"}";
            outputStream.write(result.getBytes("utf-8"));
        }
    }
}