package dev.uncandango.heapsanitizer;

import dev.uncandango.heapsanitizer.sanitizer.SanitizeConfig;
import dev.uncandango.heapsanitizer.sanitizer.SanitizeConfigProcessor;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

public class HeapSanitizer {
	public static final Logger LOGGER = LoggerFactory.getLogger(HeapSanitizer.class);

	private SanitizeConfig.Builder config = SanitizeConfig.builder();

	public HeapSanitizer modifyConfig(Consumer<SanitizeConfig.Builder> config){
		config.accept(this.config);
		return this;
	}

	public static HeapSanitizer withConfig(SanitizeConfig.Builder config){
		HeapSanitizer hs = new HeapSanitizer();
		hs.config = config;
		return hs;
	}

	public static HeapSanitizer withInput(Path fileInput){
		HeapSanitizer hs = new HeapSanitizer();
		hs.config.setInputPath(fileInput);
		return hs;
	}

	public Path sanitize() throws IOException {
		new SanitizeConfigProcessor(config.build()).process();
		return config.getOutputPath();
	}
}
