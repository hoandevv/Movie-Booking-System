package com.trainning.movie_booking_system.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for JSON conversion
 * Used for converting between JSON strings and Java objects in entities
 */
@UtilityClass
@Slf4j
public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convert List to JSON string
     */
    public static <T> String toJson(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Error converting list to JSON", e);
            return null;
        }
    }

    /**
     * Convert JSON string to List<Long>
     */
    public static List<Long> toLongList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to Long list", e);
            return new ArrayList<>();
        }
    }

    /**
     * Convert JSON string to List<Integer>
     */
    public static List<Integer> toIntegerList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to Integer list", e);
            return new ArrayList<>();
        }
    }

    /**
     * Convert JSON string to List<String>
     */
    public static List<String> toStringList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to String list", e);
            return new ArrayList<>();
        }
    }
}
