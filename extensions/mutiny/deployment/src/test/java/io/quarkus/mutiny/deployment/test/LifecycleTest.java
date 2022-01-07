package io.quarkus.mutiny.deployment.test;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class LifecycleTest {
	
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(CloseableResource.class, CloseableResourceProducer.class));

    @Inject
    CloseableResource rez;

    @Test
    public void testUni() {
        rez.useThisResource();
    }

}
