package io.quarkus.deployment.steps;

import java.util.Collection;
import java.util.List;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ExtensionDevModeConfig;
import io.quarkus.bootstrap.model.JvmOption;
import io.quarkus.builder.BuildException;
import io.quarkus.deployment.ResolvedJVMRequirements;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.ModuleOpenBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;

/**
 * Build step that resolves and aggregates JVM requirements for the Quarkus application.
 * <p>
 * This build step processes module open requirements (--add-opens) that are needed
 * at runtime for the generated application.
 * More JVM requirements in the same ballpark might be added in the future.
 */
public class JvmRequirementsBuildStep {

    /**
     * Resolves JVM requirements from the collected module open build items.
     * <p>
     * This method aggregates all {@link ModuleOpenBuildItem}s that have been produced
     * during the build process and creates a {@link ResolvedJVMRequirements} build item
     * containing the consolidated requirements.
     *
     * @param addOpens the list of modules that need to be opened.
     * @return a resolved JVM requirements build item containing all JVM requirements.
     */
    @BuildStep
    ResolvedJVMRequirements resolveJVMRequirements(final List<ModuleOpenBuildItem> addOpens) throws BuildException {
        return new ResolvedJVMRequirements(addOpens);
    }

    @BuildStep
    void remapModuleConfigurationFromExtensionMetadata(CurateOutcomeBuildItem curateOutcomeBuildItem,
            BuildProducer<CapabilityBuildItem> producer,
            BuildProducer<ModuleOpenBuildItem> moduleOpenProducer) {
        final ApplicationModel appModel = curateOutcomeBuildItem.getApplicationModel();
        for (ExtensionDevModeConfig devModeConfig : appModel.getExtensionDevModeConfig()) {
            for (JvmOption jvmOption : devModeConfig.getJvmOptions()) {
                String optionName = jvmOption.getName();
                Collection<String> optionValues = jvmOption.getValues();
            }
        }

    }

}
