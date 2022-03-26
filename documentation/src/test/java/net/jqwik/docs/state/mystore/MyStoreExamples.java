package net.jqwik.docs.state.mystore;

import java.util.*;

import net.jqwik.api.*;
import net.jqwik.api.state.*;

import static org.assertj.core.api.Assertions.*;

@PropertyDefaults(tries = 100)
public class MyStoreExamples {

	@Property(shrinking = ShrinkingMode.FULL, afterFailure = AfterFailureMode.RANDOM_SEED)
	void storeWorksAsExpected(@ForAll("storeActions") ActionChain<MyStore<Integer, String>> storeChain) {
		storeChain.run();
		// System.out.println(storeChain.transformations());
	}

	@Provide
	ActionChainArbitrary<MyStore<Integer, String>> storeActions() {
		return Chains.actionChains(
			MyStore::new,
			Tuple.of(3, new StoreAnyValue()),
			Tuple.of(1, new UpdateValue()),
			Tuple.of(1, new RemoveValue())
		);
	}

	static class StoreAnyValue implements Action<MyStore<Integer, String>> {
		@Override
		public Arbitrary<Transformer<MyStore<Integer, String>>> transformer() {
			return Combinators.combine(keys(), values())
							  .as((key, value) -> Transformer.mutate(
								  String.format("store %s=%s", key, value),
								  store -> {
									  store.store(key, value);
									  assertThat(store.isEmpty()).isFalse();
									  assertThat(store.get(key)).isEqualTo(Optional.of(value));
								  }
							  ));
		}
	}

	static class UpdateValue implements Action<MyStore<Integer, String>> {
		@Override
		public boolean precondition(MyStore<Integer, String> store) {
			return !store.isEmpty();
		}

		@Override
		public Arbitrary<Transformer<MyStore<Integer, String>>> transformer(MyStore<Integer, String> state) {
			Arbitrary<Integer> existingKeys = Arbitraries.of(state.keys());
			return Combinators.combine(existingKeys, values())
							  .as((key, value) -> Transformer.mutate(
								  String.format("update %s=%s", key, value),
								  store -> {
									  store.store(key, value);
									  assertThat(store.isEmpty()).isFalse();
									  assertThat(store.get(key)).isEqualTo(Optional.of(value));
								  }
							  ));
		}
	}

	static class RemoveValue implements Action<MyStore<Integer, String>> {
		@Override
		public boolean precondition(MyStore<Integer, String> store) {
			return !store.isEmpty();
		}

		@Override
		public Arbitrary<Transformer<MyStore<Integer, String>>> transformer(MyStore<Integer, String> state) {
			Arbitrary<Integer> existingKeys = Arbitraries.of(state.keys());
			return existingKeys.map(key -> Transformer.mutate(
				String.format("remove %s", key),
				store -> {
					store.remove(key);
					assertThat(store.get(key)).isNotPresent();
				}
			));
		}
	}

	private static Arbitrary<Integer> keys() {
		return Arbitraries.integers().between(1, Integer.MAX_VALUE);
	}

	private static Arbitrary<String> values() {
		return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10);
	}
}
