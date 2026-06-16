package com.jio.consent.repository;

import com.jio.consent.entity.Template;

import java.util.List;
import java.util.Map;

public interface TemplateRepository {

    Template saveTemplate(Template template);

    Template getByTemplateId(String templateId);

    List<Template> findTemplatesByParams(Map<String, Object> searchParams);

    long count();
}
