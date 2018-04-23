package com.unimas.kstream.kafka;

import kafka.admin.TopicCommand;
import kafka.utils.ZkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.JavaConversions;

import java.util.List;

public class DeleteTopic implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(DeleteTopic.class);

    final private String zkUrls;
    final private String topicPrefix;

    public DeleteTopic(String zkUrls, String topicPrefix) {
        this.zkUrls = zkUrls;
        this.topicPrefix = topicPrefix;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        ZkUtils zkUtils = null;
        try {
            zkUtils = ZkUtils.apply(zkUrls, 30_000, 30_000, false);
            List<String> topics = JavaConversions.seqAsJavaList(zkUtils.getAllTopics());
            StringBuilder topicsBuf = new StringBuilder();
            topics.forEach(topic -> {
                if (topic.startsWith(topicPrefix)) topicsBuf.append(topic).append(",");
            });
            String delTopic = topicsBuf.toString();
            if (!delTopic.isEmpty()) {
                String[] args = new String[]{"--topic", delTopic.substring(0, delTopic.length() - 1)};
                TopicCommand.deleteTopic(zkUtils, new TopicCommand.TopicCommandOptions(args));
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (zkUtils != null) zkUtils.close();
        }
    }
}
