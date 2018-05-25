package com.unimas.kska.webservice.impl.ks;

import com.google.common.collect.ImmutableMap;
import com.google.gson.reflect.TypeToken;
import com.unimas.kska.KsServer;
import com.unimas.kska.bean.KJson;
import com.unimas.kska.kafka.KaConsumer;
import com.unimas.kska.webservice.MysqlOperator;
import com.unimas.kska.webservice.WSUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GetFields extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(GetFields.class);

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
        logger.debug("getFields==>" + body);
        Map<String, String> bodyObj = KJson.readStringValue(body);
        String app_id = bodyObj.get("app_id");
        String topic = bodyObj.get("topic");
        String error = null;
        String result = "[]";
        MysqlOperator mysqlOperator = KsServer.getMysqlOperator();
        try {
            List<Map<String, String>> _l = null;
            if (app_id.isEmpty()) {
                _l = mysqlOperator.query("select ds_json from ciisource where ds_name=?",
                        "平台kafka");
            } else {
                List<Map<String, String>> temp = mysqlOperator.query(
                        "select app_name,ds_id from ksapp where app_id=?", app_id);
                if (temp.isEmpty()) error = "任务[" + app_id + "]数据库无记录";
                else {
                    Map<String, String> m = temp.get(0);
                    if (m.containsKey("ds_id"))
                        _l = mysqlOperator.query("select ds_json from ciisource where ds_id=?",
                                m.get("ds_id"));
                }
            }
            if (_l == null || _l.isEmpty()) error = "kafka地址为空!";
            else {
                String ds_json = _l.get(0).get("ds_json");
                Map<String, String> ds_map = KJson.readStringValue(ds_json);
                try (KaConsumer kaConsumer = new KaConsumer(ds_map.get("kafka_url"), topic)) {
                    String value = kaConsumer.getOneValue();
//                    if (value.isEmpty()) error = "kafka中读取数据为空!";
                    if (!value.isEmpty()) {
                        Map<String, String> map = ImmutableMap.of("src", value, "logtype", "json",
                                "separator", "", "head", "false", "keyword", "");
                        String param = KJson.writeValue(map, new TypeToken<Map<String, String>>() {
                        }.getType());
                        logger.debug("parse param:" + param);
                        HttpPost httpPost = new HttpPost(KsServer.wfUrl + "/parse");
                        httpPost.setEntity(new StringEntity(param, Charset.forName("utf-8")));
                        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build();
                             CloseableHttpResponse response = httpClient.execute(httpPost)) {
                            StatusLine statusLine = response.getStatusLine();
                            if (response.getStatusLine().getStatusCode() == 200) {
                                HttpEntity resEntity = response.getEntity();
                                if (resEntity != null) {
                                    String r = EntityUtils.toString(resEntity);
                                    if (r.equals("1")) error = "值解析失败!";
                                    else result = Arrays.toString(r.split(","));
                                }
                            } else error = "请求值解析失败,返回" + statusLine;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            error = "获取kafka地址出错!";
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
