package dev.uncandango.heapsanitizer.sanitizer;

import dev.uncandango.heapsanitizer.HeapSanitizer;
import dev.uncandango.heapsanitizer.fixture.HeapDumper;
import dev.uncandango.heapsanitizer.fixture.ResourceTool;
import dev.uncandango.heapsanitizer.sanitizer.example.ClassWithManyInstanceFields;
import dev.uncandango.heapsanitizer.sanitizer.example.ClassWithManyStaticFields;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static dev.uncandango.heapsanitizer.fixture.ByteArrayTool.*;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@TestMethodOrder(Random.class)
public class HeapDumpSanitizerTest {

	@TempDir
	static Path tempDir;

	// "his-secret-value" with each letter incremented by 1
	private final String hisSecretValue = "ijt.tfdsfu.wbmvf";
	private final String herSecretValue = "ifs.tfdsfu.wbmvf";
	private final String itsSecretValue = "jut.tfdsfu.wbmvf";

	// "his-classified-value" with each letter incremented by 1
	private final String hisClassifiedValue = "ijt.dmbttjgjfe.wbmvf";
	private final String herClassifiedValue = "ifs.dmbttjgjfe.wbmvf";
	private final String itsClassifiedValue = "jut.dmbttjgjfe.wbmvf";

	private final SecretArrays secretArrays = new SecretArrays();

	@BeforeEach
	public void beforeEach(final TestInfo info) {
		HeapSanitizer.LOGGER.info("Test - {}:", info.getDisplayName());
	}

	@Test
	@DisplayName("testSecretsAreInHeapDump. Verify that heap dump normally contains sensitive data")
	public void testSecretsAreInHeapDump() throws Exception {

		// keep as byte array in mem
		byte[] actualHisSecretValue = adjustLettersToByteArray(hisSecretValue);

		// keep as char array in mem
		String actualHerSecretValue = new String(actualHisSecretValue, UTF_8).replace("his", "her");

		// interned
		lengthenAndInternItsValue(actualHisSecretValue);

		actualHisSecretValue = lengthen(actualHisSecretValue, DataSize.ofMegabytes(1));
		actualHerSecretValue = lengthen(actualHerSecretValue, DataSize.ofMegabytes(1));

		final byte[] heapDump = loadHeapDump();

		final byte[] expectedHisSecretValueBytes = adjustLettersToByteArray(hisSecretValue);
		final byte[] expectedHerSecretValueBytes = adjustLettersToByteArray(herSecretValue);
		final byte[] expectedItsSecretValueBytes = adjustLettersToByteArray(itsSecretValue);

		assertThat(heapDump).overridingErrorMessage("sequences do not match") // normal error message would be long and not helpful at all
			.containsSequence(expectedHisSecretValueBytes).containsSequence(expectedHerSecretValueBytes).containsSequence(expectedItsSecretValueBytes).containsSequence(secretArrays.getByteArraySequence()).containsSequence(secretArrays.getCharArraySequence()).containsSequence(secretArrays.getShortArraySequence()).containsSequence(secretArrays.getIntArraySequence()).containsSequence(secretArrays.getLongArraySequence()).containsSequence(secretArrays.getFloatArraySequence()).containsSequence(secretArrays.getDoubleArraySequence()).containsSequence(secretArrays.getBooleanArraySequence());
	}

	private byte[] adjustLettersToByteArray(final String str) {
		return adjustLetters(str, -1).getBytes(UTF_8);
	}

	private void lengthenAndInternItsValue(final byte[] value) {
		String itsValue = new String(value, UTF_8).replace("his", "its");
		itsValue = lengthen(itsValue, DataSize.ofMegabytes(1));
		itsValue.intern();
	}

	private byte[] loadHeapDump() throws Exception {
		return loadHeapDump(triggerHeapDump());
	}

	private String adjustLetters(final String str, final int adjustment) {
		return str.chars().map(chr -> chr + adjustment).mapToObj(chr -> String.valueOf((char) chr)).collect(Collectors.joining(""));
	}

