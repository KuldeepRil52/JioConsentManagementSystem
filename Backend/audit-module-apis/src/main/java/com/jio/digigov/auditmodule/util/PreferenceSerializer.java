package com.jio.digigov.auditmodule.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.jio.digigov.auditmodule.dto.Preference;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class PreferenceSerializer implements JsonSerializer<Preference> {

    @Override
    public JsonElement serialize(Preference pref, java.lang.reflect.Type type,
                                 JsonSerializationContext context) {

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("purpose", pref.getPurpose());
        jsonObject.addProperty("isMandatory", pref.getIsMandatory());
        jsonObject.add("preferenceValidity", context.serialize(pref.getPreferenceValidity()));

        // Convert preference start/end to UTC Z
        if (pref.getStartDate() != null) {
            jsonObject.addProperty("startDate",
                    pref.getStartDate().atZone(ZoneId.systemDefault())
                            .withZoneSameInstant(ZoneOffset.UTC)
                            .format(DateTimeFormatter.ISO_INSTANT));
        }

        if (pref.getEndDate() != null) {
            jsonObject.addProperty("endDate",
                    pref.getEndDate().atZone(ZoneId.systemDefault())
                            .withZoneSameInstant(ZoneOffset.UTC)
                            .format(DateTimeFormatter.ISO_INSTANT));
        }

        if (pref.getPreferenceStatus() != null) {
            jsonObject.addProperty("preferenceStatus", pref.getPreferenceStatus().toString());
        }

        return jsonObject;
    }
}