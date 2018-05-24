package com.unimas.kska.process.output;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * for test
 * sys.out or log
 */
class SysOut extends OutPut {

    private final Logger logger = LoggerFactory.getLogger(SysOut.class);

    SysOut(Properties properties) {
        super(properties);
    }

    /**
     * 输出到目标库
     *
     * @param key   key
     * @param value value
     */
    @Override
    void apply(String key, String value) {
        logger.info(String.format("%s, %s", key, value));
    }
}
