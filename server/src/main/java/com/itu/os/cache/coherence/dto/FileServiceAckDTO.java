package com.itu.os.cache.coherence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileServiceAckDTO {
    private String filename;
    private String clientURL;
    private String status;
    private String clientName;
    private String payload;
    private boolean isDelete;
}
