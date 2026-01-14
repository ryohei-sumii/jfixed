package io.ryos.jfixed.converter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Management Class of Conversion
 */
public class ConverterRegistry {
    // contains conversion logic for each type
    private final Map<Class<?>, BiFunction<String, String, Object >> converters = new HashMap<>();

    public ConverterRegistry() {
        // registering a standard converter
        registerStandardConverter();
    }

    private void registerStandardConverter() {
        // String: not convert
        register(String.class, (value, format) -> value);

        // Integer
        register(Integer.class, (value, format) -> {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            return Integer.parseInt(value.trim());
        });
        register(int.class, (value, format) -> {
            if (value == null || value.trim().isEmpty()) {
                return 0;
            }
            return Integer.parseInt(value.trim());
        });

        // Long
        register(Long.class, (value, format) -> {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            return Long.parseLong(value.trim());
        });
        register(long.class, (value, format) -> {
            if (value == null || value.trim().isEmpty()) {
                return 0L;
            }
            return Long.parseLong(value.trim());
        });

        // Double
        register(Double.class, (value, format) -> {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            return Double.parseDouble(value.trim());
        });
        register(double.class, (value, format) -> {
            if (value == null || value.trim().isEmpty()) {
                return 0.0;
            }
            return Double.parseDouble(value.trim());
        });

        // Float
        register(Float.class, (value, format) -> {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            return Float.parseFloat(value.trim());
        });
        register(float.class, (value, format) -> {
            if (value == null || value.trim().isEmpty()) {
                return 0.0F;
            }
            return Float.parseFloat(value.trim());
        });

        // boolean
        register(Boolean.class, (value, format) -> {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            String trimmedValue = value.trim();
            if ("1".equals(trimmedValue)
                    || "true".equalsIgnoreCase(trimmedValue)
                    || "yes".equalsIgnoreCase(trimmedValue)
                    || "y".equalsIgnoreCase(trimmedValue)) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        });
        register(boolean.class, (value, format) -> {
            if (value == null || value.trim().isEmpty()) {
                return false;
            }
            String trimmedValue = value.trim();
            return "1".equals(trimmedValue)
                    || "true".equalsIgnoreCase(trimmedValue)
                    || "yes".equalsIgnoreCase(trimmedValue)
                    || "y".equalsIgnoreCase(trimmedValue);
        });

        // BigDecimal
        register(BigDecimal.class, (value, format) -> {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            return new BigDecimal(value.trim());
        });

        // Date
        register(LocalDate.class, (value, format) -> {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            String trimmedValue = value.trim();
            if (format == null || format.isEmpty()) {
                format = "yyyyMMdd";
            }
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                return LocalDate.parse(trimmedValue, formatter);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                        String.format("Invalid date format: %s", format), e
                );
            }
        });
        register(LocalDateTime.class, (value, format) -> {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            String trimmed = value.trim();
            if (format == null || format.isEmpty()) {
                // デフォルトフォーマット: yyyyMMddHHmmss
                format = "yyyyMMddHHmmss";
            }
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                return LocalDateTime.parse(trimmed, formatter);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                        String.format("Failed to parse datetime '%s' with format '%s'", trimmed, format), e);
            }
        });

    }

    /**
     * Registers a type converter.
     *
     * @param type the target type to convert
     * @param converter the conversion function (first argument: value, second argument: format)
     * @param <T> the target type
     * @throws IllegalArgumentException if type or converter is null
     */
    public <T> void register(Class<T> type, BiFunction<String, String, T> converter) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (converter == null) {
            throw new IllegalArgumentException("converter must not be null");
        }
        converters.put(type, (BiFunction<String, String, Object>) converter);
    }

    public Object convert(Class<?> type, String value, String format) {
        BiFunction<String, String, Object> converter = converters.get(type);
        if (converter == null) {
            throw new IllegalArgumentException("Unsuppoted type" + type);
        }
        try {
            return converter.apply(value, format);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Failed to convert value '%s' with format '%s'",
                            value, format), e);
        }
    }
}
