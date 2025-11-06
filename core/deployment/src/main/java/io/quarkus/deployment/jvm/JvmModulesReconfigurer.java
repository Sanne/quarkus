package io.quarkus.deployment.jvm;

import java.util.List;

import io.quarkus.deployment.builditem.ModuleOpenBuildItem;

/**
 * Interface for reconfiguring JVM module restrictions on the running JVM.
 * It's an interface as I expect us to possibly explore different strategies
 * to accomplish this.
 */
public interface JvmModulesReconfigurer {
    void openJavaModules(List<ModuleOpenBuildItem> addOpens);
}
