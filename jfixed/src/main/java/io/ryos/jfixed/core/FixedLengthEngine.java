package io.ryos.jfixed.core;

import io.ryos.jfixed.annotation.FixedField;
import io.ryos.jfixed.converter.ConverterRegistry;
import io.ryos.jfixed.exception.FixedLengthException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

/**
 * main logic of analyze fixed-text data
 */
public class FixedLengthEngine {
    private final Charset charset;
    private final ConverterRegistry registry;

    /**
     * Constructor
     * @param charset charset
     * @param converterRegistry management class of conversion
     */
    public FixedLengthEngine(Charset charset, ConverterRegistry converterRegistry) {
        if (charset == null) {
            throw new IllegalArgumentException("charset must be null");
        }
        if (converterRegistry == null) {
            throw new IllegalArgumentException("converterRegistry must be null");
        }
        this.charset = charset;
        this.registry = converterRegistry;
    }

    /**
     * Method wrapping actual processing
     * @param line string line
     * @param clazz class of field definition
     * @param lineNumber number of rows of data
     * @return instance of clazz
     * @throws Exception FixedLengthException
     */
    public <T> T process(String line, Class<T> clazz, int lineNumber) throws Exception {
        if (line == null) {
            throw new IllegalArgumentException("line must be null");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must be null");
        }
        try {
            if (clazz.isRecord()) {
                return processRecord(line, clazz, lineNumber);
            } else {
                return processPojo(line, clazz, lineNumber);
            }
        } catch (FixedLengthException e) {
            throw e;
        } catch (Exception e) {
            throw new FixedLengthException(
                    "Failed to process" + e.getMessage(),
                    lineNumber,
                    null,
                    line,
                    e
            );
        }
    }

    /**
     * analyze for record class
     * @param line string line
     * @param clazz class of field definition
     * @param lineNumber number of rows of data
     * @return instance of clazz
     * @throws Exception
     */
    private <T> T processRecord(String line, Class<T> clazz, int lineNumber) throws Exception {
        Field[] fields = clazz.getDeclaredFields();
        Object[] args = new Object[fields.length];
        Class<?>[] argTypes = new Class<?>[fields.length];

        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            argTypes[i] = field.getType();

            FixedField config = field.getAnnotation(FixedField.class);
            if (config == null) {
                args[i] = getDefaultValue(field.getType());
                continue;
            }

            args[i] = extractAndConvert(line, field, config, lineNumber);
        }

        // Recordの標準コンストラクタ（全フィールドを引数に持つもの）を取得して実行
        Constructor<T> constructor = clazz.getDeclaredConstructor(argTypes);
        return constructor.newInstance(args);
    }

    /**
     * analyze for POJO
     * @param line string line
     * @param clazz class of field definition
     * @param lineNumber number of rows of data
     * @return instance of clazz
     * @throws Exception
     */
    private <T> T processPojo(String line, Class<T> clazz, int lineNumber) throws Exception {
        T instance = clazz.getDeclaredConstructor().newInstance();

        for (Field field : clazz.getDeclaredFields()) {
            FixedField config = field.getAnnotation(FixedField.class);
            if (config == null) continue;

            Object value = extractAndConvert(line, field, config, lineNumber);

            field.setAccessible(true);
            field.set(instance, value);
        }
        return instance;
    }

    /**
     * common logic of slicing by byte length and conversion
     * @param line string line
     * @param field field definition
     * @param config attached annotation
     * @param lineNumber number of rows of data
     * @return value of field
     */
    private Object extractAndConvert(String line, Field field, FixedField config, int lineNumber) {
        String rawValue = "";
        try {
            // 1. バイト単位での切り出し（ByteSlicerの役割を内包または呼び出し）
            rawValue = ByteSlicer.slice(line, config.offset(), config.length(), charset);

            // 2. 前後トリム設定の適用
            if (config.trim()) {
                rawValue = rawValue.trim();
            }

            // 3. 型変換
            return registry.convert(field.getType(), rawValue, config.format());

        } catch (Exception e) {
            throw new FixedLengthException(
                    "Failed to analyze field: " + e.getMessage(),
                    lineNumber,
                    field.getName(),
                    line,
                    e
            );
        }
    }

    /**
     * default value of each types
     * @param type type of field
     * @return default value
     */
    private Object getDefaultValue(Class<?> type) {
        if (type.isPrimitive()) {
            if (type == boolean.class) {
                return false;
            } else if (type == byte.class) {
                return (byte) 0;
            } else if (type == short.class) {
                return (short) 0;
            } else if (type == int.class) {
                return 0;
            } else if (type == long.class) {
                return 0L;
            } else if (type == float.class) {
                return 0.0f;
            } else if (type == double.class) {
                return 0.0;
            } else if (type == char.class) {
                return '\u0000';
            }
        }
        return null;
    }
}
