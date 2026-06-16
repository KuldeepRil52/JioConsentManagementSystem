package com.jio.multitranslator.repository;

import com.jio.multitranslator.entity.TranslateToken;
import com.jio.multitranslator.entity.TranslateTranscation;

public interface TranslateRepository{

    TranslateToken getTokenFromDB(String sourceLanguage, String targetLanguage, String businessId);

    /**
     * Saves a token and evicts the cache entry for this token.
     */
    TranslateToken save(TranslateToken storeToken);

    void saveTranslate(TranslateTranscation saveTranslate);

}
