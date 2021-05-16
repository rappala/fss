package com.itu.os.cache.coherence.api;

import com.itu.os.cache.coherence.dto.FileDTO;
import com.itu.os.cache.coherence.dto.FileUploadDTO;
import org.springframework.web.bind.annotation.RequestParam;

public interface FileService {

    public void upload(FileUploadDTO message);

    public FileDTO readFile(String filename, String clientName);

    public void delete(@RequestParam String filename, @RequestParam String clientName, @RequestParam String clientURL);
}
