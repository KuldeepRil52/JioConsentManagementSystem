package com.jio.consent.repository;

import com.jio.consent.entity.Purpose;

import java.util.List;

public interface PurposeRepository {

    List<Purpose> findByPurposeIds(List<String> purposeIds);
}
