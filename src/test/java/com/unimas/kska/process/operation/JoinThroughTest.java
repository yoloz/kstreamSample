package com.unimas.kska.process.operation;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.processor.StreamPartitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.Random;

/**
 *
 */
public class JoinThroughTest {

    final static Logger logger = LoggerFactory.getLogger(JoinThroughTest.class);

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, String.valueOf(new Random().nextInt(100000)));
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "10.68.23.11:9092");
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // setting offset reset to earliest so that we can re-run the demo code with the same pre-loaded data
        // Note: To re-run the demo, you need to use the offset reset tool:
        // https://cwiki.apache.org/confluence/display/KAFKA/Kafka+Streams+Application+Reset+Tool
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        KStreamBuilder kStreamBuilder = new KStreamBuilder();
        KStream<String, String> partion1 = kStreamBuilder.stream("test1");
        KStream<String, String> partion2 = kStreamBuilder.stream("test2");
//        partion2.join(partion1, String::concat,
//                JoinWindows.of(0).after(120_000).until(120_001),
//                Serdes.String(), Serdes.String(), Serdes.String()).foreach((k, v) -> {
//            System.out.printf("key: %s,value:%s\n", k, v);
//        });
        partion2.through(Serdes.String(), Serdes.String(), new StreamPartitioner<String, String>() {
            @Override
            public Integer partition(String key, String value, int numPartitions) {
//                logger.info("========" + numPartitions);
                return null;
            }
        }, "test-1").join(partion1, String::concat,
                JoinWindows.of(0).after(3600_000).until(3600_001),
                Serdes.String(), Serdes.String(), Serdes.String()).foreach((k, v) -> {
            logger.info("key: " + k + ",value:" + v + "\n");
        });
        KafkaStreams streams = new KafkaStreams(kStreamBuilder, props);
        try {
            streams.start();
            while (true) {
                Thread.sleep(5000);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        } finally {
            streams.close();
        }

    }
}
