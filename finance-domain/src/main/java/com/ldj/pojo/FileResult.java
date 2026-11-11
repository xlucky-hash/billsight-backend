package com.ldj.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileResult {
    private String objectKey;
    private String storageType;
    private Long size;
}
