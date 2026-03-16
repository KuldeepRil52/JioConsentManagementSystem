package com.example.scanner.mapper;

import com.example.scanner.dto.CookieDto;
import com.example.scanner.entity.CookieEntity;
import com.example.scanner.enums.SameSite;
import com.example.scanner.enums.Source;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScanResultMapper {

    public static CookieEntity cookieDtoToEntity(CookieDto dto) {
        return new CookieEntity(
                dto.getName(),
                dto.getUrl(),
                dto.getDomain(),
                dto.getPath(),
                dto.getExpires(),
                dto.isSecure(),
                dto.isHttpOnly(),
                dto.getSameSite() != null
                        ? SameSite.valueOf(dto.getSameSite().name())
                        : null,
                dto.getSource() != null
                        ? Source.valueOf(dto.getSource().name())
                        : null,
                dto.getCategory(),
                dto.getDescription(),
                dto.getDescription_gpt(),
                dto.getSubdomainName() != null ? dto.getSubdomainName() : "main",
                dto.getPrivacyPolicyUrl(),
                dto.getProvider()
        );
    }
}