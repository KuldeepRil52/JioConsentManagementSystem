package com.jio.consent.repository;

import com.jio.consent.entity.ProcessorActivity;

import java.util.List;

public interface ProcessorActivityRepository {

    List<ProcessorActivity> findByProcessorActivityIds(List<String> processorActivityIds);
}
