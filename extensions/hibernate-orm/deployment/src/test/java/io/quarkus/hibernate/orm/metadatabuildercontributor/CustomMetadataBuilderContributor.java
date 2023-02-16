package io.quarkus.hibernate.orm.metadatabuildercontributor;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

import java.util.List;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

//TODO 1 rename class
//TODO 2 ensure this implementation still matches the needs of the test using it
public class CustomMetadataBuilderContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
        functionContributions.getFunctionRegistry().register(
                "addHardcodedSuffix",
                new HardcodedSuffixFunction(typeConfiguration, "_some_suffix"));
    }

    private static final class HardcodedSuffixFunction extends AbstractSqmSelfRenderingFunctionDescriptor
            implements org.hibernate.query.sqm.function.SqmFunctionDescriptor {
        private final String suffix;

        private HardcodedSuffixFunction(TypeConfiguration typeConfiguration, String suffix) {
            super(
                    "constantSuffix",
                    StandardArgumentsValidators.exactly(1),
                    StandardFunctionReturnTypeResolvers.invariant(
                            typeConfiguration.getBasicTypeRegistry().resolve(StandardBasicTypes.STRING)),
                    StandardFunctionArgumentTypeResolvers.impliedOrInvariant(typeConfiguration, STRING));
            this.suffix = suffix;
        }

        @Override
        public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments, SqlAstTranslator<?> walker) {
            sqlAppender.appendSql('(');
            walker.render(sqlAstArguments.get(0), SqlAstNodeRenderingMode.DEFAULT);
            sqlAppender.appendSql(" || '" + suffix + "')");
        }
    }
}
