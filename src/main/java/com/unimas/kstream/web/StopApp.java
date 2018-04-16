package com.unimas.kstream.web;

import com.unimas.kstream.JettyServer;
import com.unimas.kstream.StopProcess;
import com.unimas.kstream.bean.AppInfo;
import com.unimas.kstream.bean.KJson;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;

public class StopApp extends HttpServlet {

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
        StringBuilder body = new StringBuilder();
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(req.getInputStream()))) {
            String tmp;
            while ((tmp = buffer.readLine()) != null) {
                body.append(tmp);
            }
        }
        Map<String, Object> bodyObj = KJson.readValue(body.toString());
        String app_id = String.valueOf(bodyObj.get("application_id"));
        //运行时直接将状态改为停止,如果执行停止失败,后台线程会定期复查并更正运行状态
        AppInfo app = JettyServer.caches.get(app_id);
        app.setStatus(JettyServer.Status.STOP);
        app.setPid("");
        StopProcess.stop(JettyServer.app_dir.resolve(app_id).resolve("pid"));
        OutputStream outputStream = resp.getOutputStream();
        outputStream.write("{\"success\":true}".getBytes("utf-8"));
    }
}
