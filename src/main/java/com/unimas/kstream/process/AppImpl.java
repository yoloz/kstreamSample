package com.unimas.kstream.process;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MapMaker;
import com.unimas.kstream.AppServer;
import com.unimas.kstream.StopProcess;
import com.unimas.kstream.bean.KSource;
import com.unimas.kstream.dic.DicSets;
import com.unimas.kstream.error.KConfigException;
import com.unimas.kstream.kafka.KsKaClient;
import com.unimas.kstream.process.operation.Operation;
import com.unimas.kstream.process.output.OutPut;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.KTable;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

/**
 * 数据格式解析错误等级为WARN,不会关闭server
 * <p>
 * 配置文件格式错误以及不支持的操作会退出server
 * <p>
 * 众操作配置文件需要与main配置文件同一目录下
 */
public class AppImpl extends Thread implements KUtils {

    private final Logger logger = LoggerFactory.getLogger(AppImpl.class);

    private static DicSets dicSets = null;
    private static DB db = DBMaker.memoryDB().make();

    private final Properties mainProperties = new Properties();

    private KafkaStreams kafkaStreams = null;
    private CountDownLatch stop;

    private ConcurrentMap<String, Object> kSources = null;
    private ImmutableList<Properties> operations = null;
    private Properties outputConf = new Properties();
    private OutPut[] outPuts = null;

    private String testAppId = null;

    /**
     * internal call
     * do not use it to init server
     *
     * @param serverFile server property file,has been determined before {@link AppServer}
     * @param stop       quit tag
     * @param isTest     test mode
     */
    public AppImpl(String serverFile, CountDownLatch stop, boolean isTest) {
        this.stop = stop;
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(serverFile), Charset.forName("UTF-8"))) {
            this.mainProperties.load(reader);
        } catch (IOException e) {
            throw new KConfigException(e);
        }
        nonNullEmpty(mainProperties, StreamsConfig.BOOTSTRAP_SERVERS_CONFIG);
        if (isTest) {
            this.testAppId = "KS-TEST" + new Random().nextInt(100000);
            this.mainProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, testAppId);
        }
        nonNullEmpty(mainProperties, StreamsConfig.APPLICATION_ID_CONFIG);
        this.mainProperties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
                Serdes.String().getClass().getName());
        this.mainProperties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
                Serdes.String().getClass().getName());
        this.mainProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                mainProperties.getProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                        "latest"));
       /*  Security settings.
         1. These settings must match the security settings of the secure Kafka cluster.
         2. The SSL trust store and key store files must be locally accessible to the application.
            Typically, this means they would be installed locally in the client machine (or container)
            on which the application runs.  To simplify running this example, however, these files
            were generated and stored in the VM in which the secure Kafka broker is running.  This
            also explains why you must run this example application from within the VM.
        */
