package com.jio.partnerportal.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User extends AbstractEntity {

    @Id
    private ObjectId id;
    private String userId;
    private String username;
    private String email;
    private String mobile;
    private String totpSecret;
    private String identityType;
    private List<Role> roles;
    private String designation;



    // ================= INNER CLASSES =================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Role {
        private String roleId;
        private String businessId;

    }

}
