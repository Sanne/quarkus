package io.quarkus.hibernate.orm.runtime.customized;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public final class QuarkusJtaPlatformInitiator implements StandardServiceInitiator<JtaPlatform> {

    public static final QuarkusJtaPlatformInitiator INSTANCE = new QuarkusJtaPlatformInitiator();
    private static final boolean jtaIsPresent = checkJtaPresence();

    private QuarkusJtaPlatformInitiator() {
    }

    @Override
    public JtaPlatform initiateService(Map map, ServiceRegistryImplementor serviceRegistryImplementor) {
        return buildJtaPlatformInstance();
    }

    public JtaPlatform buildJtaPlatformInstance() {
        return jtaIsPresent ? getJtaInstance() : getNoJtaInstance();
    }

    private NoJtaPlatform getNoJtaInstance() {
        return NoJtaPlatform.INSTANCE;
    }

    private QuarkusJtaPlatform getJtaInstance() {
        return QuarkusJtaPlatform.INSTANCE;
    }

    @Override
    public Class<JtaPlatform> getServiceInitiated() {
        return JtaPlatform.class;
    }

    /**
     * This isn't using the "Capabilities" feature from Quarkus as we need the jtaIsPresent field
     * to be initialized as a fully static constant for DCO to work its magic in native-image;
     * in particular this necessary as otherwise the QuarkusJtaPlatform would trigger a compilation
     * failure caused by the broken classpath.
     * We could generate such a constant from the capability presence, but that seemed overkill.
     *
     * @return true if the Narayana implementation is available on the classpath
     */
    private static boolean checkJtaPresence() {
        try {
            Class.forName("com.arjuna.ats.jta.TransactionManager");
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

}
