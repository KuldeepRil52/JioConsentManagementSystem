package com.jio.multitranslator.entity;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.jio.multitranslator.dto.Source;
import com.jio.multitranslator.dto.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(value = "translateTranscation")
public class TranslateTranscation {

    @Id
    private ObjectId id;
    @Indexed(unique = true)
    private String txn;
    private String sourceLanguage;
    private String targetLanguage;
    private Status status;
    private String message;
    private Source source;

    public TranslateTranscation(TranslateTranscation other){
        this.sourceLanguage = other.sourceLanguage;
        this.targetLanguage = other.targetLanguage;
        this.txn = other.txn;
        this.status = other.status;
        this.message = other.message;
        this.source = other.source;
    }
}
