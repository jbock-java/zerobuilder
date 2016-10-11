package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.AccessorPairStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.LoneGetterStep;

import java.util.Optional;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoBeanStep.beanStepCases;
import static net.zerobuilder.compiler.generate.DtoParameter.parameterName;
import static net.zerobuilder.compiler.generate.DtoStep.abstractParameter;
import static net.zerobuilder.compiler.generate.Utilities.ClassNames.ITERABLE;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.presentInstances;

final class StepB {

  static final Function<AbstractBeanStep, TypeSpec> beanStepInterface
      = beanStepCases(
      step -> interfaceBuilder(step.thisType)
          .addMethod(regularMethod(step))
          .addMethods(presentInstances(emptyCollection(step)))
          .addModifiers(PUBLIC)
          .build(),
      step -> interfaceBuilder(step.thisType)
          .addMethod(iterateCollection(step))
          .addMethod(emptyCollection(step))
          .addModifiers(PUBLIC)
          .build());

  private static MethodSpec regularMethod(AccessorPairStep step) {
    DtoParameter.AbstractParameter parameter = abstractParameter.apply(step);
    String name = parameterName.apply(parameter);
    TypeName type = parameter.type;
    return methodBuilder(name)
        .returns(step.nextType)
        .addParameter(parameterSpec(type, name))
        .addExceptions(step.accessorPair.setterThrownTypes)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  private static Optional<MethodSpec> emptyCollection(AccessorPairStep step) {
    Optional<DtoStep.CollectionInfo> maybeEmptyOption = step.emptyOption();
    if (!maybeEmptyOption.isPresent()) {
      return Optional.empty();
    }
    DtoStep.CollectionInfo collectionInfo = maybeEmptyOption.get();
    return Optional.of(methodBuilder(collectionInfo.name)
        .returns(step.nextType)
        .addExceptions(step.accessorPair.setterThrownTypes)
        .addModifiers(PUBLIC, ABSTRACT)
        .build());
  }

  private static MethodSpec emptyCollection(LoneGetterStep step) {
    return methodBuilder(step.emptyMethod)
        .returns(step.nextType)
        .addExceptions(step.loneGetter.getterThrownTypes)
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
        .returns(step.nextType)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  private StepB() {
    throw new UnsupportedOperationException("no instances");
  }
}
