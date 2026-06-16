package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.Otp;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OtpRepository extends MongoRepository<Otp, String> {

    Optional<Otp> findByTxnId(String txnId);

    Optional<Otp> findByIdValue(String IdValue);

    Optional<Otp> findByTxnIdAndIdValue(String txnId, String idValue);

    Optional<Otp> findByOtpTxnIdAndIdValue(String txnId, String idValue);

}


