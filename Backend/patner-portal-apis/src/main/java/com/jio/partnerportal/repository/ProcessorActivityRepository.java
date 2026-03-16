package com.jio.partnerportal.repository;

import com.jio.partnerportal.entity.ProcessorActivity;

import java.util.List;
import java.util.Map;

public interface ProcessorActivityRepository {

    ProcessorActivity save(ProcessorActivity dataProcessor);

    ProcessorActivity findByProcessorActivityId(String dataProcessorId);

    List<ProcessorActivity> findByProcessorActivityIds(List<String> processorActivityIds);

    List<ProcessorActivity> findProcessorActivityByParams(Map<String, String> searchParams);

    long count();

    ProcessorActivity findLatestByProcessorActivityId(String dataProcessorId);

    boolean existsByActivityName(String activityName);

    boolean existsByActivityNameExcludingProcessorActivityId(String activityName, String excludeProcessorActivityId);

}
