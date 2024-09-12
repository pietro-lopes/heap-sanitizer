package dev.uncandango.heapsanitizer.fixture;

import org.apache.commons.io.IOUtils;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ResourceTool {

	private ResourceTool() {
		throw new AssertionError();
	}

	public static String contentOf(final Class<?> testClass, final String resourceName) throws IOException {
		final String fqPath = getFqPath(testClass, resourceName);
		return IOUtils.resourceToString(fqPath, UTF_8);
	}

	private static String getFqPath(final Class<?> testClass, final String resourceName) {
		return String.format("/files/%s/%s", testClass.getSimpleName(), resourceName);
	}

	public static byte[] bytesOf(final Class<?> testClass, final String resourceName) throws IOException {
		final String fqPath = getFqPath(testClass, resourceName);
		return IOUtils.resourceToByteArray(fqPath);
	}
}
