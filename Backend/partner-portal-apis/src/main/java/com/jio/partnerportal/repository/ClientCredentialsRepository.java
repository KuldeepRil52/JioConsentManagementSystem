package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.ClientCredentials;

import java.util.List;
import java.util.Map;

public interface ClientCredentialsRepository {

    ClientCredentials save(ClientCredentials clientCredentials);

    ClientCredentials findByBusinessId(String businessId);

    ClientCredentials findByBusinessIdAndScopeLevel(String businessId, String scopeLevel);

    ClientCredentials findByBusinessUniqueId(String businessUniqueId);

    ClientCredentials findByConsumerKey(String consumerKey);

    List<ClientCredentials> findClientCredentialsByParams(Map<String, String> searchParams);

    long count();
}

