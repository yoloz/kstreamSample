package kafka

import java.util
import java.util.Properties

import kafka.admin.{AdminOperationException, AdminUtils, RackAwareMode}
import kafka.server.ConfigType
import kafka.utils.ZkUtils
import kafka.utils.ZkUtils.getDeleteTopicPath
import org.apache.kafka.common.internals.Topic
import org.apache.kafka.common.security.JaasUtils
import org.apache.zookeeper.KeeperException.NodeExistsException
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._
import scala.collection.Seq

class KsKaClient private(client: ZkUtils) {

  val logger: Logger = LoggerFactory.getLogger(KsKaClient.getClass)

  def getBrokersWithHost: util.List[String] = getBrokers.values.toList.asJava

  def getTopics: Seq[String] = client.getAllTopics().filterNot(t => Topic.isInternal(t))

  def getBrokers: Map[Int, String] = {
    val brokers = client.getAllBrokersInCluster
    val map: collection.mutable.Map[Int, String] = collection.mutable.Map()
    for (b <- brokers) {
      val builder = new StringBuilder
      val endPoints = b.endPoints
      var first = true
      for (p <- endPoints) {
        if (first) {
          builder append p.host
          first = false
        } else {
          builder append ","
          builder append p.host
        }
      }
      map += (b.id -> builder.toString)
    }
    map.toMap
  }

  def getAllTopics: util.List[util.Map[String, String]] = {
    val list = new util.ArrayList[util.Map[String, String]]
    val topics = getTopics
    for (t <- topics) {
      val map = new util.HashMap[String, String]
      map.put("topic", t)
      val pc = client.getTopicPartitionCount(t).getOrElse(0)
      map.put("partitionCount", pc.toString)
      val rf = client.getReplicasForPartition(t, 0).size
      map.put("replicationFactor", rf.toString)
      val configs = AdminUtils.fetchEntityConfig(client, ConfigType.Topic, t).asScala
      if (configs.nonEmpty) {
        val configsAsString = configs.map { case (k, v) => s"$k=$v" }.mkString(",")
        map.put("configs", configsAsString)
      } else map.put("configs", "")
      val markedForDelete = client.isTopicMarkedForDeletion(t)
      map.put("markDelete", markedForDelete.toString)
      list.add(map)
    }
    list
  }

  def getTopicDetail(topic: String): util.List[util.Map[String, String]] = {
    val list = new util.ArrayList[util.Map[String, String]]
    val mb = getBrokers
    client.getPartitionAssignmentForTopics(Seq(topic)).get(topic) match {
      case Some(partitionAssignment) =>
        val sortedPartitions = partitionAssignment.toSeq.sortBy(_._1)
        for ((partitionId, assignedReplicas) <- sortedPartitions) {
          val map = new util.HashMap[String, String]()
          val inSyncReplicas = client.getInSyncReplicasForPartition(topic, partitionId)
          val leader = client.getLeaderForPartition(topic, partitionId)
          map.put("partition", partitionId.toString)
          map.put("leader", if (leader.isDefined) mb.getOrElse(leader.get,
            leader.get + " 未找到broker") else "none")
          map.put("replicas", idToHost(assignedReplicas, mb))
          map.put("isr", idToHost(inSyncReplicas, mb))
          list.add(map)
        }
      case None =>
    }
    list
  }

  private def idToHost(ids: Seq[Int], brokers: Map[Int, String]): String = {
    val b = new StringBuilder
    var first = true
    for (id <- ids) {
      if (first) {
        b append brokers.getOrElse(id, id + " 未找到broker")
        first = false
      } else {
        b append ","
        b append brokers.getOrElse(id, id + " 未找到broker")
      }
    }
    b.toString
  }

  def deleteTopic(topic: String, isPrefix: Boolean = false) {
    val topics = if (isPrefix) getTopics.filter(t => t.startsWith(topic)) else
      getTopics.filter(t => t.contentEquals(topic))
    if (topics.isEmpty) logger.warn("there is no topics to delete")
    else topics.foreach { topic =>
      try {
        client.createPersistentPath(getDeleteTopicPath(topic))
        logger.info("Topic %s is marked for deletion.".format(topic))
        logger.info("Note: This will have no impact if delete.topic.enable is not set to true.")
      } catch {
        case _: NodeExistsException =>
          logger.warn("Topic %s is already marked for deletion.".format(topic))
        case e: AdminOperationException =>
          throw e
        case _: Throwable =>
          throw new AdminOperationException("Error while deleting topic %s".format(topic))
      }
    }
  }

  //  cleanup.policy
  //  compression.type
  //  delete.retention.ms
  //  file.delete.delay.ms
  //  max.message.bytes
  //  min.cleanable.dirty.ratio
  //  min.compaction.lag.ms
  //  min.insync.replicas
  //  retention.bytes
  //  retention.ms
  //  segment.bytes
  //  segment.index.bytes
  //  segment.jitter.ms
  //  segment.ms
  //  unclean.leader.election.enable

  def createTopic(topic: String, partition: Int = 1, replica: Int = 1, configs: Properties,
                  rackAware: Boolean = true) {
    val rackAwareMode = if (rackAware) RackAwareMode.Disabled else RackAwareMode.Enforced
    AdminUtils.createTopic(client, topic, partition, replica, configs, rackAwareMode)
  }

  def close(): Unit = client.close()

}

object KsKaClient {

  def apply(zkUrl: String): KsKaClient = {
    val client = ZkUtils(zkUrl, 30000, 30000, JaasUtils.isZkSecurityEnabled)
    new KsKaClient(client)
  }

}
