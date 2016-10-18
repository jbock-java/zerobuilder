package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.AccessorPairStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.LoneGetterStep;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;

import javax.lang.model.element.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static java.util.Arrays.asList;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.NullPolicy.ALLOW;
import static net.zerobuilder.compiler.generate.DtoBeanStep.beanStepCases;
import static net.zerobuilder.compiler.generate.DtoBeanStep.getterThrownTypes;
import static net.zerobuilder.compiler.generate.DtoBeanStep.setterThrownTypes;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.Utilities.flatList;
import static net.zerobuilder.compiler.generate.Utilities.joinCodeBlocks;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.statement;

final class GeneratorBB {

  static final Function<BeanGoalContext, BuilderMethod> goalToBuilderB
      = goal -> {
    ClassName builderType = goal.implType();
    String name = goal.goal.details.name;
    String builder = downcase(builderType.simpleName());
    ClassName type = goal.goal.details.goalType;
    FieldSpec cache = goal.context.cache.get();
    MethodSpec method = methodBuilder(name + "Builder")
        .returns(goal.contractType().nestedClass(goal.steps().get(0).thisType))
        .addModifiers(goal.goal.details.goalOptions.access.modifiers(STATIC))
        .addExceptions(goal.context.lifecycle == REUSE_INSTANCES
            ? Collections.emptyList()
            : goal.goal.thrownTypes)
        .addCode(goal.context.lifecycle == REUSE_INSTANCES
            ? statement("$T $N = $N.get().$N", builderType, builder, cache, goal.cacheField())
            : statement("$T $N = new $T()", builderType, builder, builderType))
        .addCode(goal.context.lifecycle == REUSE_INSTANCES
            ? statement("$N.$N = new $T()", builder, goal.bean(), type)
            : emptyCodeBlock)
        .addStatement("return $N", builder)
        .build();
    return new BuilderMethod(name, method);
  };

  private GeneratorBB() {
    throw new UnsupportedOperationException("no instances");
  }
}
