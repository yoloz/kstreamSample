package com.unimas.kska.webservice.impl.ka;

import com.google.common.collect.ImmutableMap;
import com.google.gson.reflect.TypeToken;
import com.unimas.kska.KsServer;
import com.unimas.kska.bean.AppInfo;
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

public class SetAddr extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(SetAddr.class);

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
        logger.debug("setAddr==>" + body);
        Map<String, String> bodyObj = KJson.readStringValue(body);
        String kaUrl = bodyObj.get("kafka_url");
        String zkUrl = bodyObj.get("zk_url");
        String jmxUrl = bodyObj.get("jmx_url");
        String error = null;
        String ds_id = "";
        MysqlOperator mysqlOperator = KsServer.getMysqlOperator();
        try {
            String kds_name = "平台KAFKA";
            List<Map<String, String>> list = mysqlOperator.query("select ds_id from ciisource where ds_name=?",
                    kds_name);
            if (list.isEmpty()) ds_id = "11111111111111";
            else if (list.size() == 1) ds_id = list.get(0).get("ds_id");
            else error = "数据源中存在多个名称[平台kafka]记录";
            if (error == null) {
                if (kaUrl == null || kaUrl.isEmpty()) error = "kafka地址为空";
                if (error == null) try {
                    if (jmxUrl != null && !jmxUrl.isEmpty()) KsServer.setKaJMX(jmxUrl);
                    else error = "jmx地址为空";
                } catch (Throwable e) {
                    error = "jmx地址连接失败,请检查地址和端口";
                    logger.error(error, e);
                }
                if (error == null) try {
                    if (zkUrl != null && !zkUrl.isEmpty()) KsServer.setKsKaClient(zkUrl);
                    else error = "zookeeper地址为空";
                } catch (Throwable e) {
                    error = "zookeeper地址连接失败,请检查地址和端口";
                    logger.error(error, e);
                }
                if (error == null) {
                    KsServer.overWrite(zkUrl, jmxUrl);
                    Map<String, String> ds_jsonM = ImmutableMap.of("kafka_url", kaUrl,
                            "zk_url", zkUrl, "jmx_url", jmxUrl);
                    if (list.isEmpty()) {
                        mysqlOperator.fixUpdate(
                                "insert into ciisource(ds_id,ds_name,ds_type,ds_json)values(?,?,?,?)",
                                ds_id, kds_name, 0, KJson.writeValue(ds_jsonM, new TypeToken<Map<String, String>>() {
                                }.getType()));
                    } else {
                        List<Map<String, String>> app_ids = mysqlOperator.query("select app_id from ksapp where ds_id=?", ds_id);
                        if (!app_ids.isEmpty()) {
                            app_ids.forEach(m -> {
                                String app_id = m.get("app_id");
                                WSUtils.updateMysqlStatus(app_id, AppInfo.Status.INIT);
                                WSUtils.updateCacheStatus(app_id, AppInfo.Status.INIT);
                            });
                        }
                        mysqlOperator.update(null, null,
                                "update ciisource set ds_name=?,ds_type=?,ds_json=? where ds_id=?",
                                kds_name, 0, KJson.writeValue(ds_jsonM, new TypeToken<Map<String, String>>() {
                                }.getType()), ds_id);
                    }
                }
            }
        } catch (SQLException | IOException e) {
            error = "平台kafka设置失败[数据库或IO错误]";
            logger.error(error, e);
        }
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
