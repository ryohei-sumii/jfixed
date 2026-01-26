package io.ryos.jfixed.core;

import io.ryos.jfixed.annotation.FixedField;
import io.ryos.jfixed.annotation.FixedSection;
import io.ryos.jfixed.annotation.FixedStructure;
import io.ryos.jfixed.converter.ConverterRegistry;
import io.ryos.jfixed.exception.FixedLengthException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Main engine for parsing fixed-length text data.
 * 
 * <p>This class provides the core functionality for parsing fixed-length data lines
 * into Java objects (POJOs or Records) based on field annotations.</p>
 * 
 * <p>Basic usage:</p>
 * <pre>{@code
 * FixedLengthEngine engine = FixedLengthParser.create();
 * Person person = engine.process(line, Person.class, 1);
 * }</pre>
 * 
 * <p>For structured data with multiple sections (Header, Data, Trailer, End):</p>
 * <pre>{@code
 * List<String> lines = Arrays.asList("H...", "D...", "T...", "E...");
 * List<Object> result = engine.processStructure(lines, FileStructure.class);
 * }</pre>
 * 
 * <p>The engine supports both POJOs and Java Records. For POJOs, it requires
 * a no-argument constructor. If no such constructor exists, it falls back to
 * record-style processing using reflection.</p>
 * 
 * @see io.ryos.jfixed.FixedLengthParser
 * @see io.ryos.jfixed.annotation.FixedField
 * @see io.ryos.jfixed.annotation.FixedStructure
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
            throw new IllegalArgumentException("charset must not be null");
        }
        if (converterRegistry == null) {
            throw new IllegalArgumentException("converterRegistry must not be null");
        }
        this.charset = charset;
        this.registry = converterRegistry;
    }

    /**
     * Processes a single fixed-length line and converts it to an instance of the specified class.
     * 
     * <p>The method extracts fields from the line based on {@code @FixedField} annotations
     * in the target class and converts them to the appropriate types.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * String line = "John      Doe       25 ";
     * Person person = engine.process(line, Person.class, 1);
     * }</pre>
     * 
     * @param <T> the target type
     * @param line the fixed-length line to parse
     * @param clazz the target class (POJO or Record)
     * @param lineNumber the line number (used for error reporting)
     * @return an instance of the target class populated with data from the line
     * @throws IllegalArgumentException if line or clazz is null
     * @throws FixedLengthException if parsing fails (e.g., line too short, invalid format)
     * @throws Exception if an unexpected error occurs during processing
     */
    public <T> T process(String line, Class<T> clazz, int lineNumber) throws Exception {
        if (line == null) {
            throw new IllegalArgumentException("line must not be null");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
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
        Field[] fields = clazz.getDeclaredFields();

        // try to create Instance without arguments
        Constructor<T> noArgsConstructor;
        try {
            noArgsConstructor = clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            // if there is no non-Args Constructor, try to create constructor with all fields
            return processRecord(line, clazz, lineNumber);
        }

        T instance = noArgsConstructor.newInstance();

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
        String rawValue;
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
     * Processes a structured fixed-length data file with Header, Data (repeatable), Trailer, and End sections.
     * 
     * <p>This method parses a multi-section fixed-length file structure. The structure class must be
     * annotated with {@code @FixedStructure} and contain inner classes annotated with {@code @FixedSection}
     * for each section type.</p>
     * 
     * <p>Example structure:</p>
     * <pre>{@code
     * @FixedStructure(lineIdentifierField = "type", headerIdentifier = "H", dataIdentifier = "D")
     * class FileStructure {
     *     @FixedSection(type = "header")
     *     static class Header { ... }
     *     
     *     @FixedSection(type = "data")
     *     static class Data { ... }
     * }
     * }</pre>
     * 
     * <p>The method parses all sections and creates an instance of the structure class
     * containing the parsed Header, Data records, Trailer, and End sections.</p>
     * 
     * @param <T> the structure type
     * @param lines list of lines representing the structure
     * @param structureClass class annotated with @FixedStructure that defines the structure
     * @return an instance of the structure class containing parsed sections
     * @throws IllegalArgumentException if lines is null/empty or structureClass is null or not annotated
     * @throws FixedLengthException if the structure is invalid (e.g., multiple headers, missing sections)
     * @throws Exception if an unexpected error occurs during processing
     */
    public <T> T processStructure(List<String> lines, Class<T> structureClass) throws Exception {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("lines must not be null or empty");
        }
        if (structureClass == null) {
            throw new IllegalArgumentException("structureClass must not be null");
        }
        
        FixedStructure structureAnnotation = structureClass.getAnnotation(FixedStructure.class);
        if (structureAnnotation == null) {
            throw new IllegalArgumentException("structureClass must be annotated with @FixedStructure");
        }
        
        try {
            // Analyze structure fields
            StructureInfo info = analyzeStructure(structureClass, structureAnnotation);
            
            // Process lines
            Object header = null;
            List<Object> dataList = new ArrayList<>();
            Object trailer = null;
            Object end = null;
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String lineIdentifier = extractLineIdentifier(line, info.lineIdentifierField, structureAnnotation, info);
                
                if (lineIdentifier.equals(structureAnnotation.headerIdentifier())) {
                    if (header != null) {
                        throw new FixedLengthException(
                            "Multiple header records found",
                            i + 1,
                            null,
                            line,
                            null
                        );
                    }
                    header = process(line, info.headerClass, i + 1);
                } else if (lineIdentifier.equals(structureAnnotation.dataIdentifier())) {
                    Object data = process(line, info.dataClass, i + 1);
                    dataList.add(data);
                } else if (lineIdentifier.equals(structureAnnotation.trailerIdentifier())) {
                    if (trailer != null) {
                        throw new FixedLengthException(
                            "Multiple trailer records found",
                            i + 1,
                            null,
                            line,
                            null
                        );
                    }
                    trailer = process(line, info.trailerClass, i + 1);
                } else if (lineIdentifier.equals(structureAnnotation.endIdentifier())) {
                    if (end != null) {
                        throw new FixedLengthException(
                            "Multiple end records found",
                            i + 1,
                            null,
                            line,
                            null
                        );
                    }
                    end = process(line, info.endClass, i + 1);
                } else {
                    throw new FixedLengthException(
                        "Unknown line identifier: " + lineIdentifier,
                        i + 1,
                        null,
                        line,
                        null
                    );
                }
            }
            
            // Validate required sections
            if (header == null && info.headerClass != null) {
                throw new FixedLengthException("Header record is required but not found", 0, null, null, null);
            }
            if (trailer == null && info.trailerClass != null) {
                throw new FixedLengthException("Trailer record is required but not found", 0, null, null, null);
            }
            if (end == null && info.endClass != null) {
                throw new FixedLengthException("End record is required but not found", 0, null, null, null);
            }
            
            // Create structure instance
            return createStructureInstance(structureClass, info, header, dataList, trailer, end);
            
        } catch (FixedLengthException e) {
            throw e;
        } catch (Exception e) {
            throw new FixedLengthException(
                "Failed to process structure: " + e.getMessage(),
                0,
                null,
                null,
                e
            );
        }
    }
    
    /**
     * Analyzes the structure class to identify section fields and their types.
     */
    private StructureInfo analyzeStructure(Class<?> structureClass, FixedStructure annotation) {
        StructureInfo info = new StructureInfo();
        info.lineIdentifierField = annotation.lineIdentifierField();
        
        Field[] fields = structureClass.getDeclaredFields();
        for (Field field : fields) {
            FixedSection section = field.getAnnotation(FixedSection.class);
            if (section == null) continue;
            
            field.setAccessible(true);
            Class<?> fieldType = field.getType();
            
            switch (section.value()) {
                case HEADER:
                    info.headerField = field;
                    info.headerClass = fieldType;
                    break;
                case DATA:
                    info.dataField = field;
                    // For List<T>, extract T
                    if (List.class.isAssignableFrom(fieldType)) {
                        // Get generic type parameter
                        java.lang.reflect.ParameterizedType listType = 
                            (java.lang.reflect.ParameterizedType) field.getGenericType();
                        info.dataClass = (Class<?>) listType.getActualTypeArguments()[0];
                    } else {
                        throw new IllegalArgumentException(
                            "Data field must be of type List<T>, but found: " + fieldType
                        );
                    }
                    break;
                case TRAILER:
                    info.trailerField = field;
                    info.trailerClass = fieldType;
                    break;
                case END:
                    info.endField = field;
                    info.endClass = fieldType;
                    break;
            }
        }
        
        return info;
    }
    
    /**
     * Extracts the line identifier from a line by trying to parse it with each section class
     * and checking the identifier field value.
     */
    private String extractLineIdentifier(String line, String identifierFieldName, FixedStructure annotation,
                                        StructureInfo info) {
        if (identifierFieldName == null || identifierFieldName.isEmpty()) {
            // If no identifier field is specified, use the first character
            return !line.isEmpty() ? line.substring(0, 1) : "";
        }
        // get byte length of record
        int lineByteLength = line.getBytes(charset).length;

        if (info.endClass != null && lineByteLength <= 400) {
            try {
                Object section = process(line, info.endClass, 1);
                Field identifierField = findFieldByName(info.endClass, identifierFieldName);
                if (identifierField != null) {
                    identifierField.setAccessible(true);
                    Object value = identifierField.get(section);
                    if (value != null) {
                        String identifier = value.toString().trim();
                        if (identifier.equals(annotation.endIdentifier())) {
                            return identifier;
                        }
                    }
                }
            } catch (Exception e) {
                // try to extract by another class
            }
        }

        // Try to parse with each section class and extract identifier
        Class<?>[] sectionClasses = {info.headerClass, info.dataClass, info.trailerClass, info.endClass};
        for (Class<?> sectionClass : sectionClasses) {
            if (sectionClass == null) continue;
            
            try {
                Object section = process(line, sectionClass, 1);
                Field identifierField = findFieldByName(sectionClass, identifierFieldName);
                if (identifierField != null) {
                    identifierField.setAccessible(true);
                    Object value = identifierField.get(section);
                    if (value != null) {
                        return value.toString().trim();
                    }
                }
            } catch (Exception e) {
                // Try next class
            }
        }
        
        // Fallback: use first character
        return !line.isEmpty() ? line.substring(0, 1) : "";
    }
    
    /**
     * Finds a field by name in a class (including inherited fields).
     */
    private Field findFieldByName(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
    
    /**
     * Creates an instance of the structure class with all sections populated.
     */
    private <T> T createStructureInstance(Class<T> structureClass, StructureInfo info,
                                          Object header, List<Object> dataList, Object trailer, Object end) throws Exception {
        if (structureClass.isRecord()) {
            return createRecordInstance(structureClass, info, header, dataList, trailer, end);
        } else {
            return createPojoInstance(structureClass, info, header, dataList, trailer, end);
        }
    }
    
    private <T> T createRecordInstance(Class<T> structureClass, StructureInfo info,
                                       Object header, List<Object> dataList, Object trailer, Object end) throws Exception {
        Field[] fields = structureClass.getDeclaredFields();
        Object[] args = new Object[fields.length];
        Class<?>[] argTypes = new Class<?>[fields.length];
        
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            argTypes[i] = field.getType();
            
            FixedSection section = field.getAnnotation(FixedSection.class);
            if (section != null) {
                switch (section.value()) {
                    case HEADER:
                        args[i] = header;
                        break;
                    case DATA:
                        args[i] = dataList;
                        break;
                    case TRAILER:
                        args[i] = trailer;
                        break;
                    case END:
                        args[i] = end;
                        break;
                }
            } else {
                args[i] = getDefaultValue(field.getType());
            }
        }
        
        Constructor<T> constructor = structureClass.getDeclaredConstructor(argTypes);
        return constructor.newInstance(args);
    }
    
    private <T> T createPojoInstance(Class<T> structureClass, StructureInfo info,
                                      Object header, List<Object> dataList, Object trailer, Object end) throws Exception {
        T instance = structureClass.getDeclaredConstructor().newInstance();
        
        Field[] fields = structureClass.getDeclaredFields();
        for (Field field : fields) {
            FixedSection section = field.getAnnotation(FixedSection.class);
            if (section == null) continue;
            
            field.setAccessible(true);
            switch (section.value()) {
                case HEADER:
                    field.set(instance, header);
                    break;
                case DATA:
                    field.set(instance, dataList);
                    break;
                case TRAILER:
                    field.set(instance, trailer);
                    break;
                case END:
                    field.set(instance, end);
                    break;
            }
        }
        
        return instance;
    }
    
    /**
     * Helper class to store structure analysis information.
     */
    private static class StructureInfo {
        String lineIdentifierField;
        @SuppressWarnings("unused")
        Field headerField;
        @SuppressWarnings("unused")
        Field dataField;
        @SuppressWarnings("unused")
        Field trailerField;
        @SuppressWarnings("unused")
        Field endField;
        Class<?> headerClass;
        Class<?> dataClass;
        Class<?> trailerClass;
        Class<?> endClass;
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
