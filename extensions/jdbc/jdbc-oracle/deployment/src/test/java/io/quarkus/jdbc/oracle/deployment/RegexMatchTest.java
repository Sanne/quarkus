package io.quarkus.jdbc.oracle.deployment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import io.smallrye.common.constraint.Assert;

public class RegexMatchTest {

    @Test
    public void jarRegexIsMatching() {
        final String EXAMPLE_CLASSPATH = "/home/sanne/sources/quarkus/integration-tests/jpa-oracle/target/quarkus-integration-test-jpa-oracle-999-SNAPSHOT-native-image-source-jar/lib/com.oracle.database.jdbc.ojdbc11-21.1.0.0.jar";
        final Pattern pattern = Pattern.compile(OracleReflections.DRIVER_JAR_MATCH_REGEX);
        final Matcher matcher = pattern.matcher(EXAMPLE_CLASSPATH);
        Assert.assertTrue(matcher.find());
    }

    @Test
    public void resourceRegexIsMatching() {
        final String EXAMPLE_RESOURCENAME = "/META-INF/native-image/native-image.properties";
        final Pattern pattern = Pattern.compile(OracleReflections.NATIVE_IMAGE_RESOURCE_MATCH_REGEX);
        final Matcher matcher = pattern.matcher(EXAMPLE_RESOURCENAME);
        Assert.assertTrue(matcher.find());
    }

}
