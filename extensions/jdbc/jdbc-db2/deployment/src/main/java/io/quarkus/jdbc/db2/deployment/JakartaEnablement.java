package io.quarkus.jdbc.db2.deployment;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.impl.*;
import org.eclipse.transformer.util.FileUtils;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;

public class JakartaEnablement {

    @BuildStep
    void transformToJakarta(BuildProducer<BytecodeTransformerBuildItem> transformers) {
        for (String classname : new String[] { "com.ibm.db2.jcc.t2zos.ab", "com.ibm.db2.jcc.t2zos.T2zosConnection",
                "com.ibm.db2.jcc.t2zos.T2zosConfiguration" }) {
            final BytecodeTransformerBuildItem item = new BytecodeTransformerBuildItem.Builder()
                    .setCacheable(false)
                    .setContinueOnFailure(false)
                    .setClassToTransform(classname)
                    .setClassReaderOptions(ClassReader.SKIP_DEBUG)
                    .setInputTransformer(this::jakartaTransformer)
                    .build();
            transformers.produce(item);
        }
    }

    private byte[] jakartaTransformer(String name, byte[] bytes) {
        final Logger logger = LoggerFactory.getLogger("JakartaTransformer");
        logger.info("Jakarta EE compatibility: transforming " + name);
        Map<String, String> renames = new HashMap<>();
        renames.put("javax.transaction", "jakarta.transaction");
        ActionContext ctx = new ActionContextImpl(logger,
                new SelectionRuleImpl(logger, Collections.emptyMap(), Collections.emptyMap()),
                new SignatureRuleImpl(logger, renames, null, null, null, null, null, Collections.emptyMap()));
        ClassActionImpl action = new ClassActionImpl(ctx);

        ByteBuffer input = ByteBuffer.wrap(bytes);
        ByteData inputData = new ByteDataImpl(name, input, FileUtils.DEFAULT_CHARSET);
        final ByteData outputData = action.apply(inputData);
        return outputData.buffer().array();
    }

}
