# Heap Sanitizer

*This is a fork of* [Heap Dump Tool](https://github.com/paypal/heap-dump-tool) by PayPal.

The goal of this fork is to focus on the Library usage of the project.

Heap Sanitizer can capture and, more importantly, sanitize sensitive data from Java heap dumps. Sanitization is accomplished
by replacing field values in the heap dump file with zero values. Heap dump can then be more freely shared and analyzed.

A typical scenario is when a heap dump needs to be sanitized before it can be given to another person or moved to a different
environment. For example, an app running in production environment may contain sensitive data (passwords, credit card
numbers, etc.) which should not be viewable when the heap dump is copied to a development environment for analysis with a
graphical program.

<img src="https://github.com/paypal/heap-dump-tool/raw/statics/heap-dump-file.png"/>

---

<img src="https://github.com/paypal/heap-dump-tool/raw/statics/sanitized-heap-dump-file.png"/>

## TOC
  * [Examples](#examples)
  * [Usage](#usage)
  * [License](#license)
	
## Examples

This tool is focused to be used as a Library.

### [Library] Embed within an app

To use it as a library and embed it within another app, you can declare it as dependency in maven:

```xml
<repositories>
	<repository>
	    <id>jitpack.io</id>
	    <url>https://jitpack.io</url>
	</repository>
</repositories>

<dependency>
  <groupId>com.github.pietro-lopes</groupId>
  <artifactId>heap-sanitizer</artifactId>
  <version>1.0.0</version>
</dependency>
```

or Gradle:

```groovy
repositories {
	maven { url 'https://jitpack.io' }
}

dependencies {
	implementation 'com.github.pietro-lopes:heap-sanitizer:1.0.0'
}
```

<a name="usage"></a>

## Usage

```java
public static void dumpSanitized() {
    final Path heapDumpPath = // Your path

    HeapSanitizer.LOGGER.info("Heap dumping to {}", heapDumpPath);
    HeapDumper.dumpHeap(heapDumpPath);

    Path zippedOutput = HeapSanitizer.withInput(heapDumpPath)
        .modifyConfig(SanitizeConfig.Builder::compress)
        .sanitize();
}
```
## License

Heap Sanitizer is Open Source software released under the Apache 2.0 license.

