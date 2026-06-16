package com.jio.digigov.fides.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Data
public abstract class BaseEntity {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @CreatedDate
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
