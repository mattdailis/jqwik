package net.jqwik.properties;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.junit.platform.commons.support.*;
import org.junit.platform.commons.util.*;
import org.opentest4j.*;

public class GenericProperty {

	private final String name;
	private final List<Arbitrary> arbitraries;
	private final Function<List<Object>, Boolean> forAllFunction;

	public GenericProperty(String name, List<Arbitrary> arbitraries, Function<List<Object>, Boolean> forAllFunction) {
		this.name = name;
		this.arbitraries = arbitraries;
		this.forAllFunction = forAllFunction;
	}

	public PropertyCheckResult check(int tries, long seed) {
		List<Generator> generators = arbitraries.stream()
												.map(a1 -> a1.generator(seed, tries))
												.collect(Collectors.toList());
		int maxTries = generators.isEmpty() ? 1 : tries;
		int countChecks = 0;
		for (int countTries = 1; countTries <= maxTries; countTries++) {
			List<Object> params = generateParameters(generators);
			try {
				boolean check = forAllFunction.apply(params);
				countChecks++;
				if (!check) {
					return PropertyCheckResult.falsified(name, countTries, countChecks, seed, params);
				}
			} catch (TestAbortedException tae) {
				continue;
			} catch (Throwable throwable) {
				countChecks++;
				BlacklistedExceptions.rethrowIfBlacklisted(throwable);
				return PropertyCheckResult.erroneous(name, countTries, countChecks, seed, params, throwable);
			}
		}
		if (countChecks == 0)
			return PropertyCheckResult.exhausted(name, maxTries, seed);
		return PropertyCheckResult.satisfied(name, maxTries, countChecks, seed);
	}

	private List<Object> generateParameters(List<Generator> generators) {
		return generators.stream().map(Generator::next).collect(Collectors.toList());
	}
}
