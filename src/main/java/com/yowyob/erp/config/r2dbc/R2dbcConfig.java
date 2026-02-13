package com.yowyob.erp.config.r2dbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom R2DBC Configuration to handle JSON types and other custom conversions.
 * Fixes "Nested entities are not supported" error for fields of type JsonNode.
 */
@Configuration
public class R2dbcConfig {

    private final ObjectMapper objectMapper;

    public R2dbcConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Object> converters = new ArrayList<>();
        converters.add(new JsonNodeToPostgresJsonConverter(objectMapper));
        converters.add(new PostgresJsonToJsonNodeConverter(objectMapper));
        return R2dbcCustomConversions.of(PostgresDialect.INSTANCE, converters);
    }

    @WritingConverter
    public static class JsonNodeToPostgresJsonConverter implements Converter<JsonNode, Json> {
        private final ObjectMapper objectMapper;

        public JsonNodeToPostgresJsonConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Json convert(JsonNode source) {
            if (source == null || source.isNull()) {
                return null;
            }
            try {
                return Json.of(objectMapper.writeValueAsString(source));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error converting JsonNode to Postgres Json", e);
            }
        }
    }

    @ReadingConverter
    public static class PostgresJsonToJsonNodeConverter implements Converter<Json, JsonNode> {
        private final ObjectMapper objectMapper;

        public PostgresJsonToJsonNodeConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public JsonNode convert(Json source) {
            if (source == null) {
                return null;
            }
            try {
                return objectMapper.readTree(source.asString());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error converting Postgres Json to JsonNode", e);
            }
        }
    }
}
