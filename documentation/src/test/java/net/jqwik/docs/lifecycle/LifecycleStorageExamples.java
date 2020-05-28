package net.jqwik.docs.lifecycle;

import java.io.*;
import java.util.*;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import net.jqwik.api.lifecycle.*;

import static org.assertj.core.api.Assertions.*;

@AddLifecycleHook(value = TemporaryFileHook.class, propagateTo = PropagationMode.ALL_DESCENDANTS)
class LifecycleStorageExamples {

	@Property(tries = 10)
	void canWriteToFile(File anyFile, @ForAll @AlphaChars @StringLength(min = 1) String fileContents) throws Exception {
		assertThat(anyFile).isEmpty();
		writeToFile(anyFile, fileContents);
		assertThat(anyFile).isNotEmpty();
	}

	@AfterTry
	void assertFileNotEmpty(File anyFile) {
		assertThat(anyFile).isNotEmpty();
	}

	private void writeToFile(File anyFile, String contents) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(anyFile));
		writer.write(contents);
		writer.close();
	}

}

class TemporaryFileHook implements ResolveParameterHook {

	public static final Tuple.Tuple2 STORE_IDENTIFIER = Tuple.of(TemporaryFileHook.class, "temporary files");

	@Override
	public Optional<ParameterSupplier> resolve(ParameterResolutionContext parameterContext, LifecycleContext lifecycleContext) {
		if (parameterContext.typeUsage().isOfType(File.class)) {
			return Optional.of(ignoreTry -> getTemporaryFileForTry());
		}
		return Optional.empty();
	}

	private File getTemporaryFileForTry() {
		Store<File> tempFileStore = Store.getOrCreate(STORE_IDENTIFIER, Lifespan.TRY, this::createTempFile);
		tempFileStore.onClose(file -> file.delete());
		return tempFileStore.get();
	}

	private File createTempFile() {
		try {
			return File.createTempFile("temp", ".txt");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}