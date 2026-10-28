package com.garagelog.util;

import com.google.gson.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class JsonUtil {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, type, context) ->
                    src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.format(DATE_FORMATTER)))
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, type, context) -> {
                if (json == null || json.isJsonNull() || json.getAsString().isBlank()) {
                    return null;
                }
                try {
                    return DateUtil.parse(json.getAsString());
                } catch (Exception e) {
                    throw new JsonParseException("Unable to parse date: " + json.getAsString(), e);
                }
            })
            .registerTypeAdapter(BigDecimal.class, (JsonSerializer<BigDecimal>) (src, type, context) ->
                    src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.stripTrailingZeros().toPlainString()))
            .registerTypeAdapter(BigDecimal.class, (JsonDeserializer<BigDecimal>) (json, type, context) -> {
                if (json == null || json.isJsonNull() || json.getAsString().isBlank()) {
                    return null;
                }
                return new BigDecimal(json.getAsString());
            })
            .create();

    private JsonUtil() {
    }

    public static Gson getGson() {
        return GSON;
    }
}
