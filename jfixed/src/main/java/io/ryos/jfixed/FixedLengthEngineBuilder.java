package io.ryos.jfixed;

import io.ryos.jfixed.core.FixedLengthEngine;
import io.ryos.jfixed.converter.ConverterRegistry;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;

/**
 * Builder class for constructing FixedLengthEngine instances.
 * ConverterRegistry is hidden as an internal implementation, and custom converters
 * can be registered through the builder.
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * FixedLengthEngine engine = FixedLengthEngineBuilder.create()
 *     .charset(StandardCharsets.UTF_8)
 *     .registerConverter(CustomType.class, (value, format) -> new CustomType(value))
 *     .build();
 * }</pre>
 */
public class FixedLengthEngineBuilder {
    private Charset charset = StandardCharsets.UTF_8;
    private ConverterRegistry registry = new ConverterRegistry();

    /**
     * Creates a new builder instance.
     *
     * @return a builder instance
     */
    public static FixedLengthEngineBuilder create() {
        return new FixedLengthEngineBuilder();
    }

    /**
     * Sets the character encoding.
     *
     * @param charset the character encoding
     * @return this builder instance
     * @throws IllegalArgumentException if charset is null
     */
    public FixedLengthEngineBuilder charset(Charset charset) {
        if (charset == null) {
            throw new IllegalArgumentException("charset must not be null");
        }
        this.charset = charset;
        return this;
    }

    /**
     * Registers a custom type converter.
     *
     * @param type the target type to convert
     * @param converter the conversion function (first argument: value, second argument: format)
     * @param <T> the target type
     * @return this builder instance
     * @throws IllegalArgumentException if type or converter is null
     */
    public <T> FixedLengthEngineBuilder registerConverter(
            Class<T> type, 
            BiFunction<String, String, T> converter) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (converter == null) {
            throw new IllegalArgumentException("converter must not be null");
        }
        registry.register(type, converter);
        return this;
    }

    /**
     * Builds a FixedLengthEngine instance.
     *
     * @return a FixedLengthEngine instance
     */
    public FixedLengthEngine build() {
        return new FixedLengthEngine(charset, registry);
    }
}
