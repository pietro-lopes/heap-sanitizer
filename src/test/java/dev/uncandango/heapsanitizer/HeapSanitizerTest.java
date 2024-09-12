package dev.uncandango.heapsanitizer;

import dev.uncandango.heapsanitizer.fixture.HeapDumper;
import dev.uncandango.heapsanitizer.sanitizer.SanitizeConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HeapSanitizerTest {

	@TempDir
	static Path tempDir;

	@BeforeEach
	public void beforeEach(final TestInfo info) {
		HeapSanitizer.LOGGER.info("Test - {}:", info.getDisplayName());
	}

	@Test
	@DisplayName("testCompressHeapDump. Verify that heap dump was successfully compressed.")
	public void testCompressHeapDump() throws Exception {
		final Path heapDump = triggerHeapDump();

		final Path output = HeapSanitizer.withInput(heapDump)
			.modifyConfig(SanitizeConfig.Builder::compress)
			.sanitize();

		assertThat(output.toFile())
			.isFile()
			.hasExtension("zip");

		try (InputStream is = new BufferedInputStream(Files.newInputStream(output), 1024 * 64);
			 ZipInputStream zis = new ZipInputStream(is)) {
			ZipEntry ze = zis.getNextEntry();

			assertNotNull(ze);
			assertThat(ze.getName()).endsWithIgnoringCase("-sanitized.hprof");
		}
	}

	private Path triggerHeapDump() throws Exception {
		final Path heapDumpPath = newTempFilePath();

		HeapSanitizer.LOGGER.info("Heap dumping to {}", heapDumpPath);
		HeapDumper.dumpHeap(heapDumpPath);

		return heapDumpPath;
	}

	private Path newTempFilePath() throws IOException {
		final Path path = Files.createTempFile(tempDir, getClass().getSimpleName(), ".hprof");
		Files.delete(path);
		return path;
	}
}
