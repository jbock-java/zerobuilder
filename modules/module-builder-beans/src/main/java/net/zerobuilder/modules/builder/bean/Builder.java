package net.zerobuilder.modules.builder.bean;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AccessorPair;
import net.zerobuilder.compiler.generate.DtoBeanParameter.LoneGetter;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoBeanParameter.beanParameterCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.nullCheck;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.modules.builder.bean.BeanStep.nextType;

final class Builder {

  private static final ClassName ITERABLE = ClassName.get(Iterable.class);

  static final Function<BeanGoalDescription, List<FieldSpec>> fields =
      description -> singletonList(description.beanField);

  static final Function<BeanGoalDescription, List<MethodSpec>> steps =
      description -> IntStream.range(0, description.parameters.size())
          .mapToObj(i -> stepToMethods(i, description)
              .apply(description.parameters.get(i)))
          .collect(toList());

  private static Function<AbstractBeanParameter, MethodSpec> stepToMethods(
      int i, BeanGoalDescription description) {
    return beanParameterCases(
        accessorPair -> regularStep(accessorPair, i, description),
        loneGetter -> iterateCollection(loneGetter, i, description));
  }

  private static CodeBlock normalReturn(BeanGoalDescription description) {
    ParameterSpec varBean = parameterSpec(description.details.goalType,
        '_' + downcase(description.details.goalType.simpleName()));
    CodeBlock.Builder builder = CodeBlock.builder();
    builder.addStatement("$T $N = this.$N", varBean.type, varBean, description.beanField);
    return builder.addStatement("return $N", varBean).build();
  }

  private static MethodSpec iterateCollection(LoneGetter step, int i, BeanGoalDescription description) {
    String name = step.name();
    ParameterizedTypeName it = ParameterizedTypeName.get(ITERABLE,
        subtypeOf(step.iterationType()));
    ParameterSpec parameter = parameterSpec(it, name);
    ParameterSpec iterationVar = step.iterationVar(parameter);
    return methodBuilder(name)
        .addAnnotation(Override.class)
        .returns(nextType(i, description))
        .addExceptions(step.getterThrownTypes)
        .addParameter(parameter)
        .addCode(nullCheck(parameter))
        .beginControlFlow("for ($T $N : $N)",
            iterationVar.type, iterationVar, parameter)
        .addStatement("this.$N.$L().add($N)", description.beanField,
            step.getter, iterationVar)
        .endControlFlow()
        .addCode(i == description.parameters.size() - 1 ?
            normalReturn(description) :
            statement("return this"))
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec regularStep(AccessorPair step, int i, BeanGoalDescription description) {
    ParameterSpec parameter = parameterSpec(step.type, step.name());
    return methodBuilder(step.name())
        .addAnnotation(Override.class)
        .addExceptions(step.setterThrownTypes)
        .addParameter(parameter)
        .addModifiers(PUBLIC)
        .returns(nextType(i, description))
        .addCode(Step.nullCheck.apply(step))
        .addStatement("this.$N.$L($N)", description.beanField, step.setterName(), parameter)
        .addCode(i == description.parameters.size() - 1 ?
            normalReturn(description) :
            statement("return this"))
        .build();
  }

  private Builder() {
    throw new UnsupportedOperationException("no instances");
  }
}
