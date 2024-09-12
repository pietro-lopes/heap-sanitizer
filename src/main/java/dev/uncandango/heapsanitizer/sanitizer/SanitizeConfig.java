package dev.uncandango.heapsanitizer.sanitizer;

import dev.uncandango.heapsanitizer.HeapSanitizer;
import dev.uncandango.heapsanitizer.utils.ProgressMonitor;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SanitizeConfig {

	private final InputStream inputStream;
	private final OutputStream outputStream;
	private final ProgressMonitor progressMonitor;
	private final String sanitizationText;
	private final boolean sanitizeArraysOnly;
	private final boolean sanitizeByteCharArraysOnly;

	private SanitizeConfig(Builder builder) {
		this.inputStream = builder.inputStream;
		this.outputStream = builder.outputStream;
		this.progressMonitor = builder.progressMonitor;
		this.sanitizationText = builder.sanitizationText;
		this.sanitizeArraysOnly = builder.sanitizeArraysOnly;
		this.sanitizeByteCharArraysOnly = builder.sanitizeByteCharArraysOnly;
	}

	public static Builder builder() {
		return new Builder();
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

	public ProgressMonitor getProgressMonitor() {
		return progressMonitor;
	}

	public String getSanitizationText() {
		return sanitizationText;
	}

	public boolean isSanitizeArraysOnly() {
		return sanitizeArraysOnly;
	}

	public boolean isSanitizeByteCharArraysOnly() {
		return sanitizeByteCharArraysOnly;
	}

	public static class Builder {
		@Nullable
		private InputStream inputStream;
		@Nullable
		private OutputStream outputStream;
		@Nullable
		private Path inputPath;

		public Path getOutputPath() {
			return resolveOutputPath();
		}

		@Nullable
		private Path outputPath;
		private ProgressMonitor progressMonitor = ProgressMonitor.numBytesProcessedMonitor(DataSize.ofMegabytes(100), HeapSanitizer.LOGGER);
		private String sanitizationText = StringEscapeUtils.unescapeJava("\\0");
		private boolean compressed = false;
		private boolean sanitizeArraysOnly = false;
		private boolean sanitizeByteCharArraysOnly = true;

		public Builder setInputStream(@Nullable InputStream inputStream) {
			this.inputStream = inputStream;
			return this;
		}

		public Builder setOutputStream(@Nullable OutputStream outputStream) {
			this.outputStream = outputStream;
			return this;
		}

		public Builder setInputPath(Path inputPath) {
			this.inputPath = inputPath;
			return this;
		}

		public Builder setOutputPath(Path outputPath) {
			this.outputPath = outputPath;
			return this;
		}

		public Builder setProgressMonitor(ProgressMonitor progressMonitor) {
			this.progressMonitor = progressMonitor;
			return this;
		}

		public Builder setSanitizationText(String sanitizationText) {
			this.sanitizationText = sanitizationText;
			return this;
		}

		public Builder compress() {
			this.compressed = true;
			return this;
		}

		public Builder sanitizeArraysOnly() {
			this.sanitizeArraysOnly = true;
			return this;
		}

		public Builder sanitizeArraysOnly(boolean sanitizeArraysOnly) {
			this.sanitizeArraysOnly = sanitizeArraysOnly;
			return this;
		}

		public Builder sanitizeByteCharArraysOnly() {
			this.sanitizeByteCharArraysOnly = true;
			return this;
		}

		public Builder sanitizeByteCharArraysOnly(boolean sanitizeByteCharArraysOnly) {
			this.sanitizeByteCharArraysOnly = sanitizeByteCharArraysOnly;
			return this;
		}

		public SanitizeConfig build() throws IOException {
			validate();
			return new SanitizeConfig(this);
		}

		private void validate() throws IOException {
			this.outputStream = resolveOutputStream();
			this.inputStream = resolveInputStream();
		}

		private OutputStream resolveOutputStream() throws IOException {
			if (outputStream == null) {
				outputStream = new BufferedOutputStream(Files.newOutputStream(resolveOutputPath()), 1024 * 64);
			}
			if (compressed || resolveOutputPath().toFile().getName().endsWith(".zip")) {
				outputStream = new ZipOutputStream(outputStream);
			}
			if (outputStream instanceof ZipOutputStream) {
				ZipOutputStream zos = (ZipOutputStream) outputStream;
				zos.setLevel(Deflater.BEST_SPEED);
				ZipEntry ze = new ZipEntry(resolveInputPath().toFile().getName().replace(".hprof", "-sanitized.hprof"));
				zos.putNextEntry(ze);
			}
			return outputStream;
		}

		private InputStream resolveInputStream() throws IOException {
			if (inputStream == null) {
				inputStream = new BufferedInputStream(Files.newInputStream(resolveInputPath(), StandardOpenOption.DELETE_ON_CLOSE), 1024 * 64);
			}
			return inputStream;
		}

		private Path resolveOutputPath() {
			if (outputPath != null) {
				return outputPath;
			}

			return outputPath = resolveInputPath().resolveSibling(StringUtils.removeEndIgnoreCase(resolveInputPath().toFile().getName(),".hprof") + "-sanitized" + (compressed ? ".zip" : ".hprof"));
		}

		private Path resolveInputPath() {
			Validate.notNull(inputPath, "Input path can't be null!");
			return inputPath;
		}
	}
}
