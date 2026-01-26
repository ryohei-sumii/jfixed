# jfixed

A Java library for parsing fixed-length text data with annotation-based field mapping.

## Features

- **Simple API**: Easy-to-use annotation-based approach for defining fixed-length data structures
- **Type Support**: Built-in support for common types (String, Integer, Long, Double, Float, Boolean, LocalDate, LocalDateTime, BigDecimal, etc.)
- **POJO and Record Support**: Works with both traditional POJOs and Java Records
- **Structured Data**: Support for multi-section files (Header, Data, Trailer, End)
- **Custom Converters**: Register custom type converters for specialized data types
- **Flexible Configuration**: Configurable character encoding, trimming, and formatting options
- **Java 17+ Compatible**: Built with Java 25, compatible with Java 17 and later

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.ryohei-sumii</groupId>
    <artifactId>jfixed</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

Add the following to your `build.gradle`:

```gradle
dependencies {
    implementation 'io.github.ryohei-sumii:jfixed:1.0.0'
}
```

## Quick Start

### Basic Usage

1. Define your data class with `@FixedField` annotations:

```java
import io.ryos.jfixed.annotation.FixedField;

class Person {
    @FixedField(offset = 0, length = 10)
    String name;
    
    @FixedField(offset = 10, length = 10)
    String surname;
    
    @FixedField(offset = 20, length = 3)
    int age;
}
```

2. Parse the fixed-length line:

```java
import io.ryos.jfixed.FixedLengthParser;

FixedLengthEngine engine = FixedLengthParser.create();
String line = "John      Doe       25 ";
Person person = engine.process(line, Person.class, 1);
```

### Using Java Records

```java
import io.ryos.jfixed.annotation.FixedField;

record PersonRecord(
    @FixedField(offset = 0, length = 10) String name,
    @FixedField(offset = 10, length = 10) String surname,
    @FixedField(offset = 20, length = 3) int age
) {}
```

```java
FixedLengthEngine engine = FixedLengthParser.create();
String line = "John      Doe       25 ";
PersonRecord person = engine.process(line, PersonRecord.class, 1);
```

## Advanced Usage

### Date and DateTime Parsing

```java
import java.time.LocalDate;
import java.time.LocalDateTime;
import io.ryos.jfixed.annotation.FixedField;

class PersonWithDate {
    @FixedField(offset = 0, length = 10)
    String name;
    
    @FixedField(offset = 20, length = 3)
    int age;
    
    // Default format: yyyyMMdd
    @FixedField(offset = 23, length = 8)
    LocalDate birthDate;
    
    // Custom format
    @FixedField(offset = 31, length = 10, format = "yyyy-MM-dd")
    LocalDate registrationDate;
    
    // DateTime: yyyyMMddHHmmss
    @FixedField(offset = 41, length = 14)
    LocalDateTime createdAt;
}
```

### Structured Data (Header, Data, Trailer, End)

For files with multiple sections:

```java
import io.ryos.jfixed.annotation.FixedStructure;
import io.ryos.jfixed.annotation.FixedSection;

@FixedStructure(
    lineIdentifierField = "type",
    headerIdentifier = "H",
    dataIdentifier = "D",
    trailerIdentifier = "T",
    endIdentifier = "E"
)
class FileStructure {
    @FixedSection(type = "header")
    static class Header {
        @FixedField(offset = 0, length = 1)
        String type;
        
        @FixedField(offset = 1, length = 8)
        String fileDate;
    }
    
    @FixedSection(type = "data")
    static class Data {
        @FixedField(offset = 0, length = 1)
        String type;
        
        @FixedField(offset = 1, length = 20)
        String name;
    }
    
    @FixedSection(type = "trailer")
    static class Trailer {
        @FixedField(offset = 0, length = 1)
        String type;
        
        @FixedField(offset = 1, length = 10)
        int recordCount;
    }
    
    @FixedSection(type = "end")
    static class End {
        @FixedField(offset = 0, length = 1)
        String type;
    }
}
```

```java
List<String> lines = Arrays.asList(
    "H20240122",
    "DJohn Doe            ",
    "DJane Smith          ",
    "T0000000002",
    "E"
);

FixedLengthEngine engine = FixedLengthParser.create();
List<Object> result = engine.processStructure(lines, FileStructure.class);
```

### Custom Converters

Register custom type converters:

```java
import io.ryos.jfixed.FixedLengthEngineBuilder;
import java.nio.charset.StandardCharsets;

class CustomType {
    private final String value;
    public CustomType(String value) { this.value = value; }
    public String getValue() { return value; }
}

FixedLengthEngine engine = FixedLengthEngineBuilder.create()
    .charset(StandardCharsets.UTF_8)
    .registerConverter(CustomType.class, (value, format) -> new CustomType(value))
    .build();
```

### Field Options

```java
class Person {
    // Trim whitespace (default: true)
    @FixedField(offset = 0, length = 10, trim = true)
    String name;
    
    // Don't trim
    @FixedField(offset = 10, length = 10, trim = false)
    String code;
    
    // Custom fill character (default: space)
    @FixedField(offset = 20, length = 5, fillChar = '0')
    String id;
}
```

## API Reference

### FixedLengthParser

Entry point for creating parsers:

- `FixedLengthParser.create()` - Creates a parser with UTF-8 encoding
- `FixedLengthParser.create(Charset charset)` - Creates a parser with specified encoding
- `FixedLengthParser.builder()` - Returns a builder for custom configuration

### FixedLengthEngine

Main processing engine:

- `process(String line, Class<T> clazz, int lineNumber)` - Process a single line
- `processStructure(List<String> lines, Class<?> structureClass)` - Process structured data

### Annotations

- `@FixedField` - Marks a field as a fixed-length field
  - `offset` - Starting position (required)
  - `length` - Field length (required)
  - `format` - Format pattern for dates/datetimes (optional)
  - `trim` - Whether to trim whitespace (default: true)
  - `fillChar` - Character used for padding (default: space)

- `@FixedStructure` - Marks a class as a structured data definition
  - `lineIdentifierField` - Field name that identifies the section type
  - `headerIdentifier` - Value that identifies header lines (default: "H")
  - `dataIdentifier` - Value that identifies data lines (default: "D")
  - `trailerIdentifier` - Value that identifies trailer lines (default: "T")
  - `endIdentifier` - Value that identifies end lines (default: "E")

- `@FixedSection` - Marks an inner class as a section type
  - `type` - Section type: "header", "data", "trailer", or "end"

## Supported Types

The library supports the following types out of the box:

- `String`
- `Integer` / `int`
- `Long` / `long`
- `Double` / `double`
- `Float` / `float`
- `Boolean` / `boolean`
- `Short` / `short`
- `Byte` / `byte`
- `Character` / `char`
- `BigDecimal`
- `LocalDate` (format: `yyyyMMdd` or custom)
- `LocalDateTime` (format: `yyyyMMddHHmmss` or custom)

## Error Handling

The library throws `FixedLengthException` when:

- The input line is too short for the field definitions
- Type conversion fails (e.g., invalid number format)
- Required fields are missing
- Invalid data format

Example:

```java
try {
    Person person = engine.process(line, Person.class, 1);
} catch (FixedLengthException e) {
    System.err.println("Error at line " + e.getLineNumber() + ": " + e.getMessage());
}
```

## Requirements

- Java 17 or later
- No external dependencies (except for testing)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Author

ryohei-sumii

## Links

- [GitHub Repository](https://github.com/ryohei-sumii/jfixed)
- [Maven Central](https://central.sonatype.com/artifact/io.github.ryohei-sumii/jfixed)
