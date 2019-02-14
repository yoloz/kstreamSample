package com.ks.process.output;

import com.ks.error.KConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.mq.Producer;
import org.zbus.net.http.Message;

import java.io.IOException;
import java.util.Properties;

/**
 * output zbus
 */
public class ZbusOutput extends OutPut {

    private final Logger logger = LoggerFactory.getLogger(ZbusOutput.class);

    private Broker broker;
    private final Producer producer;

    ZbusOutput(Properties properties) {
        super(properties);
        String outZbusAddr = nonNullEmpty(properties, "output.target.zbus.address");
        String outZbusMq = nonNullEmpty(properties, "output.target.zbus.mq");
        try {
            this.broker = new ZbusBroker(outZbusAddr);
            this.producer = new Producer(broker, outZbusMq);
            producer.createMQ();
        } catch (IOException | InterruptedException e) {
            throw new KConfigException(e);
        }
    }

    /**
     * 输出到目标库
     *
     * @param key   key
     * @param value value
     */
    @Override
    void apply(String key, String value) {
        try {
            producer.sendSync(new Message(value));
        } catch (IOException | InterruptedException e) {
            logger.warn(value, e);
        }
    }


    /**
     * close impl
     */
    @Override
    public void close() {
        try {
            if (broker != null) {
                broker.close();
            }
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }
        broker = null;
    }
}
