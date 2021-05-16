package com.itu.os.cache.coherence.service;

import com.itu.os.cache.coherence.api.FileService;
import com.itu.os.cache.coherence.biz.FileOperationsBO;
import com.itu.os.cache.coherence.configuration.AdminConfiguration;
import com.itu.os.cache.coherence.datastore.DataStore;
import com.itu.os.cache.coherence.dto.FileDTO;
import com.itu.os.cache.coherence.dto.FileServiceAckDTO;
import com.itu.os.cache.coherence.dto.FileUploadDTO;
import com.itu.os.cache.coherence.exception.ServerException;
import com.itu.os.cache.coherence.kafka.KafkaConsumerClient;
import com.itu.os.cache.coherence.kafka.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/fileservice")
public class FileServiceImpl  implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    @Autowired
    private Producer producer;

    @Autowired
    private KafkaConsumerClient kafkaConsumerClient;

    @Autowired
    private FileOperationsBO fileOperationsBO;

    @Override
    @RequestMapping(method = RequestMethod.POST)
    public void upload(@RequestBody FileUploadDTO fileUploadDTO) {
        log.info("A request received to upload file {} ", fileUploadDTO);
        //check if a file service is in progress for the same file
//        if(AdminConfiguration.ackPendingMapping.containsKey(fileUploadDTO.getFileName())){
//            FileServiceAckDTO fileServiceAckDTO = AdminConfiguration.ackPendingMapping.get(fileUploadDTO.getFileName());
//            log.error("File service is in progress for file {} for client {}", fileServiceAckDTO.getFilename(), fileServiceAckDTO);
//            throw new ServerException(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED, "Please try after a while");
//        }

        Set<String> clientHoldingFile = DataStore.INSTANCE.addEntryToFileToClientMapping(fileUploadDTO.getFileName(), fileUploadDTO.getClientName());
        Set<String> clientsListForAckProcessing = new HashSet<>(clientHoldingFile);
        clientsListForAckProcessing.remove(fileUploadDTO.getClientName());
        FileServiceAckDTO fileServiceAckDTO = FileServiceAckDTO.builder().clientName(fileUploadDTO.getClientName())
                .clientURL(fileUploadDTO.getClientAddress())
                .filename(fileUploadDTO.getFileName())
                .payload(fileUploadDTO.getPayload())
                .build();

        if(clientsListForAckProcessing.size() > 0){
//            AdminConfiguration.ackPendingMapping.put(fileUploadDTO.getFileName(), fileServiceAckDTO);
            DataStore.INSTANCE.addAEntryToAckPendingMapping(fileUploadDTO.getFileName(), fileServiceAckDTO);
//            AdminConfiguration.notificationAckPendingMapping.put(fileUploadDTO.getFileName(), clientsListForAckProcessing);
            DataStore.INSTANCE.addEntryToNotificationAckPendingMapping(fileUploadDTO.getFileName(), clientsListForAckProcessing);
            producer.sendKafkaMessage(fileUploadDTO);
        }
        else{
            kafkaConsumerClient.uploadFileToServer(fileServiceAckDTO);
            kafkaConsumerClient.ackClientForFileService(fileServiceAckDTO);
        }
    }

    @Override
    @RequestMapping(method = RequestMethod.GET)
    public FileDTO readFile(@RequestParam String filename, @RequestParam String clientName) {
        log.info("A read request received from a client {} for file {}", clientName, filename);
        String content = fileOperationsBO.readFileContent(AdminConfiguration.CLIENT_FILES_ROOT_DIR + "/" + filename);
        DataStore.INSTANCE.addEntryToFileToClientMapping(filename, clientName);
        return FileDTO.builder().fileName(filename).payload(content).build();
    }

    @Override
    @RequestMapping(method = RequestMethod.DELETE)
    public void delete(@RequestParam String filename, @RequestParam String clientName, @RequestParam String port) {

        if(!DataStore.INSTANCE.containsFile(filename)){
            throw new ServerException(HttpStatus.BAD_REQUEST, "File not present in server");
        }

        log.info("A request has been received from client {} to delete file {}", filename, clientName);
        FileUploadDTO fileUploadDTO = FileUploadDTO.builder().fileName(filename)
                .clientName(clientName)
                .clientAddress(AdminConfiguration.CLIENT_URL_PREFIX + port)
                .isDelete(true)
                .build();

        Set<String> clientHoldingFile = DataStore.INSTANCE.addEntryToFileToClientMapping(fileUploadDTO.getFileName(), fileUploadDTO.getClientName());
        Set<String> clientsListForAckProcessing = new HashSet<>(clientHoldingFile);
        clientsListForAckProcessing.remove(fileUploadDTO.getClientName());

        FileServiceAckDTO fileServiceAckDTO = FileServiceAckDTO.builder().clientName(fileUploadDTO.getClientName())
                .clientURL(fileUploadDTO.getClientAddress())
                .filename(fileUploadDTO.getFileName())
                .isDelete(true)
                .build();

        if(clientsListForAckProcessing.size() > 0){
            DataStore.INSTANCE.addAEntryToAckPendingMapping(fileUploadDTO.getFileName(), fileServiceAckDTO);
            DataStore.INSTANCE.addEntryToNotificationAckPendingMapping(fileUploadDTO.getFileName(), clientsListForAckProcessing);
            producer.sendKafkaMessage(fileUploadDTO);
        }else{
            fileOperationsBO.deleteFile(filename);
            kafkaConsumerClient.ackClientForFileService(fileServiceAckDTO);
        }
    }
}
