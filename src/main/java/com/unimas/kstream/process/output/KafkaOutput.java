package com.unimas.kstream.process.output;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;


/**
 * output kafka
 */
public class KafkaOutput extends OutPut {

    private final Logger logger = LoggerFactory.getLogger(KafkaOutput.class);

    private final String topic;
    private final String address;
    private KafkaProducer<String, String> producer;


    KafkaOutput(Properties properties) {
        super(properties);
        this.topic = nonNullEmpty(properties, "output.target.kafka.topic");
        this.address = nonNullEmpty(properties, CONFIG.OUT_KAFKA_ADDRESS.getValue());
        this.createProducer();
    }

    private void createProducer() {
        if (producer == null) {
            Properties props = new Properties();
            props.put("bootstrap.servers", address);
            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            producer = new KafkaProducer<>(props);
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
        if (producer == null) this.createProducer();
        producer.send(new ProducerRecord<>(topic, key, value), new DefaultCallBack(key, value, logger));
    }


    private class DefaultCallBack implements Callback {

        private final Logger logger;
        private final String key;
        private final String value;


        DefaultCallBack(String key, String value, Logger logger) {
            this.key = key;
            this.value = value;
            this.logger = logger;
        }

        /**
         * A callback method the user can implement to provide asynchronous handling of request completion. This method will
         * be called when the record sent to the server has been acknowledged. Exactly one of the arguments will be
         * non-null.
         *
         * @param metadata  The metadata for the record that was sent (i.e. the partition and offset). Null if an error
         *                  occurred.
         * @param exception The exception thrown during processing of this record. Null if no error occurred.
         *                  Possible thrown exceptions include:
         *                  <p>
         *                  Non-Retriable exceptions (fatal, the message will never be sent):
         *                  <p>
         *                  InvalidTopicException
         *                  OffsetMetadataTooLargeException
         *                  RecordBatchTooLargeException
         *                  RecordTooLargeException
         *                  UnknownServerException
         *                  <p>
         *                  Retriable exceptions (transient, may be covered by increasing #.retries):
         *                  <p>
         *                  CorruptRecordException
         *                  InvalidMetadataException
         *                  NotEnoughReplicasAfterAppendException
         *                  NotEnoughReplicasException
         *                  OffsetOutOfRangeException
         *                  TimeoutException
         */
        @Override
        public void onCompletion(RecordMetadata metadata, Exception exception) {
            if (metadata == null) logger.error(String.format("%s, %s", key, value), exception);
        }
    }

    /**
     * close impl
     */
    @Override
    public void close() {
        if (producer != null) {
            producer.close();
        }
        producer = null;
    }
}
