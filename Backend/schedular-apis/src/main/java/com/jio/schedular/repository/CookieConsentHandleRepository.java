package com.jio.schedular.repository;

import com.jio.schedular.entity.CookieConsentHandle;

import java.time.Instant;
import java.util.List;

public interface CookieConsentHandleRepository {

    List<CookieConsentHandle> findExpiredPendingHandles(Instant currentTime);

    int markHandlesAsExpired(List<String> handleIds, Instant updatedAt);
}