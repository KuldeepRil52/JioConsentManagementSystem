package com.jio.schedular.repository;

import com.jio.schedular.entity.Purpose;

import java.util.List;

public interface PurposeRepository {

    List<Purpose> findByPurposeIds(List<String> purposeIds);
}
