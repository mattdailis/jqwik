package net.jqwik.api.constraints;

import java.lang.annotation.*;

/**
 * Constrain the chars used to generate Strings or Characters to upper and lower case chars.
 *
 * Applies to String or Character parameters which are also annotated with {@code @ForAll}.
 *
 * When used with String parameters it can be combined with other char constraining annotations.
 * In that case the set of possible characters is expanded by each annotation.
 *
 * @see net.jqwik.api.ForAll
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@LowerChars
@UpperChars
@Documented
public @interface AlphaChars {
}
