package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoParameter.AbstractParameter;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.presentInstances;
import static com.google.common.collect.ImmutableList.of;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.DtoParameter.parameterName;
import static net.zerobuilder.compiler.generate.DtoStep.abstractParameter;
import static net.zerobuilder.compiler.generate.DtoStep.declaredExceptions;
import static net.zerobuilder.compiler.generate.DtoStep.emptyOption;

final class StepContextV {

  static final Function<AbstractStep, TypeSpec> regularStepInterface
      = new Function<AbstractStep, TypeSpec>() {
    @Override
    public TypeSpec apply(AbstractStep step) {
      return interfaceBuilder(step.thisType)
          .addMethod(regularStepMethod(step))
          .addMethods(presentInstances(of(emptyCollection(step))))
          .addModifiers(PUBLIC)
          .build();
    }
  };

  private static MethodSpec regularStepMethod(AbstractStep step) {
    AbstractParameter parameter = abstractParameter.apply(step);
    String name = parameterName.apply(parameter);
    TypeName type = parameter.type;
    return methodBuilder(name)
        .returns(step.nextType)
        .addParameter(parameterSpec(type, name))
        .addExceptions(declaredExceptions.apply(step))
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  private static Optional<MethodSpec> emptyCollection(AbstractStep step) {
    Optional<DtoStep.EmptyOption> maybeEmptyOption = emptyOption.apply(step);
    if (!maybeEmptyOption.isPresent()) {
      return absent();
    }
    DtoStep.EmptyOption emptyOption = maybeEmptyOption.get();
    return Optional.of(methodBuilder(emptyOption.name)
        .returns(step.nextType)
        .addExceptions(declaredExceptions.apply(step))
        .addModifiers(PUBLIC, ABSTRACT)
        .build());
  }

  private StepContextV() {
    throw new UnsupportedOperationException("no instances");
  }
}
