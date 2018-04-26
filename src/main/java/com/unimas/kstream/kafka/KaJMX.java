package com.unimas.kstream.kafka;


import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.util.Set;

public class KaJMX {

    private final Logger logger = LoggerFactory.getLogger(KaJMX.class);

    private final JMXConnector jmxConnector;
    private final MBeanServerConnection context;

    public KaJMX(String url) throws IOException {
        JMXServiceURL jmxServiceURL = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + url + "/jmxrmi");
        this.jmxConnector = JMXConnectorFactory.connect(jmxServiceURL);
        this.context = jmxConnector.getMBeanServerConnection();
    }

    public ImmutableMap<String, Object> getLogEndOffset(String topic) throws Exception {
        ImmutableMap.Builder<String, Object> map = new ImmutableMap.Builder<>();
        ObjectName objectName = new ObjectName("kafka.log:type=Log,name=LogEndOffset,topic=" + topic + ",partition=*");
        Set<ObjectName> objectNames = context.queryNames(objectName, null);
        for (ObjectName obj : objectNames) {
            map.put(obj.getKeyProperty("partition"), context.getAttribute(new ObjectName(obj.getCanonicalName()), "Value"));
        }
        return map.build();
    }

    public void close() {
        try {
            if (jmxConnector != null) jmxConnector.close();
        } catch (IOException e) {
            logger.error("KaJMX关闭出错", e);
        }
    }
}
