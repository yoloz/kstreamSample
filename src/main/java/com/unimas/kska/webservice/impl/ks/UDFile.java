package com.unimas.kska.webservice.impl.ks;

import com.google.common.collect.ImmutableList;
import com.unimas.kska.KsServer;
import com.unimas.kska.webservice.MysqlOperator;
import com.unimas.kska.webservice.WSUtils;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * upload or download file
 */
@MultipartConfig
public class UDFile extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(UDFile.class);

    private static final int MEMORY_THRESHOLD = 1024 * 1024 * 3;  // 3MB
    private static final int MAX_FILE_SIZE = 1024 * 1024 * 40; // 40MB
    private static final int MAX_REQUEST_SIZE = 1024 * 1024 * 50; // 50MB

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
//        String type = req.getParameter("type");
//        String id = req.getParameter("id");
//        String error = null;
//        MysqlOperator mysqlOperator = KsServer.getMysqlOperator();
//        try {
//            String service_id;
//            List<String> app_ids;
//            List<Map<String, String>> list;
//            if ("app".equals(type)) list = mysqlOperator.query(
//                    "select service_id from ksapp where app_id=? and app_status>0", id);
//            else list = mysqlOperator.query(
//                    "select app_id from ksapp where service_id=? and app_status>0", id);
//            if (list.isEmpty()) error = type.equals("app") ? "任务" : "服务" + "[" + id + "]无可下载文件";
//            if ("app".equals(type)) {
//                app_ids = ImmutableList.of(id);
//                service_id = list.get(0).get("service_id");
//            } else {
//                service_id = id;
//                ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();
//                for (Map<String, String> map : list) {
//                    builder.add(map.get("app_id"));
//                }
//                app_ids = builder.build();
//            }
//            Path dir = KsServer.download_dir.resolve(service_id);
//            if (Files.exists(dir, LinkOption.NOFOLLOW_LINKS)) Files.walkFileTree(dir, new WSUtils.EmptyDir());
//            Files.createDirectory(dir);
//            mysqlOperator.fixUpdate("select * from ksservice where service_id=? into outfile '/home/ylzhang/test.sql'",
//                    service_id);
//        } catch (SQLException e) {
//
//        }
        String value = req.getParameter("f");
        logger.debug(value);
        String error;
        Path file = Paths.get("/home/ylzhang/projects/kstream/upload/scala-2.11.11.tgz");
        OutputStream output = resp.getOutputStream();
        try {
            resp.setContentType("multipart/form-data;" +
                    URLEncoder.encode(file.getFileName().toString(), "UTF-8"));
//            resp.setHeader("Content-Disposition", "attachment; filename=\"" +
//                    URLEncoder.encode(file.getFileName().toString(), "UTF-8") + "\"");
            try (InputStream in = new FileInputStream(file.toFile())) {
                int len;
                byte b[] = new byte[32 * 1024];
                while ((len = in.read(b)) != -1) {
                    output.write(b, 0, len);
                }
            }
        } catch (IOException e) {
            error = "准备下载文件数据出错!";
            logger.error(error, e);
            String result = "{\"success\":false,\"error\":\"" + error + "\"}";
            output.write(result.getBytes("utf-8"));
        } finally {
            if (output != null) output.close();
        }
    }

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
     * @throws IOException      if an input or output error is
     *                          detected when the servlet handles
     *                          the request
     * @throws ServletException if the request for the POST
     *                          could not be handled
     * @see ServletOutputStream
     * @see ServletResponse#setContentType
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String error = null;
        req.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT,
                new MultipartConfigElement(System.getProperty("java.io.tmpdir"),
                        MAX_FILE_SIZE, MAX_REQUEST_SIZE, MEMORY_THRESHOLD));
        Collection<Part> parts = req.getParts();
        if (!parts.isEmpty()) {
            for (Part part : parts) {
                Path fp = KsServer.upload_dir.resolve(part.getSubmittedFileName());
                try (FileChannel target = FileChannel.open(fp, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                     ReadableByteChannel source = Channels.newChannel(part.getInputStream())) {
                    target.transferFrom(source, 0, Integer.MAX_VALUE);
                    logger.debug("upload file:" + fp);
                } catch (IOException e) {
                    error = "上传文件失败!";
                    logger.error(error, e);
                }
            }
        } else error = "上传文件为空";
        OutputStream outputStream = resp.getOutputStream();
        String result;
        if (error == null) {
            outputStream.write("{\"success\":true}".getBytes("utf-8"));
        } else {
            result = "{\"success\":false,\"error\":\"" + error + "\"}";
            outputStream.write(result.getBytes("utf-8"));
        }
    }
}
