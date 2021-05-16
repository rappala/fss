package com.itu.os.cache.coherence.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileUploadDTO {

    private String fileName;
    private String payload;
    private String clientName;
    private String clientAddress;
    private boolean isDelete;
}
