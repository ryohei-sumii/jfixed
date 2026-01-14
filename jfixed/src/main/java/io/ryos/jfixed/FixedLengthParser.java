package io.ryos.jfixed;

import io.ryos.jfixed.core.FixedLengthEngine;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Entry point for the fixed-length data parsing library.
 * Provides the simplest way to use the library.
 * 
 * <p>Basic usage example:</p>
 * <pre>{@code
 * FixedLengthEngine engine = FixedLengthParser.create();
 * Person person = engine.process(line, Person.class);
 * }</pre>
 * 
 * <p>Usage with custom converters:</p>
 * <pre>{@code
 * FixedLengthEngine engine = FixedLengthParser.builder()
 *     .charset(StandardCharsets.UTF_8)
 *     .registerConverter(CustomType.class, (value, format) -> new CustomType(value))
 *     .build();
 * }</pre>
 */
public class FixedLengthParser {
    
    /**
     * Creates a parser with UTF-8 encoding.
     *
     * @return a FixedLengthEngine instance
     */
    public static FixedLengthEngine create() {
        return create(StandardCharsets.UTF_8);
    }

    /**
     * Creates a parser with the specified character encoding.
     *
     * @param charset the character encoding
     * @return a FixedLengthEngine instance
     * @throws IllegalArgumentException if charset is null
     */
    public static FixedLengthEngine create(Charset charset) {
        return FixedLengthEngineBuilder.create()
            .charset(charset)
            .build();
    }

    /**
     * Returns a builder for creating a FixedLengthEngine with custom settings.
     * Use this when you need to register custom converters or configure other options.
     *
     * @return a FixedLengthEngineBuilder instance
     */
    public static FixedLengthEngineBuilder builder() {
        return FixedLengthEngineBuilder.create();
    }
}
