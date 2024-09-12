package dev.uncandango.heapsanitizer.utils;

import dev.uncandango.heapsanitizer.HeapSanitizer;
import dev.uncandango.heapsanitizer.sanitizer.DataSize;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ReadsStdIo;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static dev.uncandango.heapsanitizer.sanitizer.DataSize.ofBytes;
import static org.assertj.core.api.Assertions.assertThat;

public class ProgressMonitorTest {

	@Test
	public void testNumBytesWrittenMonitor() {
		Set<String> messages = new HashSet<>();

		final ProgressMonitor numBytesWrittenMonitor = numBytesProcessedMonitor(ofBytes(5), HeapSanitizer.LOGGER, messages);
		numBytesWrittenMonitor.accept(4L);
		assertThat(messages)
			.isEmpty();

		numBytesWrittenMonitor.accept(5L);
		assertThat(messages)
			.hasSize(1)
			.contains("Processed 5 bytes");

		numBytesWrittenMonitor.accept(6L);
		assertThat(messages)
			.hasSize(1)
			.contains("Processed 5 bytes");

		numBytesWrittenMonitor.accept(11L);
		assertThat(messages)
			.hasSize(2)
			.contains("Processed 5 bytes")
			.contains("Processed 11 bytes");
	}

	private static ProgressMonitor numBytesProcessedMonitor(final DataSize stepSize, final Logger logger, Set<String> messages) {
		final long stepSizeBytes = stepSize.toBytes();
		final MutableLong steps = new MutableLong();

		return numBytesProcessed -> {
			final long currentSteps = numBytesProcessed / stepSizeBytes;
			if (currentSteps != steps.getValue().longValue()) {
				steps.setValue(currentSteps);
				logger.info("Processed {}", FileUtils.byteCountToDisplaySize(numBytesProcessed));
				messages.add(String.format("Processed %s", FileUtils.byteCountToDisplaySize(numBytesProcessed)));
			}
		};
	}
//    @Test
//    public void testMonitoredInputStream() throws IOException {
//
//        final ProgressMonitor monitor = mock(ProgressMonitor.class);
//
//        final InputStream inputStream = new ByteArrayInputStream("hello".getBytes(UTF_8));
//        doCallRealMethod().when(monitor).monitoredInputStream(inputStream);
//
//        final InputStream monitoredInputStream = monitor.monitoredInputStream(inputStream);
//        IOUtils.toByteArray(monitoredInputStream);
//
//        verify(monitor, times(2)).accept((long) "hello".length());
//    }
//
//    @Test
//    public void testMonitoredOutputStream() throws IOException {
//
//        final ProgressMonitor monitor = mock(ProgressMonitor.class);
//
//        final OutputStream outputStream = new ByteArrayOutputStream();
//        doCallRealMethod().when(monitor).monitoredOutputStream(outputStream);
//
//        final OutputStream monitoredOutputStream = monitor.monitoredOutputStream(outputStream);
//        IOUtils.write("world", monitoredOutputStream, UTF_8);
//
//        verify(monitor).accept((long) "world".length());
//    }
}
