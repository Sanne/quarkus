package io.quarkus.deployment.jvm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.deployment.builditem.ModuleOpenBuildItem;

public class ReflectiveHackModulesReconfigurer implements JvmModulesReconfigurer {

    private static final Logger log = Logger.getLogger(ReflectiveHackModulesReconfigurer.class);

    //Opted to not make this a final field, to allow for more graceful error handling
    private static volatile MethodHandle implAddOpensHandle;

    @Override
    public void openJavaModules(List<ModuleOpenBuildItem> addOpens) {
        if (addOpens.isEmpty())
            return;
        init();
        for (ModuleOpenBuildItem m : addOpens) {
            Module openedModule = m.openedModule();
            Module openingModule = m.openingModule();
            for (String packageName : m.packageNames()) {
                addOpens(openedModule, packageName, openingModule);
            }
        }
    }

    /**
     * Attempts to get a handle to the private implAddOpens method of Module;
     * this is normally sealed, so it MUST be run with: --add-opens=java.base/java.lang.invoke=ALL-UNNAMED
     * Once we have it, we have full access to reconfigure other modules.
     */
    private static void init() {
        if (implAddOpensHandle != null)
            return;

        try {
            //Get the super-privileged MethodHandles.Lookup instance (IMPL_LOOKUP):
            //this is necessary to access the otherwise sealed private implAddOpens method.
            Field lookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");

            //This setAccessible call is the part that would fail when the java.base module is not opened.
            lookupField.setAccessible(true);

            MethodHandles.Lookup privilegedLookup = (MethodHandles.Lookup) lookupField.get(null);

            //Signature of the method we want to find
            MethodType methodType = MethodType.methodType(void.class, String.class, Module.class);

            //Use the privileged lookup to find the private method
            implAddOpensHandle = privilegedLookup.findVirtual(
                    Module.class, // Class to find the method in
                    "implAddOpens", // Name of the private method
                    methodType // Signature of the method
            );

            log.debug("Successfully acquired MethodHandle for implAddOpens.");

        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InaccessibleObjectException e) {
            throw new RuntimeException("Failed to acquire handle to Module#implAddOpens. " +
                    "This must be run with JVM parameter '--add-opens=java.base/java.lang.invoke=ALL-UNNAMED'", e);
        }
    }

    /**
     * Uses the MethodHandle to open a package.
     *
     * @param sourceModule The module to open
     * @param packageName The package to open
     * @param targetModule The module to open to
     */
    private static void addOpens(Module sourceModule, String packageName, Module targetModule) {
        try {
            implAddOpensHandle.invokeExact(sourceModule, packageName, targetModule);
            log.debugf("Successfully opened module %s/%s to %s",
                    sourceModule.getName(), packageName, targetModule.isNamed() ? targetModule.getName() : "UNNAMED");
        } catch (Throwable e) {
            // MethodHandle.invokeExact throws Throwable
            throw new RuntimeException("Failed to invoke implAddOpens", e);
        }
    }

}
