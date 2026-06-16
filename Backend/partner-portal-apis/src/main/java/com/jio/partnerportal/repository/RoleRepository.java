package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RoleRepository extends MongoRepository<Role, String> {
}