	private byte[] loadHeapDump(final Path heapDumpPath) throws IOException {
		final long size = Files.size(heapDumpPath);
		HeapSanitizer.LOGGER.info("Loading heap dump. size={} name={}", byteCountToDisplaySize(size), heapDumpPath.getFileName());
		return Files.readAllBytes(heapDumpPath);
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

	@Test
	@DisplayName("testConfidentialsNotInHeapDump. Verify that sanitized heap dump does not contains sensitive data")
	public void testConfidentialsNotInHeapDump() throws Exception {

		byte[] actualHisConfidentialValue = ResourceTool.bytesOf(getClass(), "classifieds.txt");
		String actualHerConfidentialValue = new String(actualHisConfidentialValue, UTF_8).replace("his", "her");
		lengthenAndInternItsValue(actualHisConfidentialValue);

		actualHisConfidentialValue = lengthen(actualHisConfidentialValue, DataSize.ofMegabytes(1));
		actualHerConfidentialValue = lengthen(actualHerConfidentialValue, DataSize.ofMegabytes(1));

		final byte[] heapDump = loadSanitizedHeapDump(SanitizeConfig.builder());

		final byte[] expectedHisClassifiedValueBytes = adjustLettersToByteArray(hisClassifiedValue);
		final byte[] expectedHerClassifiedValueBytes = adjustLettersToByteArray(herClassifiedValue);
		final byte[] expectedItsClassifiedValueBytes = adjustLettersToByteArray(itsClassifiedValue);

		verifyDoesNotContainsSequence(heapDump, expectedHisClassifiedValueBytes);
		verifyDoesNotContainsSequence(heapDump, expectedHerClassifiedValueBytes);
		verifyDoesNotContainsSequence(heapDump, expectedItsClassifiedValueBytes);

		verifyDoesNotContainsSequence(heapDump, secretArrays.getByteArraySequence());
		verifyDoesNotContainsSequence(heapDump, secretArrays.getCharArraySequence());

		// by default only byte and char arrays are sanitized
		assertThat(heapDump).overridingErrorMessage("sequences do not match") // normal error message would be long and not helpful at all
			.containsSequence(secretArrays.getShortArraySequence()).containsSequence(secretArrays.getIntArraySequence()).containsSequence(secretArrays.getLongArraySequence()).containsSequence(secretArrays.getFloatArraySequence()).containsSequence(secretArrays.getDoubleArraySequence()).containsSequence(secretArrays.getBooleanArraySequence());
	}

	private byte[] loadSanitizedHeapDump(SanitizeConfig.Builder configBuilder) throws Exception {
		final Path heapDump = triggerHeapDump();
		final Path sanitizedHeapDumpPath = newTempFilePath();

		HeapSanitizer.withConfig(configBuilder)
			.modifyConfig(config ->
				config.setInputPath(heapDump)
					.setOutputPath(sanitizedHeapDumpPath))
			.sanitize();

		return loadHeapDump(sanitizedHeapDumpPath);
	}

	private void verifyDoesNotContainsSequence(final byte[] big, final byte[] small) {
		final String corrId = System.currentTimeMillis() + "";
		assertThatCode(() -> {
			assertThat(big).withFailMessage(corrId).containsSequence(small);
		}).withFailMessage("does in fact contains sequence").hasMessageContaining(corrId);
	}

	@Test
	@DisplayName("testSanitizeFieldsOfNonArrayPrimitiveType. Verify that fields of non-array primitive type can be sanitized")
	public void testSanitizeFieldsOfNonArrayPrimitiveType() throws Exception {
		final Object instance = new ClassWithManyInstanceFields();
		final Object staticFields = new ClassWithManyStaticFields();
		assertThat(instance).isNotNull();
		assertThat(staticFields).isNotNull();

		byte[] sanitizedHeapDump = loadSanitizedHeapDump(SanitizeConfig.builder().sanitizeByteCharArraysOnly(false));
		verifyDoesNotContainsSequence(sanitizedHeapDump, nCopiesLongToBytes(deadcow(), 100));
		assertThat(countOfSequence(sanitizedHeapDump, nCopiesLongToBytes(cafegirl(), 1))).isLessThan(1000);

		sanitizedHeapDump = null;
		clearLoadedHeapDumpInfo();

		{
			final byte[] clearHeapDump = loadSanitizedHeapDump(SanitizeConfig.builder().sanitizeByteCharArraysOnly());
			assertThat(clearHeapDump).overridingErrorMessage("sequences do not match") // normal error message would be long and not helpful at all
				.containsSequence(nCopiesLongToBytes(deadcow(), 500));

			assertThat(countOfSequence(clearHeapDump, nCopiesLongToBytes(cafegirl(), 1))).isGreaterThan(500);
		}

		{
			final byte[] clearHeapDump = loadSanitizedHeapDump(SanitizeConfig.builder().sanitizeByteCharArraysOnly(false).sanitizeArraysOnly());
			assertThat(clearHeapDump).overridingErrorMessage("sequences do not match") // normal error message would be long and not helpful at all
				.containsSequence(nCopiesLongToBytes(deadcow(), 500));

			assertThat(countOfSequence(clearHeapDump, nCopiesLongToBytes(cafegirl(), 1))).isGreaterThan(500);
		}
	}

	// 0xDEADBEEF
	private long deadcow() {
		return 0xDEADBEEE + Long.parseLong("1");
	}

	// 0xCAFEBABE
	private long cafegirl() {
		return 0XCAFEBABD + Long.parseLong("1");
	}

	@BeforeEach
	@AfterEach
	public void clearLoadedHeapDumpInfo() {
		System.gc();
	}

	@Test
	public void testSanitizeArraysOnly() throws Exception {
		final byte[] heapDump = loadSanitizedHeapDump(SanitizeConfig.builder().sanitizeByteCharArraysOnly(false).sanitizeArraysOnly());
		verifyDoesNotContainsSequence(heapDump, secretArrays.getByteArraySequence());
		verifyDoesNotContainsSequence(heapDump, secretArrays.getCharArraySequence());
		verifyDoesNotContainsSequence(heapDump, secretArrays.getShortArraySequence());
		verifyDoesNotContainsSequence(heapDump, secretArrays.getIntArraySequence());
		verifyDoesNotContainsSequence(heapDump, secretArrays.getLongArraySequence());
		verifyDoesNotContainsSequence(heapDump, secretArrays.getFloatArraySequence());
		verifyDoesNotContainsSequence(heapDump, secretArrays.getDoubleArraySequence());
		verifyDoesNotContainsSequence(heapDump, secretArrays.getBooleanArraySequence());
	}

	private static class SecretArrays {
		private static final int LENGTH = 512;

		private final byte[] byteArray = new byte[LENGTH];
		private final char[] charArray = new char[LENGTH];
		private final short[] shortArray = new short[LENGTH];
		private final int[] intArray = new int[LENGTH];
		private final long[] longArray = new long[LENGTH];
		private final float[] floatArray = new float[LENGTH];
		private final double[] doubleArray = new double[LENGTH];
		private final boolean[] booleanArray = new boolean[LENGTH];

		{
			final ThreadLocalRandom random = ThreadLocalRandom.current();
			for (int i = 0; i < LENGTH; i++) {
				byteArray[i] = (byte) random.nextInt();
				charArray[i] = (char) random.nextInt();
				shortArray[i] = (short) random.nextInt();
				intArray[i] = random.nextInt();
				longArray[i] = random.nextLong();
				floatArray[i] = random.nextFloat();
				doubleArray[i] = random.nextDouble();
				booleanArray[i] = random.nextBoolean();
			}
		}

		public byte[] getByteArraySequence() {
			return byteArray;
		}

		public byte[] getCharArraySequence() {
			final ByteBuffer buffer = ByteBuffer.allocate(Character.BYTES * LENGTH);
			buffer.order(BIG_ENDIAN);
			for (int i = 0; i < LENGTH; i++) {
				buffer.putChar(i * Character.BYTES, charArray[i]);
			}
			return buffer.array();
		}

		public byte[] getShortArraySequence() {
			final ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES * LENGTH);
			buffer.order(BIG_ENDIAN);
			for (int i = 0; i < LENGTH; i++) {
				buffer.putShort(i * Short.BYTES, shortArray[i]);
			}
			return buffer.array();
		}

		public byte[] getIntArraySequence() {
			final ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES * LENGTH);
			buffer.order(BIG_ENDIAN);
			for (int i = 0; i < LENGTH; i++) {
				buffer.putInt(i * Integer.BYTES, intArray[i]);
			}
			return buffer.array();
		}

