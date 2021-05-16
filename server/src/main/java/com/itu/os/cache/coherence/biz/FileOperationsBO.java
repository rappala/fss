package com.itu.os.cache.coherence.biz;

import com.itu.os.cache.coherence.configuration.AdminConfiguration;
import com.itu.os.cache.coherence.exception.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class FileOperationsBO {

    Logger log = LoggerFactory.getLogger(FileOperationsBO.class);

    public String readFileContent(String filePath){
        StringBuilder builder = new StringBuilder();
        try(Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)){
            stream.forEach( s-> builder.append(s).append("\n"));
        }catch(IOException ex){
            log.error("There is an error while reading file {} and the exception {}", filePath, ex);
            throw new ServerException(HttpStatus.INTERNAL_SERVER_ERROR, "There is an error while reading file");
        }
        return builder.toString();
    }

    public void deleteFile(String filename){
        File directory = new File(AdminConfiguration.CLIENT_FILES_ROOT_DIR);
        List<File> files = Arrays.asList(directory.listFiles());
        files.forEach(f -> log.info(f.getName()));
        Optional<File> matchedFile = files.stream().filter(f -> f.getName().equals(filename)).findFirst();
        if(matchedFile.isPresent()){
            if(matchedFile.get().delete()){
                log.info("File {} has been deleted", filename);
            }else{
                log.error("There is a prb deleting file {} from client dir, please delete manually", filename);
                throw new ServerException(HttpStatus.INTERNAL_SERVER_ERROR, "Please delete file manually");
            }
        }
        log.info("The requested file is not present in the server");
    }
}
