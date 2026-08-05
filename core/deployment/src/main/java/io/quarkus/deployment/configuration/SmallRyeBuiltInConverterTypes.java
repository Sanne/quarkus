package io.quarkus.deployment.configuration;

import java.util.Set;

/**
 * Types with a SmallRye Config built-in converter (Converters.ALL_CONVERTERS) that Quarkus core does NOT
 * also register a converter for via the Converter SPI - these need no reflection registration at all,
 * regardless of whether they're used as a {@code @ConfigMapping} property type or a direct
 * {@code @ConfigProperty} injection point: SmallRye resolves their converter without going through
 * reflection.
 * <p>
 * Types Quarkus DOES register an SPI converter for (e.g. Duration, Locale, InetAddress, Pattern) are
 * deliberately excluded here, even where SmallRye also has a built-in: the generated config builder
 * resolves the SPI converter's target type via {@code Class.forName(String)} at runtime (see
 * {@code AbstractConfigBuilder#withConverter}), and on Mandrel that resolution is only reliable when the
 * class has at least its public methods registered - constructors-only is not enough. Types with no
 * explicit converter at all also need this, since SmallRye then probes implicit converter methods
 * ({@code valueOf}, {@code parse}, {@code of}, ...), which per the MicroProfile Config spec are always
 * public.
 * <p>
 * Copied as a constant to avoid the processing overhead of computing the list, as it rarely changes - kept
 * in sync via {@code SmallRyeBuiltInConverterTypesTest}.
 */
public final class SmallRyeBuiltInConverterTypes {

    public static final Set<String> NAMES = Set.of(
            "java.lang.String",
            "java.lang.Boolean",
            "java.lang.Double",
            "java.lang.Float",
            "java.lang.Long",
            "java.lang.Integer",
            "java.lang.Short",
            "java.lang.Byte",
            "java.lang.Character",
            "java.lang.Class",
            "java.util.UUID",
            "java.util.Currency",
            "java.util.BitSet",
            "java.io.File",
            "java.net.URI",
            "java.time.format.DateTimeFormatter",
            "java.lang.CharSequence",
            "java.util.OptionalInt",
            "java.util.OptionalLong",
            "java.util.OptionalDouble");

    public static boolean isBuiltIn(String typeName) {
        return NAMES.contains(typeName);
    }

    private SmallRyeBuiltInConverterTypes() {
    }
}
