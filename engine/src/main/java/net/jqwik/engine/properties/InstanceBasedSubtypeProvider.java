package net.jqwik.engine.properties;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import net.jqwik.api.*;
import net.jqwik.api.providers.*;
import net.jqwik.api.support.*;

import static net.jqwik.engine.support.JqwikReflectionSupport.*;
import static net.jqwik.engine.support.OverriddenMethodAnnotationSupport.*;

abstract class InstanceBasedSubtypeProvider implements ArbitraryProvider.SubtypeProvider {

	private final Object instance;

	protected InstanceBasedSubtypeProvider(Object instance) {
		this.instance = instance;
	}

	@Override
	public Set<Arbitrary<?>> apply(TypeUsage targetType) {
		Optional<ForAll> optionalForAll = targetType.findAnnotation(ForAll.class);
		Optional<String> optionalForAllValue = optionalForAll
			.map(ForAll::value).filter(name -> !name.equals(ForAll.NO_VALUE));

		// Using Optional and map and filter for that destroys the type for some reason
		Optional<Class<? extends ArbitrarySupplier<?>>> optionalForAllSupplier = Optional.empty();
		if (optionalForAll.isPresent()) {
			Class<? extends ArbitrarySupplier<?>> supplier = optionalForAll.get().supplier();
			if (!supplier.equals(ArbitrarySupplier.NONE.class)) {
				optionalForAllSupplier = Optional.of(supplier);
			}
		}

		Optional<From> optionalFrom = targetType.findAnnotation(From.class);
		Optional<String> optionalFromValue = optionalFrom
			.map(From::value).filter(name -> !name.equals(ForAll.NO_VALUE));

		// Using Optional and map and filter for that destroys the type for some reason
		Optional<Class<? extends ArbitrarySupplier<?>>> optionalFromSupplier = Optional.empty();
		if (optionalFrom.isPresent()) {
			Class<? extends ArbitrarySupplier<?>> supplier = optionalFrom.get().supplier();
			if (!supplier.equals(ArbitrarySupplier.NONE.class)) {
				optionalFromSupplier = Optional.of(supplier);
			}
		}

		long countSpecs = Stream.of(optionalForAllValue, optionalForAllSupplier, optionalFromValue, optionalFromSupplier)
								.filter(Optional::isPresent)
								.map(Optional::get).count();

		Set<Arbitrary<?>> resolvedArbitraries;
		if (countSpecs == 0) {
			resolvedArbitraries = resolve(targetType);
		} else if (countSpecs == 1) {
			if (optionalForAllValue.isPresent() || optionalFromValue.isPresent()) {
				resolvedArbitraries = resolveFromGeneratorName(targetType, optionalForAllValue, optionalFromValue);
			} else {
				resolvedArbitraries = resolveFromSupplier(targetType, optionalForAllSupplier, optionalFromSupplier);
			}
		} else {
			return onlyOneSpecAllowedError(targetType, countSpecs);
		}

		return resolvedArbitraries
				   .stream()
				   .map(arbitrary -> configure(arbitrary, targetType))
				   .filter(Objects::nonNull)
				   .collect(CollectorsSupport.toLinkedHashSet());
	}

	private Set<Arbitrary<?>> resolveFromSupplier(
		TypeUsage targetType,
		Optional<Class<? extends ArbitrarySupplier<?>>> optionalForAllSupplier,
		Optional<? extends Class<? extends ArbitrarySupplier<?>>> optionalFromSupplier
	) {
		Class<? extends ArbitrarySupplier<?>> supplierClass =
			optionalForAllSupplier.orElseGet(() -> optionalFromSupplier.orElseThrow(() -> new JqwikException("Should never happen")));
		ArbitrarySupplier<?> supplier = newInstanceInTestContext(supplierClass, instance);
		Arbitrary<?> arbitrary = supplier.supplyFor(targetType);
		if (arbitrary == null) {
			String message = String.format(
				"Supplier [%s] for type [%s] returns null but should return an arbitrary",
				supplierClass,
				targetType
			);
			throw new JqwikException(message);
		}
		return Collections.singleton(arbitrary);
	}

	private Set<Arbitrary<?>> resolveFromGeneratorName(
		TypeUsage targetType,
		Optional<String> optionalForAllValue,
		Optional<String> optionalFromValue
	) {
		String generatorName = optionalForAllValue.orElseGet(() -> optionalFromValue.orElseThrow(() -> new JqwikException("Should never happen")));
		return findArbitraryGeneratorByName(targetType, generatorName)
			.map(providerMethod -> invokeProviderMethod(providerMethod, targetType))
			.orElse(Collections.emptySet());
	}

	private Set<Arbitrary<?>> onlyOneSpecAllowedError(TypeUsage targetType, long countSpecs) {
		String message = String.format(
			"You can only have one arbitrary specification per parameter, but you have %s:%n%s",
			countSpecs,
			targetType
		);
		throw new JqwikException(message);
	}

	private Set<Arbitrary<?>> invokeProviderMethod(Method providerMethod, TypeUsage targetType) {
		return new ProviderMethodInvoker(providerMethod, targetType, instance, this).invoke();
	}

	private Optional<Method> findArbitraryGeneratorByName(TypeUsage typeUsage, String generatorToFind) {
		if (generatorToFind.isEmpty())
			return Optional.empty();

		Function<Method, String> generatorNameSupplier = method -> {
			Optional<Provide> provideAnnotation = findDeclaredOrInheritedAnnotation(method, Provide.class);
			return provideAnnotation.map(Provide::value).orElse("");
		};
		TypeUsage targetArbitraryType = TypeUsage.of(Arbitrary.class, typeUsage);

		return findGeneratorMethod(generatorToFind, this.instance.getClass(), Provide.class, generatorNameSupplier, targetArbitraryType);
	}

	protected abstract Set<Arbitrary<?>> resolve(TypeUsage parameterType);

	protected abstract Arbitrary<?> configure(Arbitrary<?> arbitrary, TypeUsage targetType);

}
