package com.itu.os.cache.coherence.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.itu.os.cache.coherence.biz.FileOperationsBO;
import com.itu.os.cache.coherence.configuration.AdminConfiguration;
import com.itu.os.cache.coherence.datastore.DataStore;
import com.itu.os.cache.coherence.dto.FileNotificationAckDTO;
import com.itu.os.cache.coherence.dto.FileServiceAckDTO;
import com.itu.os.cache.coherence.exception.ServerException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Component
public class KafkaConsumerClient {

    Logger log = LoggerFactory.getLogger(KafkaConsumerClient.class);

    private KafkaConsumer<String, String> kafkaConsumer;

    @Autowired
    private FileOperationsBO fileOperationsBO;

    public KafkaConsumerClient(){
        Properties consumerProperties = new Properties();
        consumerProperties.put("bootstrap.servers", AdminConfiguration.KAFKA_SERVER);
        consumerProperties.put("group.id", AdminConfiguration.ZOOKEEPER_GROUP_ID_ACK_FROM_CLIENTS);
        consumerProperties.put("zookeeper.session.timeout.ms", "6000");
        consumerProperties.put("zookeeper.sync.time.ms","2000");
        consumerProperties.put("auto.commit.enable", "false");
        consumerProperties.put("auto.commit.interval.ms", "1000");
        consumerProperties.put("consumer.timeout.ms", "-1");
        consumerProperties.put("max.poll.records", "1");
        consumerProperties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProperties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        kafkaConsumer = new KafkaConsumer<String, String>(consumerProperties);
        kafkaConsumer.subscribe(Arrays.asList(AdminConfiguration.KAFKA_TOPIC_ACK_FROM_CLIENTS));
    }

    public void consumeMessage(){

        while(true){
            ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(AdminConfiguration.KAFKA_QUEUE_POLL_TIME);
            for(ConsumerRecord<String, String> record : consumerRecords){
                ObjectMapper mapper = new ObjectMapper();
                try {
                    FileNotificationAckDTO fileNotificationAckDTO = mapper.readValue(record.value(), FileNotificationAckDTO.class);
                    log.info("New File received {}", fileNotificationAckDTO);
                    processAckFromClient(fileNotificationAckDTO);
                } catch (JsonProcessingException e) {
                    log.error("There is an error parsing record {} and the exception {}", record.value(), e);
                } catch (IOException e) {
                    log.error("There is an exception while parsing a record {} and the exception {}", record.value(), e);
                }
                {
                    Map<TopicPartition, OffsetAndMetadata> commitMessage = new HashMap<>();
                    commitMessage.put(new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1));

                    kafkaConsumer.commitSync(commitMessage);

                    log.info("Offset committed to Kafka.");
                }
            }
        }
    }

    private synchronized void processAckFromClient(FileNotificationAckDTO fileNotificationAckDTO){
        try{
            String fileName = fileNotificationAckDTO.getFileName();
            DataStore.INSTANCE.removeEntryFromNotificationAckPendingMapping(fileName, fileNotificationAckDTO.getClientName());

            if(DataStore.INSTANCE.sizeOfNotificationAckPendingMapping(fileName) == 0){
                FileServiceAckDTO fileServiceAckDTO = DataStore.INSTANCE.removeEntryFromAckPendingMapping(fileName);
                if(!fileNotificationAckDTO.isDelete()){
                    uploadFileToServer(fileServiceAckDTO);
                }else{
                    fileOperationsBO.deleteFile(fileName);
                }
                ackClientForFileService(fileServiceAckDTO);
                if(fileNotificationAckDTO.isDelete()){
                    DataStore.INSTANCE.removeEntryFromFileToClientMapping(fileName);
                }
            }
        }catch(Exception e){
            log.error("Exception happened while processing notification ack from client and the notification {} and the exception {}", fileNotificationAckDTO, e);
        }
    }

    public void uploadFileToServer(FileServiceAckDTO  fileServiceAckDTO){
        String filename = fileServiceAckDTO.getFilename();
        try {
            File directory = new File(AdminConfiguration.CLIENT_FILES_ROOT_DIR);
            List<File> files = Arrays.asList(directory.listFiles());
            files.forEach(f -> log.info(f.getName()));
            Optional<File> matchedFile = files.stream().filter(f -> f.getName().equals(fileServiceAckDTO.getFilename())).findFirst();
            if(matchedFile.isPresent()){
                replaceFileContent(matchedFile.get(), fileServiceAckDTO);
            }else{
                log.info("A new file {} is being uploaded to server", fileServiceAckDTO.getFilename());
                File newFile = new File(AdminConfiguration.CLIENT_FILES_ROOT_DIR + "/" + fileServiceAckDTO.getFilename());
                if(!newFile.createNewFile()){
                    log.error("Unable to create a new file, check permission settings of dir");
                    throw new ServerException(HttpStatus.INTERNAL_SERVER_ERROR, "unable to upload file");
                }
                Files.write(fileServiceAckDTO.getPayload().getBytes(), newFile);
                return;
            }
        } catch (IOException e) {
            log.error("There is an error persisting the file {} ", filename, e);
        }

    }

    private void replaceFileContent(File resource, FileServiceAckDTO fileServiceAckDTO) throws IOException {
        byte[] empty = new byte[0];
        Files.write(empty, resource);
        if((resource.length() > 0)){
            log.error("Unable to delete file {}", fileServiceAckDTO.getFilename());
            throw new ServerException(HttpStatus.INTERNAL_SERVER_ERROR, "unable to delete the file content");
        }
        Files.write(fileServiceAckDTO.getPayload().getBytes(), resource);
    }

    public void ackClientForFileService(FileServiceAckDTO fileServiceAckDTO){
        String fileName = fileServiceAckDTO.getFilename();
        try{
            fileServiceAckDTO.setStatus(AdminConfiguration.FILE_SERVICE_SUCCESS_STATUS);
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            JSONObject fileServiceAckPayload = new JSONObject(fileServiceAckDTO);
            String clientCallBackURL = fileServiceAckDTO.getClientURL() + AdminConfiguration.CLIENT_FILE_SERVICE_CALL_BACK_API;
            log.info("Client callback URL {}", clientCallBackURL);
            HttpEntity<String> httpEntity = new HttpEntity<>(fileServiceAckPayload.toString(), httpHeaders);
            ResponseEntity<String> responseEntity = restTemplate.exchange(clientCallBackURL, HttpMethod.POST, httpEntity, String.class);

            if(responseEntity.getStatusCode().equals(HttpStatus.OK)){
                log.info("Client has been updated successfully with upload status for file {}", fileName);
            }else{
                log.error("Client has not been notified, status code {} and due to {}" , responseEntity.getStatusCode().toString(), responseEntity);
            }
        }catch(Exception e ){
            log.error("There is an error while notifying client about the file {} upload status and the exception e ", fileName, e);
        }
    }
}
