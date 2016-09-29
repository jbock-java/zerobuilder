package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.analyse.DtoParameter.AbstractParameter;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.presentInstances;
import static com.google.common.collect.ImmutableList.of;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.analyse.DtoParameter.parameterName;
import static net.zerobuilder.compiler.generate.DtoStep.declaredExceptions;
import static net.zerobuilder.compiler.generate.DtoStep.validParameter;

final class StepContextV {

  static final Function<AbstractStep, TypeSpec> regularStepInterface
      = new Function<AbstractStep, TypeSpec>() {
    @Override
    public TypeSpec apply(AbstractStep step) {
      return interfaceBuilder(step.thisType)
          .addMethods(regularMethods(step))
          .addModifiers(PUBLIC)
          .build();
    }
  };

  private static ImmutableList<MethodSpec> regularMethods(AbstractStep step) {
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    builder.add(regularStepMethod(step));
    builder.addAll(presentInstances(of(emptyCollection(step))));
    return builder.build();
  }

  private static Optional<MethodSpec> emptyCollection(AbstractStep step) {
    Optional<DtoStep.EmptyOption> maybeEmptyOption = step.accept(DtoStep.emptyOption);
    if (!maybeEmptyOption.isPresent()) {
      return absent();
    }
    DtoStep.EmptyOption emptyOption = maybeEmptyOption.get();
    return Optional.of(methodBuilder(emptyOption.name)
        .returns(step.nextType)
        .addExceptions(step.accept(declaredExceptions))
        .addModifiers(PUBLIC, ABSTRACT)
        .build());
  }

  private static MethodSpec regularStepMethod(AbstractStep step) {
    AbstractParameter parameter = step.accept(validParameter);
    String name = parameter.acceptParameter(parameterName);
    TypeName type = parameter.type;
    return methodBuilder(name)
        .returns(step.nextType)
        .addParameter(parameterSpec(type, name))
        .addExceptions(step.accept(declaredExceptions))
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  private StepContextV() {
    throw new UnsupportedOperationException("no instances");
  }
}
