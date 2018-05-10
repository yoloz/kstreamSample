package com.unimas.kstream.webservice.impl.ks;

import com.unimas.kstream.KsServer;
import com.unimas.kstream.bean.AppInfo;
import com.unimas.kstream.bean.KJson;
import com.unimas.kstream.bean.ObjectId;
import com.unimas.kstream.bean.ServiceInfo;
import com.unimas.kstream.error.KRunException;
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

/**
 * 应用目录名:application_id
 * 入口文件统一命名:main.properties
 * 输出文件统一命名:output.properties
 */
public class StoreApp extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(StoreApp.class);


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
        logger.debug("storeApp==>" + body);
        Map<String, Object> bodyObj = KJson.readValue(body);
        String id = (String) bodyObj.get("id");
        String type = String.valueOf(bodyObj.get("type"));
        Map<String, Object> value = (Map<String, Object>) bodyObj.get("value");
        String error = null;
        String re_id = null;
        try {
            switch (type) {
                case "main":
                    re_id = storeMain(id, value);
                    break;
                case "input":
                    re_id = storeInput(id, value);
                    break;
                case "operation":
                    re_id = storeOperation(id, value);
                    break;
                case "output":
                    storeOutput(id, value);
                    break;
                default:
                    error = "storeApp=>type:" + type + " 不支持";
            }
        } catch (SQLException | IOException e) {
            error = "保存失败[数据库或IO异常]";
            logger.error(error, e);
        } catch (KRunException e) {
            error = e.getMessage();
            logger.error(error, e);
        }
        OutputStream outputStream = resp.getOutputStream();
        String result;
        if (error == null) {
            if (re_id == null) result = "{\"success\":true}";
            else result = "{\"success\":true,\"id\":\"" + re_id + "\"}";
            outputStream.write(result.getBytes("utf-8"));
        } else {
            result = "{\"success\":false,\"error\":\"" + error + "\"}";
            outputStream.write(result.getBytes("utf-8"));
        }
    }


    private String storeMain(String service_id, Map<String, Object> value) throws IOException, SQLException {
        String app_id = (String) value.remove("app_id");
        String app_name = (String) value.remove("app_name");
        String app_desc = (String) value.remove("app_desc");
        String ds_id = (String) value.remove("ds_id");
        String error = (app_id == null || app_id.isEmpty()) ? null : WSUtils.unModify(service_id, app_id);
        if (error == null) {
            MysqlOperator mysqlOperator = KsServer.getMysqlOperator();
            Map<String, String> dsm = mysqlOperator.query("select ds_name,ds_json from ciisource where ds_id=?",
                    ds_id).get(0);
            String ds_name = dsm.get("ds_name");
            String ds_json = dsm.get("ds_json");
            Map<String, String> ka_source = KJson.readStringValue(ds_json);
            String zk_url = ka_source.get("zk_url");
            value.put("bootstrap_servers", ka_source.get("kafka_url"));
            value.put("ks_zookeeper_url", zk_url);
            if (app_id == null || app_id.isEmpty()) {
                app_id = ObjectId.get().toString();
                mysqlOperator.fixUpdate(
                        "insert into ksapp(service_id,app_id,app_name,app_desc,main_json,zk_url,ds_id,ds_name)" +
                                "values(?,?,?,?,?,?,?,?)",
                        service_id, app_id, app_name, app_desc, KJson.writeValueAsString(value), zk_url, ds_id, ds_name);
            } else {
                String status = "update ksapp set app_status=0 where app_id='" + app_id + "'";
                mysqlOperator.update(status, null,
                        "update ksapp set app_name=?,app_desc=?,main_json=?,zk_url=?,ds_id=?,ds_name=? where app_id=?",
                        app_name, app_desc, KJson.writeValueAsString(value), zk_url, ds_id, ds_name, app_id);
            }
            ServiceInfo serviceInfo = KsServer.caches.get(service_id);
            Map<String, AppInfo> appInfoMap = serviceInfo.getAppInfoMap();
            AppInfo appInfo;
            if (appInfoMap == null || !appInfoMap.containsKey(app_id)) {
                appInfo = new AppInfo();
                appInfo.setId(app_id);
                serviceInfo.addAppInfo(appInfo);
            }
            appInfo = serviceInfo.getAppInfoMap().get(app_id);
            appInfo.setName(app_name);
            appInfo.setDesc(app_desc);
            appInfo.setZkUrl(zk_url);
            appInfo.setStatus(AppInfo.Status.INIT);
            return app_id;
        } else throw new KRunException(error);
    }

    private String storeInput(String app_id, Map<String, Object> value) throws IOException, SQLException {
        String error = WSUtils.unModify(null, app_id);
        if (error == null) {
            MysqlOperator mysqlOperator = KsServer.getMysqlOperator();
            String status = "update ksapp set app_status=0 where app_id='" + app_id + "'";
            String input_id = (String) value.remove("input_id");
            if (input_id == null || input_id.isEmpty()) {
                input_id = ObjectId.get().toString();
                mysqlOperator.update(status, null,
                        "insert into ksinput(app_id,input_id,input_json)values(?,?,?)",
                        app_id, input_id, KJson.writeValueAsString(value));
            } else {
                mysqlOperator.update(status, null,
                        "update ksinput set input_json=? where app_id=? and input_id=?",
                        KJson.writeValueAsString(value), app_id, input_id);
            }
            WSUtils.updateCacheStatus(app_id, AppInfo.Status.INIT);
            return input_id;
        } else throw new KRunException(error);
    }

    private String storeOperation(String app_id, Map<String, Object> value) throws IOException, SQLException {
        String error = WSUtils.unModify(null, app_id);
        if (error == null) {
            MysqlOperator mysqlOperator = KsServer.getMysqlOperator();
            String status = "update ksapp set app_status=0 where app_id='" + app_id + "'";
            String operation_id = (String) value.remove("operation_id");
            if (operation_id == null || operation_id.isEmpty()) {
                operation_id = ObjectId.get().toString();
                mysqlOperator.update(status, null,
                        "insert into ksoperation(app_id,operation_id,operation_json)values(?,?,?)",
                        app_id, operation_id, KJson.writeValueAsString(value));
            } else {
                mysqlOperator.update(status, null,
                        "update ksoperation set operation_json=? where app_id=? and operation_id=?",
                        KJson.writeValueAsString(value), app_id, operation_id);
            }
            WSUtils.updateCacheStatus(app_id, AppInfo.Status.INIT);
            return operation_id;
        } else throw new KRunException(error);
    }

    private void storeOutput(String app_id, Map<String, Object> value) throws IOException, SQLException {
        String error = WSUtils.unModify(null, app_id);
        if (error == null) {
            MysqlOperator mysqlOperator = KsServer.getMysqlOperator();
            String status = "update ksapp set app_status=0 where app_id='" + app_id + "'";
            String delete = "delete from ksoutput where app_id='" + app_id + "'";
            mysqlOperator.update(status, delete,
                    "insert into ksoutput(app_id,output_json)values(?,?)",
                    app_id, KJson.writeValueAsString(value));
            WSUtils.updateCacheStatus(app_id, AppInfo.Status.INIT);
        } else throw new KRunException(error);
    }
}
