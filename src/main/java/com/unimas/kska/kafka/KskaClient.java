package com.unimas.kska.kafka;

import com.google.common.collect.ImmutableList;
import kafka.admin.AdminUtils;
import kafka.cluster.Broker;
import kafka.cluster.EndPoint;
import kafka.server.ConfigType;
import kafka.utils.ZkUtils;
import org.apache.kafka.common.internals.Topic;
import org.apache.kafka.common.security.JaasUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;
import scala.collection.JavaConversions;
import scala.collection.JavaConverters;
import scala.collection.Seq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class KskaClient {

    private final Logger logger = LoggerFactory.getLogger(KskaClient.class);

    private final ZkUtils zkClient;

    public KskaClient(String zkUrl) {
        this.zkClient = ZkUtils.apply(zkUrl, 30000, 30000,
                JaasUtils.isZkSecurityEnabled());
    }

    public void close() {
        if (zkClient != null) zkClient.close();
    }

    public void createTopic(String topic) {
        createTopic(topic, 1, 1, new Properties());
    }

    public void createTopic(String topic, int partition, int replica, Properties configs) {
        AdminUtils.createTopic(zkClient, topic, partition, replica, configs, null);
    }

    private Map<Integer, String> getBrokers() {
        List<Broker> brokers = JavaConversions.seqAsJavaList(zkClient.getAllBrokersInCluster());
        Map<Integer, String> _m = new HashMap<>(brokers.size());
        for (Broker b : brokers) {
            StringBuilder builder = new StringBuilder();
            List<EndPoint> endPoints = JavaConversions.seqAsJavaList(b.endPoints());
            for (int i = 0; i < endPoints.size(); i++) {
                EndPoint p = endPoints.get(i);
                builder.append(p.host());
                if (i != endPoints.size() - 1) builder.append(",");
            }
            _m.put(b.id(), builder.toString());
        }
        return _m;
    }

    public List<String> getTopics() {
        List<String> list = new ArrayList<>();
        Collection<String> _all = JavaConverters.asJavaCollectionConverter(zkClient.getAllTopics()).asJavaCollection();
        _all.forEach(t -> {
            if (!Topic.isInternal(t)) list.add(t);
        });
        return list;
    }

    public List<Map<String, String>> getAllTopics() {
        List<Map<String, String>> list = new ArrayList<>();
        List<String> topics = getTopics();
        for (String t : topics) {
            Map<String, String> map = new HashMap<>();
            map.put("topic", t);
            Option<Object> _pc = zkClient.getTopicPartitionCount(t);
            int pc = _pc.isDefined() ? (int) _pc.get() : 0;
            map.put("partitionCount", pc + "");
            int rf = zkClient.getReplicasForPartition(t, 0).size();
            map.put("replicationFactor", rf + "");
            Properties configs = AdminUtils.fetchEntityConfig(zkClient, ConfigType.Topic(), t);
            if (configs.isEmpty()) map.put("configs", "");
            else {
                StringBuilder stringBuilder = new StringBuilder();
                configs.forEach((k, v) -> stringBuilder.append(k).append("=").append(v).append(","));
                map.put("configs", stringBuilder.substring(0, stringBuilder.length() - 1));
            }
            boolean markedForDelete = zkClient.isTopicMarkedForDeletion(t);
            map.put("markDelete", String.valueOf(markedForDelete));
            list.add(map);
        }
        return list;
    }

    public List<Map<String, String>> getTopicDetail(String topic) {
        List<Map<String, String>> list = new ArrayList<>();
        Map<Integer, String> mb = getBrokers();
        Option<scala.collection.Map<Object, Seq<Object>>> _map = zkClient.getPartitionAssignmentForTopics(
                JavaConverters.asScalaIteratorConverter(ImmutableList.of(topic).iterator()).asScala().toSeq())
                .get(topic);
        Map<Object, Seq<Object>> map = _map.isDefined() ? JavaConversions.mapAsJavaMap(_map.get()) : Collections.emptyMap();
        if (map != null && !map.isEmpty()) {
            map.forEach((k, v) -> {
                Map<String, String> _m = new HashMap<>(4);
                Seq<Object> inSyncReplicas = zkClient.getInSyncReplicasForPartition(topic, (int) k);
                Option<Object> _leader = zkClient.getLeaderForPartition(topic, (int) k);
                int leader = _leader.isDefined() ? (int) _leader.get() : 0;
                _m.put("partition", String.valueOf(k));
                _m.put("leader", mb.getOrDefault(leader, leader + ""));
                _m.put("replicas", idToHost(v, mb));
                _m.put("isr", idToHost(inSyncReplicas, mb));
                list.add(_m);
            });
        }
        return list;
    }

    private String idToHost(Seq<Object> ids, Map<Integer, String> brokers) {
        StringBuilder b = new StringBuilder();
        Collection<Object> _ids = JavaConverters.asJavaCollectionConverter(ids).asJavaCollection();
        _ids.forEach(id -> b.append(brokers.getOrDefault((int) id, id + "")).append(","));
        return b.substring(0, b.length() - 1);
    }

    public void deleteTopic(final String topic, boolean isPrefix) {
        List<String> topics = new ArrayList<>();
        List<String> _topics = getTopics();
        if (isPrefix) _topics.forEach(t -> {
            if (t.startsWith(topic)) topics.add(t);
        });
        else _topics.forEach(t -> {
            if (t.equals(topic)) topics.add(t);
        });
        if (topics.isEmpty()) logger.warn("there is no topics to delete");
        else topics.forEach(_topic -> {
            try {
                String path = ZkUtils.getDeleteTopicPath(_topic);
                zkClient.createPersistentPath(path, "", zkClient.defaultAcls(path));
                logger.info(String.format("Topic %s is marked for deletion.", _topic));
                logger.info("Note: This will have no impact if delete.topic.enable is not set to true.");
            } catch (Throwable r) {
                logger.error(String.format("Error while deleting topic %s", _topic), r);
            }
        });
    }
}
