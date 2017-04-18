package net.jqwik.properties;

import java.util.*;
import java.util.function.*;

public interface RandomGenerator<T> {

	T next(Random random);

	default RandomGenerator<T> filter(Predicate<? super T> predicate) {
		return random -> {
			while (true) {
				T value = RandomGenerator.this.next(random);
				if (predicate.test(value))
					return value;
			}
		};
	};
}
