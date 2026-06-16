package com.jio.schedular.repository;

import com.jio.schedular.entity.Template;

import java.util.List;
import java.util.Map;

public interface TemplateRepository {

    Template saveTemplate(Template template);

    Template getByTemplateId(String templateId);

    List<Template> findTemplatesByParams(Map<String, Object> searchParams);

    long count();
}
