package com.example.userservice.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Класс для безопасного форматирования даты и времени в строку.
 *
 * @author Simon35845
 */
public class DateTimeUtil {
    private static final String DATE_TIME_FORMAT = "dd.MM.yyyy HH:mm:ss";

    private DateTimeUtil() {
    }

    public static String dateTimeToString(LocalDateTime localDateTime) {
        return (localDateTime != null)
                ? localDateTime.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))
                : null;
    }
}
