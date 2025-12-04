package io.quarkus.deployment.jvm;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.changeagent.ClassChangeAgent;
import io.quarkus.deployment.builditem.ModuleOpenBuildItem;
import net.bytebuddy.agent.ByteBuddyAgent;

final class AgentBasedModulesReconfigurer implements JvmModulesReconfigurer {

    private static final Logger logger = Logger.getLogger("io.quarkus.deployment.jvm");

    private final Instrumentation instrumentation;

    /**
     * Initializes, attempting to find or load an `Instrumentation` instance:
     * first we check if the `ClassChangeAgent` is attached - in which case
     * we can use it.
     * Otherwise we'll proceed to attaching a new agent leveraging the
     * self-attaching strategy from Byte Buddy.
     * If an agent cannot be installed, an {@link IllegalStateException} is thrown.
     */
    AgentBasedModulesReconfigurer() {
        Instrumentation existingIntrumentation = ClassChangeAgent.getInstrumentation();
        if (existingIntrumentation != null) {
            this.instrumentation = existingIntrumentation;
        } else {
            // ByteBuddyAgent.install() attaches its own agent to the current
            // JVM and returns the Instrumentation instance.
            try {
                instrumentation = ByteBuddyAgent.install();
            } catch (IllegalStateException e) {
                throw new RuntimeException("Failed to install an agent in the running JVM. Please report this issue.", e);
            }
        }
    }

    @Override
    public void openJavaModules(List<ModuleOpenBuildItem> addOpens, ModulesClassloaderContext modulesContext) {
        if (addOpens.isEmpty())
            return;
        //We now need to aggregate the list into a differently organized data structure
        HashMap<Module, PerModuleOpenInstructions> aggregateByModule = new HashMap<>();
        for (ModuleOpenBuildItem m : addOpens) {
            Optional<Module> openedModuleOptional = modulesContext.findModule(m.openedModuleName());
            if (openedModuleOptional.isEmpty()) {
                warnModuleGetsSkipped(m.openedModuleName(), m);
                continue;
            }
            final Module openedModule = openedModuleOptional.get();
            PerModuleOpenInstructions perModuleOpenInstructions = aggregateByModule.computeIfAbsent(openedModule,
                    k -> new PerModuleOpenInstructions());
            Optional<Module> openingModuleNameOptional = modulesContext.findModule(m.openingModuleName());
            if (openingModuleNameOptional.isEmpty()) {
                warnModuleGetsSkipped(m.openingModuleName(), m);
                continue;
            }
            final Module openingModule = openingModuleNameOptional.get();
            for (String packageName : m.packageNames()) {
                perModuleOpenInstructions.addOpens(packageName, openingModule);
            }
        }
        //Now that we have a map of openings for each module, let's instrument each of them
        for (Map.Entry<Module, PerModuleOpenInstructions> entry : aggregateByModule.entrySet()) {
            addOpens(entry.getKey(), entry.getValue().modulesToOpenToByPackage);
        }
    }

    private static void warnModuleGetsSkipped(String m, ModuleOpenBuildItem addOpens) {
        logger.warnf("Module %s not found, skipping processing of ModuleOpenBuildItem: %s", m, addOpens);
    }

    /**
     * Uses the MethodHandle to open a package.
     *
     * @param sourceModule The module to open
     * @param openInstructions The map of packages / target modules to open to
     */
    private void addOpens(Module sourceModule, Map<String, Set<Module>> openInstructions) {
        try {
            // We are redefining the target module, adding a new "open"
            // rule for it.
            //This method is additive: we don't need to read previous reads and exports
            //to avoid losing them.
            instrumentation.redefineModule(
                    sourceModule, // The module to change
                    Set.of(), // Extra reads
                    Map.of(), // Extra exports
                    openInstructions, // The relevant one
                    Set.of(), // Extra uses
                    Map.of() // Extra provides
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to redefine module " + sourceModule.getName());
        }
    }

    // A convenience container to keep our logic above more readable
    private static class PerModuleOpenInstructions {
        private final Map<String, Set<Module>> modulesToOpenToByPackage = new HashMap<>();

        public void addOpens(final String packageName, final Module openingModule) {
            final Set<Module> modulesToOpenTo = modulesToOpenToByPackage.computeIfAbsent(packageName, k -> new HashSet<>());
            modulesToOpenTo.add(openingModule);
        }
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
