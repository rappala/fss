package com.itu.os.cache.coherence.kafka;

import com.itu.os.cache.coherence.configuration.AdminConfiguration;
import com.itu.os.cache.coherence.dto.FileUploadDTO;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class Producer {

    Logger log = LoggerFactory.getLogger(Producer.class);

    @Value("${kafka.bootstrap.servers}")
    private String kafkaServer;

    private KafkaProducer<String, String> kafkaProducer;

    public Producer(){
        Properties producerProperties = new Properties();
        producerProperties.put("bootstrap.servers", AdminConfiguration.KAFKA_SERVER);
        producerProperties.put("acks", "all");
        producerProperties.put("retries", 0);
        producerProperties.put("batch.size", 16384);
        producerProperties.put("linger.ms",1);
        producerProperties.put("buffer.memory",33554432);
        producerProperties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProperties.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");

        kafkaProducer  = new KafkaProducer<String, String>(producerProperties);
    }


    public void sendKafkaMessage(FileUploadDTO kafkaMessageDTO){
        log.info("A file {} is posted to topic {}", kafkaMessageDTO.getFileName(), AdminConfiguration.KAFKA_TOPIC_MSG_TO_CLIENTS);
        JSONObject jsonPayload = new JSONObject(kafkaMessageDTO);
        kafkaProducer.send(new ProducerRecord<>(AdminConfiguration.KAFKA_TOPIC_MSG_TO_CLIENTS, jsonPayload.toString()));
    }

}
