package com.example.scanner.entity;

import com.example.scanner.enums.SameSite;
import com.example.scanner.enums.Source;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.Instant;

@Data
public class CookieEntity {
  private String name;
  private String url;
  private String domain;
  private String path;
  @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
  private Instant expires;
  private boolean secure;
  private boolean httpOnly;
  private SameSite sameSite;
  private Source source;
  private String category;
  private String description;
  private String description_gpt;
  private String subdomainName;
  private String privacyPolicyUrl;
  private String provider;


  public CookieEntity() {
  }

  public CookieEntity(String name, String url, String domain, String path, Instant expires,
                      boolean secure, boolean httpOnly, SameSite sameSite, Source source,
                      String category, String description, String description_gpt, String subdomainName,
                      String privacyPolicyUrl, String provider) {
    this.name = name;
    this.url = url;
    this.domain = domain;
    this.path = path;
    this.expires = expires;
    this.secure = secure;
    this.httpOnly = httpOnly;
    this.sameSite = sameSite;
    this.source = source;
    this.category = category;
    this.description = description;
    this.description_gpt = description_gpt;
    this.subdomainName = subdomainName;
    this.privacyPolicyUrl = privacyPolicyUrl;
    this.provider = provider;
  }
}