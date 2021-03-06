package io.quarkus.it.bouncycastle;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.util.JavaVersionUtil;
import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
public class BouncyCastleJsseITCase extends BouncyCastleJsseTestCase {
    @Test
    @Override
    public void testListProviders() {
        if (!JavaVersionUtil.isJava11OrHigher()) {
            LOG.trace("Skipping BouncyCastleJsseITCase, Java version is older than 11");
            return;
        }
        doTestListProviders();
        checkLog(true);
    }
}
