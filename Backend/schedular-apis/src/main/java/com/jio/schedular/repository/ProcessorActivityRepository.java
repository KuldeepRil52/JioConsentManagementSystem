package com.jio.schedular.repository;

import com.jio.schedular.entity.ProcessorActivity;

import java.util.List;

public interface ProcessorActivityRepository {

    List<ProcessorActivity> findByProcessorActivityIds(List<String> processorActivityIds);
}
