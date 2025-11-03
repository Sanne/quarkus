package io.quarkus.deployment.runnerjar;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ModuleOpenBuildItem;

/**
 * Verifies that the fast-jar generated Jar has a META-INF/MANIFEST.MF containing the
 * matching Add-Opens entries from the ModuleOpenBuildItem(s) defined by all available extensions.
 * N.B. we have an additional ModuleOpenBuildItem defined in this test to verify extensibility.
 */
public class FastJarManifestEntryTest extends BootstrapFromOriginalJarTestBase {

    @Override
    protected TsArtifact composeApplication() {
        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties());
    }

    @Override
    protected void testBootstrap(QuarkusBootstrap creator) throws Exception {
        final CuratedApplication curated = creator.bootstrap();
        AugmentResult app = curated.createAugmentor().createProductionApplication();
        final Path runnerJar = app.getJar().getPath();
        assertTrue(Files.exists(runnerJar));
        try (JarFile jar = new JarFile(runnerJar.toFile())) {
            final Attributes mainAttrs = jar.getManifest().getMainAttributes();
            //This will have to contain the combination of ModuleOpenBuildItem(s) defined in this test and also
            //the other ones implied by the core module (see JBossThreadsProcessor):
            assertEquals("java.base/java.lang testing.notrealmodule/org.example", mainAttrs.getValue("Add-Opens"));
        }
    }

    @Override
    protected Properties buildSystemProperties() {
        var props = new Properties();
        props.setProperty("quarkus.package.jar.type", "fast-jar");
        return props;
    }

    //We define an additional ModuleOpenBuildItem to verify extensibility & composeability
    public static class AdditionalBuildItem {
        @BuildStep
        ModuleOpenBuildItem additionalModuleOpen() {
            return new ModuleOpenBuildItem("testing.notrealmodule", "some.other.module", "org.example");
        }
    }

}
