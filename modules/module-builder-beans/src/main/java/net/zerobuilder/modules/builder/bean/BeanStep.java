package net.zerobuilder.modules.builder.bean;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.AccessorPairStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.LoneGetterStep;
import net.zerobuilder.compiler.generate.DtoParameter;

import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoBeanStep.beanStepCases;
import static net.zerobuilder.compiler.generate.DtoParameter.parameterName;
import static net.zerobuilder.compiler.generate.ZeroUtil.ClassNames.ITERABLE;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

final class BeanStep {

  static final Function<AbstractBeanStep, TypeSpec> beanStepInterface
      = beanStepCases(
      accessorPair -> interfaceBuilder(accessorPair.thisType)
          .addMethod(regularMethod(accessorPair))
          .addModifiers(PUBLIC)
          .build(),
      loneGetter -> interfaceBuilder(loneGetter.thisType)
          .addMethod(iterateCollection(loneGetter))
          .addModifiers(PUBLIC)
          .build());

  private static MethodSpec regularMethod(AccessorPairStep step) {
    DtoParameter.AbstractParameter parameter = step.abstractParameter();
    String name = parameterName.apply(parameter);
    TypeName type = parameter.type;
    return methodBuilder(name)
        .returns(nextType(step))
        .addParameter(parameterSpec(type, name))
        .addExceptions(step.accessorPair.setterThrownTypes)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  private static MethodSpec iterateCollection(LoneGetterStep step) {
    String name = step.loneGetter.name();
    TypeName type = ParameterizedTypeName.get(ITERABLE,
        subtypeOf(step.loneGetter.iterationType()));
    return methodBuilder(name)
        .addParameter(parameterSpec(type, name))
        .addExceptions(step.loneGetter.getterThrownTypes)
        .returns(nextType(step))
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  static TypeName nextType(AbstractBeanStep step) {
    if (step.nextStep.isPresent()) {
      return step.context.generatedType
          .nestedClass(upcase(step.goalDetails.name() + "Builder"))
          .nestedClass(step.nextStep.get().thisType);
    }
    return step.goalDetails.type();
  }


  private BeanStep() {
    throw new UnsupportedOperationException("no instances");
  }
}
