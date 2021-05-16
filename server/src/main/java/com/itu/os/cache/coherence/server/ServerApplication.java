package com.itu.os.cache.coherence.server;

import com.itu.os.cache.coherence.configuration.AdminConfiguration;
import com.itu.os.cache.coherence.kafka.KafkaConsumerClient;
import com.itu.os.cache.coherence.kafka.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

import javax.annotation.PostConstruct;
import java.io.File;

@ComponentScan({"com.itu.os.cache.coherence.*"})
@SpringBootApplication
@EnableConfigurationProperties
public class ServerApplication {
	private static final Logger log = LoggerFactory.getLogger(ServerApplication.class);

	@Autowired
	private KafkaConsumerClient kafkaConsumerClient;

	public static void main(String[] args) {

		SpringApplication.run(ServerApplication.class, args);
		File directory = new File(AdminConfiguration.CLIENT_FILES_ROOT_DIR);
		directory.mkdirs();
		if(!directory.exists()){
			log.info("directory not available : {}");
		}
		log.info(directory.getAbsolutePath());

	}

	@PostConstruct
	public void listenToKafkaQueue(){
		// start kafka consumer thread
		log.info("========== Kafka consumer client is started =============");
		Thread thread = new Thread(() -> {
			kafkaConsumerClient.consumeMessage();
		});
		thread.start();
	}

}
