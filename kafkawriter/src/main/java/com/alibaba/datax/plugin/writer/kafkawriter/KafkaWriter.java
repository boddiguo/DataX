package com.alibaba.datax.plugin.writer.kafkawriter;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import com.alibaba.datax.common.element.Column;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordReceiver;
import com.alibaba.datax.common.spi.Writer;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

//TODO writeProxy
public class KafkaWriter extends Writer {

    public static class Job extends Writer.Job {
        private Configuration originalConfig = null;

        @Override
        public void preCheck(){
            this.init();
        }

        @Override
        public void init() {
            this.originalConfig = super.getPluginJobConf();
        }

        // 一般来说，是需要推迟到 task 中进行pre 的执行（单表情况例外）
        @Override
        public void prepare() {
            //实跑先不支持 权限 检验
        }

        @Override
        public List<Configuration> split(int mandatoryNumber) {
            return Collections.singletonList(this.originalConfig);
        }

        // 一般来说，是需要推迟到 task 中进行post 的执行（单表情况例外）
        @Override
        public void post() {

        }

        @Override
        public void destroy() {

        }

    }

    public static class Task extends Writer.Task {
        private Configuration writerSliceConfig;

        private List<Producer<String, String>> producers;
        private Integer batchSize;
        private String toTableName;
        private String writeMode;
        private List<Object> columns;
        private String topic;

        @Override
        public void init() {
            this.writerSliceConfig = super.getPluginJobConf();
            this.producers  = createProducer(writerSliceConfig);
            this.batchSize = writerSliceConfig.getInt("batchSize", 1024);
            this.toTableName = writerSliceConfig.getString("toTableName");
            this.columns = writerSliceConfig.getList("column");
            this.topic = writerSliceConfig.getString("topic");
            this.writeMode = writerSliceConfig.getString("writeMode", "insert");

        }

        @Override
        public void prepare() {

        }

        //TODO 改用连接池，确保每次获取的连接都是可用的（注意：连接可能需要每次都初始化其 session）
        public void startWrite(RecordReceiver recordReceiver) {

            List<Record> writeBuffer = new ArrayList<Record>(this.batchSize);
            int bufferBytes = 0;
            try {
                Record record;

                while ((record = recordReceiver.getFromReader()) != null) {
                    writeBuffer.add(record);
                    bufferBytes += record.getMemorySize();

                    if (writeBuffer.size() >= batchSize || bufferBytes >= batchSize) {
                        sendData(writeBuffer);
                        writeBuffer.clear();
                        bufferBytes = 0;
                    }
                }
                if (!writeBuffer.isEmpty()) {
                    sendData(writeBuffer);
                    writeBuffer.clear();
                }

            } catch (Exception e) {
                throw DataXException.asDataXException(
                    KafkaErrorCode.WRITE_DATA_ERROR, e);
            } finally {
                for (Producer<String, String> producer : producers) {
                    producer.flush();
                }
            }

        }

        private void sendData(List<Record>  writeBuffer) {
            JSONObject json = new JSONObject();
            JSONArray array = new JSONArray();

            for (Record record : writeBuffer) {
                JSONObject dataJson = new JSONObject();
                for (int i = 0; i < record.getColumnNumber(); i++) {
                    Column column = record.getColumn(i);
                    dataJson.put(columns.get(i).toString(), JSON.parseObject(column.toString()));
                }
                array.add(dataJson);
            }
            json.put("data", array);
            json.put("toTableName", toTableName);
            json.put("writeMode", writeMode);
            json.put("timestamp", Calendar.getInstance().getTimeInMillis());

            for (Producer<String, String> producer : producers) {
                producer.send(new ProducerRecord<String, String>(topic,
                    "datax", json.toJSONString()));
            }
        }


        @Override
        public void post() {


        }

        @Override
        public void destroy() {
            for (Producer<String, String> producer : producers) {
                producer.close();
            }
        }

        @Override
        public boolean supportFailOver(){
            return false;
        }

        private List<Producer<String, String>> createProducer(Configuration writerSliceConfig) {

            List<Object> connConfs = writerSliceConfig.getList("connection");
            List<Producer<String, String>> producers = new ArrayList(connConfs.size());
            for (Object connection : connConfs) {
                Configuration connConf = Configuration.from(connection.toString());
                Properties props = new Properties();
                props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                    connConf.get("serverAddress"));
                if (connConf.get(ProducerConfig.BATCH_SIZE_CONFIG) != null) {
                    props.put(ProducerConfig.BATCH_SIZE_CONFIG,
                        connConf.get(ProducerConfig.BATCH_SIZE_CONFIG));
                }
                if (connConf.get(ProducerConfig.ACKS_CONFIG) != null) {
                    props.put(ProducerConfig.ACKS_CONFIG,
                        connConf.get(ProducerConfig.ACKS_CONFIG));
                }
                if (connConf.get(ProducerConfig.RETRIES_CONFIG) != null) {
                    props.put(ProducerConfig.RETRIES_CONFIG,
                        connConf.get(ProducerConfig.RETRIES_CONFIG));
                }
                if (connConf.get(ProducerConfig.LINGER_MS_CONFIG) != null) {
                    props.put(ProducerConfig.LINGER_MS_CONFIG,
                        connConf.get(ProducerConfig.LINGER_MS_CONFIG));
                }
                props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                    StringSerializer.class.getName());
                props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                    StringSerializer.class.getName());
                producers.add(new KafkaProducer(props));

            }
            return producers;


        }


    }


}
