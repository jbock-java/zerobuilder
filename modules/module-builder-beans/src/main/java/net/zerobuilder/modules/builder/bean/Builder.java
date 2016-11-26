package net.zerobuilder.modules.builder.bean;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AccessorPair;
import net.zerobuilder.compiler.generate.DtoBeanParameter.LoneGetter;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeName.BOOLEAN;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoBeanParameter.beanParameterCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.ClassNames.ITERABLE;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.nullCheck;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.modules.builder.bean.BeanStep.nextType;

final class Builder {

  static final Function<BeanGoalContext, List<FieldSpec>> fields =
      goal -> goal.mayReuse() ?
          asList(
              fieldSpec(BOOLEAN, "_currently_in_use", PRIVATE),
              goal.bean()) :
          singletonList(goal.bean());

  static final Function<BeanGoalContext, List<MethodSpec>> steps =
      goal -> IntStream.range(0, goal.description().parameters().size())
          .mapToObj(i -> stepToMethods(i, goal)
              .apply(goal.description().parameters().get(i)))
          .collect(toList());

  private static Function<AbstractBeanParameter, MethodSpec> stepToMethods(
      int i, BeanGoalContext goal) {
    return beanParameterCases(
        accessorPair -> regularStep(accessorPair, i, goal),
        loneGetter -> iterateCollection(loneGetter, i, goal));
  }

  private static CodeBlock normalReturn(BeanGoalContext goal) {
    ParameterSpec varBean = parameterSpec(goal.type(),
        '_' + downcase(goal.details.goalType.simpleName()));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (goal.mayReuse()) {
      builder.addStatement("this._currently_in_use = false");
    }
    builder.addStatement("$T $N = this.$N", varBean.type, varBean, goal.bean());
    if (goal.mayReuse()) {
      builder.addStatement("this.$N = null", goal.bean());
    }
    return builder.addStatement("return $N", varBean).build();
  }

  private static MethodSpec iterateCollection(LoneGetter step, int i, BeanGoalContext goal) {
    String name = step.name();
    ParameterizedTypeName it = ParameterizedTypeName.get(ITERABLE,
        subtypeOf(step.iterationType()));
    ParameterSpec parameter = parameterSpec(it, name);
    ParameterSpec iterationVar = step.iterationVar(parameter);
    return methodBuilder(name)
        .addAnnotation(Override.class)
        .returns(nextType(i, goal))
        .addExceptions(step.getterThrownTypes)
        .addParameter(parameter)
        .addCode(nullCheck(parameter))
        .beginControlFlow("for ($T $N : $N)",
            iterationVar.type, iterationVar, parameter)
        .addStatement("this.$N.$L().add($N)", goal.bean(),
            step.getter, iterationVar)
        .endControlFlow()
        .addCode(i == goal.description().parameters().size() - 1 ?
            normalReturn(goal) :
            statement("return this"))
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec regularStep(AccessorPair step, int i, BeanGoalContext goal) {
    ParameterSpec parameter = parameterSpec(step.type, step.name());
    return methodBuilder(step.name())
        .addAnnotation(Override.class)
        .addExceptions(step.setterThrownTypes)
        .addParameter(parameter)
        .addModifiers(PUBLIC)
        .returns(nextType(i, goal))
        .addCode(Step.nullCheck.apply(step))
        .addStatement("this.$N.$L($N)", goal.bean(), step.setterName(), parameter)
        .addCode(i == goal.description().parameters().size() - 1 ?
            normalReturn(goal) :
            statement("return this"))
        .build();
  }

  private Builder() {
    throw new UnsupportedOperationException("no instances");
  }
}