//        streamsConfiguration.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
//        streamsConfiguration.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "/etc/security/tls/kafka.client.truststore.jks");
//        streamsConfiguration.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "test1234");
//        streamsConfiguration.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, "/etc/security/tls/kafka.client.keystore.jks");
//        streamsConfiguration.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "test1234");
//        streamsConfiguration.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "test1234");
    }

    /**
     * initialization
     * <p>
     * kSources,operations,output
     */
    private void init(final KStreamBuilder kStreamBuilder) {
        logger.info("#################### parse config file ####################");
        kSources = new MapMaker().makeMap();
        split(nonNullEmpty(mainProperties, "ks.source"), COMMA).forEach(o -> {
            Optional<Properties> p = getProperties(o);
            if (p.isPresent()) {
                KSource kSource = new KSource(p.get(), kStreamBuilder);
                kSources.put(kSource.getKsName(), kSource.source());
            }
        });
        String _operations = mainProperties.getProperty("ks.operation");
        if (!isNullOrEmpty(_operations)) {
            final ImmutableList.Builder<Properties> operatorBuilder = new ImmutableList.Builder<>();
            split(_operations, COMMA).forEach(o -> {
                Optional<Properties> p = getProperties(o);
                if (p.isPresent()) operatorBuilder.add(p.get());
            });
            operations = operatorBuilder.build();
        } else operations = ImmutableList.of();
        Optional<Properties> _out = getProperties(nonNullEmpty(mainProperties, "ks.output"));
        if (_out.isPresent()) outputConf = _out.get();
        else throw new KConfigException("output unconfigured...");
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
    @Override
    public void run() {
        logger.info("#################### kServer start ####################");
        try {
            Files.write(Paths.get(AppServer.app_dir, "pid"), ManagementFactory.getRuntimeMXBean()
                    .getName().split("@")[0].getBytes("UTF-8"));
            final KStreamBuilder builder = new KStreamBuilder();
            init(builder);
            getDic();
            for (Properties p : operations) {
                String opera = nonNullEmpty(p, Operation.operator);
                Operation operation = Operation.getImpl(opera, p);
                logger.info("#################### init " + p.getProperty(Operation.operatorKS)
                        + "==>" + opera + " ####################");
                operation.process(kSources);
            }
            outputStream();
            kafkaStreams = new KafkaStreams(builder, mainProperties);
            kafkaStreams.start();
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            StopProcess.stop(Paths.get(AppServer.app_dir, "pid"));
            System.exit(-1);
        }
    }

    /**
     * 获取dic缓存本地
     */
    private void getDic() {
        Optional<DicSets> optional = DicSets.getImpl(mainProperties.getProperty(DicSets.dicType), mainProperties);
        if (optional.isPresent()) {
            logger.info("#################### start dic ####################");
            DicSets dicSets = optional.get();
            new Thread(dicSets).start();
            while (dicSets.isBlock()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) { //ignore
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * close resources
     */
    public void close() {
        if (kafkaStreams != null) kafkaStreams.close();
        if (outPuts != null) {
            outPuts[0].closeExpandWin();
            for (OutPut outPut : outPuts) {
                outPut.close();
            }
        }
        if (dicSets != null) dicSets.close();
        db.close();
        if (stop != null) stop.countDown();
        String zkUrls = "ks.zookeeper.url";
        if (!isNullOrEmpty(testAppId) && !isNullOrEmpty(mainProperties.getProperty(zkUrls))) {
            KsKaClient client = KsKaClient.apply(mainProperties.getProperty(zkUrls));
            try {
                client.deleteTopic(testAppId);
            } catch (Throwable e) {
                logger.error("delete topics error", e);
            }
            client.close();
        }
        logger.info("#################### kServer stop ####################");
    }

    @SuppressWarnings("unchecked")
    private void outputStream() {
        String _target = outputConf.getProperty(OutPut.CONFIG.OUT_TARGETS.getValue(), "sysout");
        _target = _target.isEmpty() ? "sysout" : _target;
        if (_target.contains("kafka") &&
                isNullOrEmpty(outputConf.getProperty(OutPut.CONFIG.OUT_KAFKA_ADDRESS.getValue()))) {
            outputConf.put(OutPut.CONFIG.OUT_KAFKA_ADDRESS.getValue(), mainProperties.getProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG));
        }
        outPuts = OutPut.getImpl(outputConf, split(_target, COMMA).toArray());
        logger.info("#################### init output " + outputConf.getProperty(OutPut.CONFIG.OUT_KSOURCE.getValue())
                + "==>" + _target + " ####################");
        Object obj = kSources.get(outPuts[0].getOutKSourceName());
        if (obj != null) {
            if (obj instanceof KTable) obj = ((KTable) obj).toStream();
            outPuts[0].output((KStream) obj, outPuts);
        } else throw new KConfigException(concat(" ", OutPut.CONFIG.OUT_TARGETS.getValue(), " is not found..."));
    }

    public static DicSets getDicSets() {
        if (dicSets == null) throw new KConfigException("dic is null(maybe dic not configured)...");
        return dicSets;
    }

    public static DB getMapDb() {
        if (db == null) throw new KConfigException("mapDb is null...");
        return db;
    }

    /**
     * get properties from app_dir by file name
     *
     * @param fileName file name
     * @return properties
     */
    private Optional<Properties> getProperties(String fileName) {
        Path f = Paths.get(AppServer.app_dir, fileName + ".properties");
        if (f.toFile().exists()) {
            Properties p = new Properties();
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(f.toFile()), Charset.forName("UTF-8"))) {
                p.load(reader);
            } catch (IOException e) {
                throw new KConfigException(e);
            }
            return Optional.of(p);
        } else {
            logger.error(concat(" ", f.toString(), "does not exit..."));
        }
        return Optional.absent();
    }
}
