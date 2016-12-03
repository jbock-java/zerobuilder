package net.zerobuilder.modules.updater.bean;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AccessorPair;
import net.zerobuilder.compiler.generate.DtoBeanParameter.LoneGetter;
import net.zerobuilder.compiler.generate.ZeroUtil;

import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoBeanParameter.beanParameterCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.ClassNames.ITERABLE;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.modules.updater.bean.BeanUpdater.implType;

final class Updater {

  final static Function<BeanGoalContext, List<FieldSpec>> fields =
      goal -> singletonList(goal.bean());

  final static Function<BeanGoalContext, List<MethodSpec>> stepMethods = goal ->
      goal.description().parameters().stream()
          .map(stepToMethods(goal))
          .collect(toList());

  private static Function<AbstractBeanParameter, MethodSpec> stepToMethods(BeanGoalContext goal) {
    return beanParameterCases(
        accessorPair -> normalUpdate(accessorPair, goal),
        loneGetter -> iterateCollection(goal, loneGetter));
  }

  private static MethodSpec normalUpdate(AccessorPair step, BeanGoalContext goal) {
    String name = step.name();
    ParameterSpec parameter = parameterSpec(step.type, step.name());
    return methodBuilder(name)
        .returns(implType(goal))
        .addExceptions(step.setterThrownTypes)
        .addParameter(parameter)
        .addStatement("this.$N.$L($N)",
            goal.bean(), step.setterName(), parameter)
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec iterateCollection(BeanGoalContext goal, LoneGetter step) {
    ParameterizedTypeName iterable = ParameterizedTypeName.get(ITERABLE,
        subtypeOf(step.iterationType()));
    String name = step.name();
    ParameterSpec parameter = parameterSpec(iterable, name);
    ParameterSpec iterationVar = step.iterationVar(parameter);
    return methodBuilder(name)
        .returns(implType(goal))
        .addParameter(parameter)
        .addExceptions(step.getterThrownTypes)
        .addCode(ZeroUtil.nullCheck(name, name))
        .addCode(clearCollection(goal, step))
        .beginControlFlow("for ($T $N : $N)", iterationVar.type, iterationVar, name)
        .addStatement("this.$N.$N().add($N)",
            goal.bean(), step.getter, iterationVar)
        .endControlFlow()
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private static CodeBlock clearCollection(BeanGoalContext goal, LoneGetter step) {
    return CodeBlock.builder().addStatement("this.$N.$N().clear()",
        goal.bean(), step.getter).build();
  }

  private Updater() {
    throw new UnsupportedOperationException("no instances");
  }
}
