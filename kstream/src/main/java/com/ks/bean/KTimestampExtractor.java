package com.ks.bean;

import com.ks.error.KConfigException;
import static com.ks.process.KUtils.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.processor.TimestampExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * custom time extractor
 */
public class KTimestampExtractor implements TimestampExtractor {

    private final Logger logger = LoggerFactory.getLogger(KTimestampExtractor.class);

    private KTime kTime;

    KTimestampExtractor(KTime kTime) {
        this.kTime = kTime;
    }

    /**
     * Extracts a timestamp from a record. The timestamp must be positive to be considered a valid timestamp.
     * Returning a negative timestamp will cause the record not to be processed but rather silently skipped.
     * In case the record contains a negative timestamp and this is considered a fatal error for the application,
     * throwing a {@link RuntimeException} instead of returning the timestamp is a valid option too.
     * For this case, Streams will stop processing and shut down to allow you investigate in the root cause of the
     * negative timestamp.
     * <p>
     * The timestamp extractor implementation must be stateless.
     * <p>
     * The extracted timestamp MUST represent the milliseconds since midnight, January 1, 1970 UTC.
     * <p>
     * It is important to note that this timestamp may become the message timestamp for any messages sent to changelogs
     * updated by {@link KTable}s and joins.
     * The message timestamp is used for log retention and log rolling, so using nonsensical values may result in
     * excessive log rolling and therefore broker performance degradation.
     *
     * @param record            a data record
     * @param previousTimestamp the latest extracted valid timestamp of the current record's partition˙ (could be -1 if unknown)
     * @return the timestamp of the record
     */
    @Override
    public long extract(ConsumerRecord<Object, Object> record, long previousTimestamp) {
        String field = kTime.getName();
        try {
            Map val = KJson.readValue((String) record.value());
            if (val.containsKey(field)) {
                return kTime.getTimeStamp((String) val.get(field));
            } else {
                throw new KConfigException(concat(" ", "custom time", field, "does not exit..."));
            }
        } catch (IOException | DateTimeParseException | ArithmeticException e) {
            logger.error(concat("==>", (String) record.value(), field), e);
        }
        return 0L;
    }
}
