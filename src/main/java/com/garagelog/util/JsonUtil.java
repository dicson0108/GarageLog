package com.garagelog.util;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class JsonUtil {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDate.class, new JsonSerializer<LocalDate>() {
                @Override
                public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
                    return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.format(DATE_FORMATTER));
                }
            })
            .registerTypeAdapter(LocalDate.class, new JsonDeserializer<LocalDate>() {
                @Override
                public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                        throws JsonParseException {
                    if (json == null || json.isJsonNull() || json.getAsString().isBlank()) {
                        return null;
                    }
                    return LocalDate.parse(json.getAsString(), DATE_FORMATTER);
                }
            })
            .registerTypeAdapter(BigDecimal.class, new JsonSerializer<BigDecimal>() {
                @Override
                public JsonElement serialize(BigDecimal src, Type typeOfSrc, JsonSerializationContext context) {
                    return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.stripTrailingZeros().toPlainString());
                }
            })
            .registerTypeAdapter(BigDecimal.class, new JsonDeserializer<BigDecimal>() {
                @Override
                public BigDecimal deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                        throws JsonParseException {
                    if (json == null || json.isJsonNull() || json.getAsString().isBlank()) {
                        return null;
                    }
                    return new BigDecimal(json.getAsString());
                }
            })
            .create();

    private JsonUtil() {
    }

    public static Gson getGson() {
        return GSON;
    }
}
