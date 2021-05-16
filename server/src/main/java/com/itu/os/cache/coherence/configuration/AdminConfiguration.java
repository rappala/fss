package com.itu.os.cache.coherence.configuration;

import com.itu.os.cache.coherence.dto.FileServiceAckDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdminConfiguration {
    public static final String CLIENT_FILES_ROOT_DIR  = "/local/ServerFiles";
    public static final String KAFKA_SERVER = "localhost:9092";
    public static final String KAFKA_TOPIC_MSG_TO_CLIENTS = "server.to.clients";
    public static final String KAFKA_TOPIC_ACK_FROM_CLIENTS = "clients.to.server";
    public static final String ZOOKEEPER_GROUP_ID_ACK_FROM_CLIENTS = "clients.to.server";
    public static final long KAFKA_QUEUE_POLL_TIME =  5000;


    public static final String FILE_SERVICE_SUCCESS_STATUS = "success";

    public static final String CLIENT_FILE_SERVICE_CALL_BACK_API = "/callback";

    public static final String CLIENT_URL_PREFIX = "http://localhost:";
}
