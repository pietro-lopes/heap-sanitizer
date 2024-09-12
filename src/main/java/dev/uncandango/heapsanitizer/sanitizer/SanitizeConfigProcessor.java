package dev.uncandango.heapsanitizer.sanitizer;

import dev.uncandango.heapsanitizer.HeapSanitizer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SanitizeConfigProcessor {
	private final SanitizeConfig config;

	public SanitizeConfigProcessor(SanitizeConfig config) {
		this.config = config;
	}

	public void process() {
		try (InputStream is = config.getInputStream();
			 OutputStream os = config.getOutputStream()) {

			HeapDumpSanitizer sanitizer = new HeapDumpSanitizer();
			sanitizer.setInputStream(is);
			sanitizer.setOutputStream(os);
			sanitizer.setProgressMonitor(config.getProgressMonitor());
			sanitizer.setSanitizationText(config.getSanitizationText());
			sanitizer.setSanitizeArraysOnly(config.isSanitizeArraysOnly());
			sanitizer.setSanitizeByteCharArraysOnly(config.isSanitizeByteCharArraysOnly());

			sanitizer.sanitize();
		} catch (IOException e) {
			HeapSanitizer.LOGGER.error("Error while sanitizing file.", e);
		}
	}
}
