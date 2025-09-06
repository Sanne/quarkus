package io.quarkus.deployment.jvm;

import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.deployment.builditem.ModuleOpenBuildItem;

/**
 * Interface for reconfiguring JVM module restrictions on the running JVM.
 * It's an interface as I expect us to possibly explore different strategies
 * to accomplish this.
 */
public interface JvmModulesReconfigurer {

    void openJavaModules(List<ModuleOpenBuildItem> addOpens);

    /**
     * Creates a new instance of {@link JvmModulesReconfigurer}.
     *
     * Initialization of such services is fairly costly: try
     * to avoid it, and aim to reuse the produced instance.
     *
     * @return a new {@link JvmModulesReconfigurer} instance
     */
    static JvmModulesReconfigurer create() {
        final Logger logger = Logger.getLogger("io.quarkus.deployment.jvm");
        JvmModulesReconfigurer reconfigurer = null;
        try {
            reconfigurer = new ReflectiveAccessModulesReconfigurer();
        } catch (RuntimeException e) {
            logger.warn(
                    "This Quarkus development instance was not run with '--add-opens=java.base/java.lang.invoke=ALL-UNNAMED'. "
                            +
                            "We recommend using this flag for an improved development experience. " +
                            "Fallback strategy triggered: we're going to try loading a self-attaching agent to make up for it.");
        }
        if (reconfigurer != null) { //to avoid nesting try/catch blocks
            return reconfigurer;
        }
        try {
            reconfigurer = new AgentBasedModulesReconfigurer();
        } catch (RuntimeException e) {
            logger.warn(
                    "Unable to install an agent in the running JVM. Please report this issue. This Quarkus instance will not be able"
                            +
                            " to reconfigure JVM parameters automatically, which might limit compatibility with libraries which aren't fully"
                            +
                            " ready for the Java modules system. More details might be logger later as they get hit.");
        }
        if (reconfigurer != null) { //to avoid nesting try/catch blocks
            return reconfigurer;
        }

        return new FallbackModulesReconfigurer();
    }

}
