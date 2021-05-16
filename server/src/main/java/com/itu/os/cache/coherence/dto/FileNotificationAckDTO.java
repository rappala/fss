package com.itu.os.cache.coherence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileNotificationAckDTO {

    String clientName;
    String fileName;
    String status;
    boolean isDelete;

}
