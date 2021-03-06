package com.pattersonconsultingtn.kafka.examples.PollutionDataTracker;


import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 Quick Start
 # Start Zookeeper. Since this is a long-running service, you should run it in its own terminal.
 $ ./bin/zookeeper-server-start ./etc/kafka/zookeeper.properties
 # Start Kafka, also in its own terminal.
 $ ./bin/kafka-server-start ./etc/kafka/server.properties
 # Start the Schema Registry, also in its own terminal.
 ./bin/schema-registry-start ./etc/schema-registry/schema-registry.properties
 // Create topic in Kafka
 ./bin/kafka-topics --create --zookeeper localhost:2181 --replication-factor 1 \
 --partitions 1 --topic PT08S1
 // Run the producer from maven
 mvn exec:java -Dexec.mainClass="com.pattersonconsultingtn.kafka.examples.PollutionDataTracker.PollutionSensorProducer" \
 -Dexec.args="10 http://localhost:8081"
 // check topic for entries
 ./bin/kafka-avro-console-consumer --zookeeper localhost:2181 --topic air_quality_sensors_topic --from-beginning
 */

public class PollutionSensorProducer {

    private static final String CSV_FILE_PATH = "/Users/paulharris/code/kafka_streaming_sensor_analysis/src/main/resources/AirQualityUCI.csv";

    public static void main(String[] args){

        //Props
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("key.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
        props.put("value.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
        props.put("schema.registry.url", "http://localhost:8081");

        //Map of CSV Column titles to Sensor Ids.
        Map<String, String> sensorIds = new HashMap<String, String>();
        sensorIds.put("PT08S1", "PT08.S1(CO)");
        sensorIds.put("NMHC", "NMHC(GT)");
        sensorIds.put("PT08S2", "PT08.S2(NMHC)");
        sensorIds.put("NOx", "NOx(GT)");
        sensorIds.put( "PT08S3", "PT08.S3(NOx)");
        sensorIds.put("NO2", "NO2(GT)");
        sensorIds.put("PT08S4", "PT08.S4(NO2)");
        sensorIds.put("PT08S5", "PT08.S5(O3)");

        //Sensor Readings Schema
        String schemaString = "{\"namespace\": \"example.avro\", " +
        "\"type\": \"record\", " +
        "\"name\": \"sensosReadingSchema\"," +
        "\"fields\": [" +
        "{\"name\": \"date\", \"type\": \"string\"}," +
        "{\"name\": \"time\", \"type\": \"string\"}," +
        "{\"name\": \"sensor\", \"type\": \"string\"}," +
        "{\"name\": \"reading\", \"type\": \"double\"}" +
        "]}";

        //Kakfa Producer - Key - String, Value - GenericRecord
        Producer<String, GenericRecord> producer = new KafkaProducer<String, GenericRecord>(props);
        Schema.Parser parser = new Schema.Parser();
        Schema schema = parser.parse( schemaString );

        //BefferReader and CSVParser for reading data.
        Reader reader = null;
        CSVParser csvParser = null;

        try {
            reader = Files.newBufferedReader(Paths.get(CSV_FILE_PATH));
            csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                                      .withFirstRecordAsHeader());

            for (CSVRecord csvRecord : csvParser) {
              for(String key: sensorIds.keySet()){
                String topicName = key;
                String date = csvRecord.get("Date");
                String time = csvRecord.get("Time");
                String reading = csvRecord.get(sensorIds.get(key));

                GenericRecord reading_record = new GenericData.Record(schema);
                reading_record.put("date", date);
                reading_record.put("time", time);
                reading_record.put("sensor", key);
                reading_record.put("reading", Double.parseDouble(reading));

                ProducerRecord<String, GenericRecord> data = new ProducerRecord<String, GenericRecord>( topicName, null, reading_record );
                producer.send(data);
                System.out.println(data);
              }
            }
        }catch(Exception e) {
            System.out.println(e);
        }
        producer.close();
    }

}
