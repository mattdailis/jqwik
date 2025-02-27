package net.jqwik.api.support;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import org.apiguardian.api.*;
import org.junit.platform.commons.support.*;

import net.jqwik.api.*;

import static org.apiguardian.api.API.Status.*;

@API(status = INTERNAL)
public class LambdaSupport {

	private LambdaSupport() {}

	/**
	 * This method is used in {@linkplain Object#equals(Object)} implementations of {@linkplain Arbitrary} types
	 * to allow memoization of generators.
	 * <p>
	 * Comparing two lambdas by their implementation class works if they don't access an enclosing object's state.
	 * When in doubt, fail comparison.
	 **/
	public static <T> boolean areEqual(T l1, T l2) {
		if (l1 == l2) return true;
		Class<?> l1Class = l1.getClass();
		if (l1Class != l2.getClass()) return false;
		if (l1 instanceof Serializable) {
			try {
				return Arrays.equals(serialize(l1), serialize(l2));
			} catch (IOException e) {
				// ignore
			}
		}
		// Check enclosed state the hard way
		for (Field field : l1Class.getDeclaredFields()) {
			if (!fieldIsEqualIn(field, l1, l2)) {
				return false;
			}
		}
		return true;
	}

	private static <T> byte[] serialize(T l1) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream);
		outputStream.writeObject(l1);
		return byteArrayOutputStream.toByteArray();
	}

	private static boolean fieldIsEqualIn(Field field, Object left, Object right) {
		field.setAccessible(true);
		try {
			// If field is a functional type use LambdaSupport.areEqual().
			// TODO: Could there be circular references among functional types?
			if (isFunctionalType(field.getType())) {
				return areEqual(field.get(left), field.get(right));
			}
			return field.get(left).equals(field.get(right));
		} catch (IllegalAccessException e) {
			return false;
		}
	}

	// TODO: This duplicates JqwikReflectionSupport.isFunctionalType() because module dependencies
	private static boolean isFunctionalType(Class<?> candidateType) {
		if (!candidateType.isInterface()) {
			return false;
		}
		return countInterfaceMethods(candidateType) == 1;
	}

	private static long countInterfaceMethods(Class<?> candidateType) {
		Method[] methods = candidateType.getMethods();
		return findInterfaceMethods(methods).size();
	}

	private static List<Method> findInterfaceMethods(Method[] methods) {
		return Arrays
				   .stream(methods)
				   .filter(m -> !m.isDefault() && !ModifierSupport.isStatic(m))
				   .collect(Collectors.toList());
	}

}
