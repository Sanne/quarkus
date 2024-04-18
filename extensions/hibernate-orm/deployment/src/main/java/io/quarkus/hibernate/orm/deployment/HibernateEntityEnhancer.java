package io.quarkus.hibernate.orm.deployment;

import java.util.function.BiFunction;

import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import io.quarkus.deployment.QuarkusClassVisitor;
import io.quarkus.deployment.QuarkusClassWriter;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.hibernate.orm.deployment.integration.QuarkusEnhancementContext;
import net.bytebuddy.ClassFileVersion;

/**
 * Used to transform bytecode by registering to
 * io.quarkus.deployment.ProcessorContext#addByteCodeTransformer(java.util.function.Function).
 * This function adapts the Quarkus bytecode transformer API - which uses ASM - to use the Entity Enhancement API of
 * Hibernate ORM, which exposes a simple byte array.
 *
 * N.B. For enhancement the hardcoded tool of choice is the Byte Buddy based enhancer.
 * This is not configurable, and we enforce the ORM environment to use the "noop" enhancer as we require all
 * entities to be enhanced at build time.
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public final class HibernateEntityEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    private static final BytecodeProvider PROVIDER = new org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl(
            ClassFileVersion.JAVA_V11);

    private final Enhancer enhancer = PROVIDER.getEnhancer(new QuarkusEnhancementContext());

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new HibernateEnhancingClassVisitor(className, outputClassVisitor, enhancer);
    }

    private static class HibernateEnhancingClassVisitor extends QuarkusClassVisitor {

        private final String className;
        private final ClassVisitor outputClassVisitor;
        private final Enhancer enhancer;

        public HibernateEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor,
                Enhancer enhancer) {
            //Careful: the ASM API version needs to match the ASM version of Gizmo, not the one from Byte Buddy.
            //Most often these match - but occasionally they will diverge which is acceptable as Byte Buddy is shading ASM.
            super(Gizmo.ASM_API_VERSION, new QuarkusClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS));
            this.className = className;
            this.outputClassVisitor = outputClassVisitor;
            this.enhancer = enhancer;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            final ClassWriter writer = (ClassWriter) this.cv; //safe cast: cv is the ClassWriter instance we passed to the super constructor
            //We need to convert the nice Visitor chain into a plain byte array to adapt to the Hibernate ORM
            //enhancement API:
            final byte[] inputBytes = writer.toByteArray();
            final byte[] transformedBytes = hibernateEnhancement(className, inputBytes);
            //Then re-convert the transformed bytecode to not interrupt the visitor chain:
            ClassReader cr = new ClassReader(transformedBytes);
            cr.accept(outputClassVisitor, super.getOriginalClassReaderOptions());
        }

        private byte[] hibernateEnhancement(final String className, final byte[] originalBytes) {
            final byte[] enhanced = enhancer.enhance(className, originalBytes);
            return enhanced == null ? originalBytes : enhanced;
        }

    }

    public byte[] enhance(String className, byte[] bytes) {
        return enhancer.enhance(className, bytes);
    }

}
