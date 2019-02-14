package com.ks.zbus;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.mq.Consumer;

import java.io.IOException;

/**
 * linux:
 * java -cp .:${project.basedir}/libs/zbus/zbus-7.2.1.jar:
 * kstream/libs/zbus/netty-all-4.0.33.Final.jar
 * ConsumerExample 127.0.0.1:15555 test
 * <p>
 * window:
 * java -cp .;${project.basedir}/libs/zbus/zbus-7.2.1.jar;
 * kstream/libs/zbus/netty-all-4.0.33.Final.jar
 * ConsumerExample 127.0.0.1:15555 test
 */
public class ConsumerExample {

    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            System.out.printf("Missing configuration parameters...\nusage:%s %s\n",
                    "address<ip:port>",
                    "mq<name>");
            System.exit(-1);
        }
        if (args.length != 2) {
            System.out.println("missing config.....");
            System.exit(-1);
        }
        Broker broker = new ZbusBroker(args[0]);
        new Consumer(broker, args[1]).start((msg, consumer) -> System.out.println(msg));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                broker.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }
}
