package com.jio.digigov.auditmodule.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of = "id")
public abstract class BaseEntity {

    @Id
    private String id;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @CreatedDate
    private LocalDateTime createdAt;

//    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
//    @LastModifiedDate
//    private LocalDateTime updatedAt;
}