		public byte[] getLongArraySequence() {
			final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES * LENGTH);
			buffer.order(BIG_ENDIAN);
			for (int i = 0; i < LENGTH; i++) {
				buffer.putLong(i * Long.BYTES, longArray[i]);
			}
			return buffer.array();
		}

		public byte[] getFloatArraySequence() {
			final ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES * LENGTH);
			buffer.order(BIG_ENDIAN);
			for (int i = 0; i < LENGTH; i++) {
				buffer.putFloat(i * Float.BYTES, floatArray[i]);
			}
			return buffer.array();
		}

		public byte[] getDoubleArraySequence() {
			final ByteBuffer buffer = ByteBuffer.allocate(Double.BYTES * LENGTH);
			buffer.order(BIG_ENDIAN);
			for (int i = 0; i < LENGTH; i++) {
				buffer.putDouble(i * Double.BYTES, doubleArray[i]);
			}
			return buffer.array();
		}

		public byte[] getBooleanArraySequence() {
			final ByteBuffer buffer = ByteBuffer.allocate(LENGTH);
			buffer.order(BIG_ENDIAN);
			for (int i = 0; i < LENGTH; i++) {
				buffer.put(i, booleanArray[i] ? (byte) 1 : (byte) 0);
			}
			return buffer.array();
		}
	}
}
