package io.quarkus.deployment.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.spi.Converter;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.util.ServiceUtil;
import io.smallrye.config.Converters;

/**
 * This test exists to verify that the constant field
 * ConfigGenerationBuildStep#BUILT_IN_CONVERTER_TYPES
 * is aligned with the content of the Smallrye Config version we depend on,
 * specifically it needs to match the union of:
 * - all converters included by default in Smallrye Config (field ALL_CONVERTERS in class io.smallrye.config.Converters)
 * - all additional converters provided by Quarkus by default (listed in
 * quarkus/core/runtime/src/main/resources/META-INF/services/org.eclipse.microprofile.config.spi.Converter)
 * If this test fails, it implies the hardcoded list in BUILT_IN_CONVERTER_TYPES needs to be updated.
 */
class ConfigGenerationBuildStepTest {

    private static final String CONVERTER_SERVICES = "META-INF/services/" + Converter.class.getName();

    @Test
    @SuppressWarnings("unchecked")
    void builtInConverterTypesMatchSmallRyeConfig() throws Exception {
        Set<String> actual = new TreeSet<>();
        actual.addAll(smallRyeBuiltInConverterTypes());
        actual.addAll(quarkusServiceLoadedConverterTypes());

        Field quarkusField = ConfigGenerationBuildStep.class.getDeclaredField("BUILT_IN_CONVERTER_TYPES");
        quarkusField.setAccessible(true);
        Set<String> hardcoded = (Set<String>) quarkusField.get(null);

        assertThat(new TreeSet<>(hardcoded))
                .as("BUILT_IN_CONVERTER_TYPES must match the union of SmallRye Converters.ALL_CONVERTERS "
                        + "and the set of converters provided by Quarkus core - update the hardcoded set if this fails")
                .isEqualTo(actual);
    }

    @SuppressWarnings("unchecked")
    private static Set<String> smallRyeBuiltInConverterTypes() throws Exception {
        Field smallryeField = Converters.class.getDeclaredField("ALL_CONVERTERS");
        smallryeField.setAccessible(true);
        Map<Type, ?> allConverters = (Map<Type, ?>) smallryeField.get(null);
        return allConverters.keySet().stream()
                .filter(t -> t instanceof Class<?>)
                .map(t -> ((Class<?>) t).getName())
                .collect(Collectors.toSet());
    }

    /**
     * The converter types Quarkus core registers via the {@link Converter} SPI. Only the Quarkus core
     * runtime declares such a service file on this module's test classpath, so this resolves to the
     * contents of {@code core/runtime/src/main/resources/META-INF/services/org.eclipse.microprofile.config.spi.Converter}.
     */
    private static Set<String> quarkusServiceLoadedConverterTypes() throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<String> converterNames = ServiceUtil.classNamesNamedIn(classLoader, CONVERTER_SERVICES);
        assertThat(converterNames)
                .as("The Quarkus core Converter service file should be on the test classpath")
                .isNotEmpty();

        Set<String> types = new TreeSet<>();
        for (String converterName : converterNames) {
            Class<?> converterClass = Class.forName(converterName, false, classLoader);
            Type converterType = Converters.getConverterType(converterClass);
            assertThat(converterType)
                    .as("Unable to resolve the converted type of " + converterName)
                    .isInstanceOf(Class.class);
            types.add(((Class<?>) converterType).getName());
        }
        return types;
    }
}
