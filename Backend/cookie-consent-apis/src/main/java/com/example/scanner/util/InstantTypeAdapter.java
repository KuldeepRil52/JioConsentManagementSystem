package com.example.scanner.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;

public class InstantTypeAdapter extends TypeAdapter<Instant> {

    @Override
    public void write(JsonWriter out, Instant value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.toString()); // ISO-8601 format (e.g. 2025-09-25T12:34:56Z)
        }
    }

    @Override
    public Instant read(JsonReader in) throws IOException {
        if (in.peek() == null) {
            in.nextNull();
            return null;
        }
        return Instant.parse(in.nextString());
    }
}
