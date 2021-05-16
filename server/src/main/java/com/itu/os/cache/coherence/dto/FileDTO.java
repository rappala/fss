package com.itu.os.cache.coherence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDTO {
    String fileName;
    String payload;
    String clientURL;
    String clientName;
    
}
