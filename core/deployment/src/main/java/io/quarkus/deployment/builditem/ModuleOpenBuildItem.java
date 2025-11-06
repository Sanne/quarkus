package io.quarkus.deployment.builditem;

import java.util.Objects;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This will generate the equivalent of "--add-opens [openedModule/package(s)]=[openingModule]" for
 * all runners of the generated application.
 * Some limitations apply which will depend on the launch mode of Quarkus; specifically, when
 * generating a runnable Jar we can only open a module to ALL-UNNAMED.
 */
public final class ModuleOpenBuildItem extends MultiBuildItem {

    private final String openedModuleName;
    private final String openingModuleName;
    private final Set<String> packageNames;

    /**
     * Create a new ModuleOpenBuildItem instance.
     *
     * @param openedModuleName The module which needs to be opened.
     * @param openingModuleName The module which is being granted access.
     * @param packageNames The packages which are being opened. At least one must be specified.
     */
    public ModuleOpenBuildItem(String openedModuleName, String openingModuleName, String... packageNames) {
        //Validating for actual module existence is deferred
        this.openedModuleName = Objects.requireNonNull(openedModuleName);
        this.openingModuleName = Objects.requireNonNull(openingModuleName);
        this.packageNames = Set.of(packageNames);
        if (packageNames.length == 0) {
            throw new IllegalArgumentException("At least one package name must be specified");
        }
    }

    public String openedModuleName() {
        return openedModuleName;
    }

    /**
     * Convenience method to load the module matching the opened module name.
     * Be careful: the named module might not exist.
     *
     * @return the Module instance corresponding to the opened module name
     * @throws RuntimeException if the module cannot be found or constructed
     */
    public Module openedModule() {
        return requireModule(openedModuleName);
    }

    public String openingModuleName() {
        return openingModuleName;
    }

    /**
     * Convenience method to load the module matching the opening module name.
     * Be careful: the named module might not exist.
     *
     * @return the Module instance corresponding to the opening module name
     * @throws RuntimeException if the module cannot be found or constructed
     */
    public Module openingModule() {
        return requireModule(openingModuleName);
    }

    public Set<String> packageNames() {
        return packageNames;
    }

    private static Module requireModule(final String moduleName) {
        Module module = ModuleLayer.boot().findModule(moduleName).orElse(null);
        if (module == null) {
            throw new RuntimeException("Module '" + moduleName
                    + "' has been named for an --add-opens instruction, but the module could not be found");
        }
        return module;
    }

}
