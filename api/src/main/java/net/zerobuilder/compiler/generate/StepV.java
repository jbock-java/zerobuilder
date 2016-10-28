package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoParameter.AbstractParameter;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;
import net.zerobuilder.compiler.generate.DtoSimpleGoal.SimpleGoal;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoParameter.parameterName;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.simpleGoalCases;
import static net.zerobuilder.compiler.generate.DtoStep.AbstractStep.nextType;
import static net.zerobuilder.compiler.generate.Utilities.concat;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.presentInstances;

final class StepV {

  static Function<AbstractRegularStep, TypeSpec> regularStepInterface(SimpleGoal goal) {
    return step -> interfaceBuilder(step.thisType)
        .addMethod(regularStepMethod(step, goal))
        .addMethods(presentInstances(emptyCollection(step)))
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec regularStepMethod(AbstractRegularStep step, SimpleGoal goal) {
    AbstractParameter parameter = step.abstractParameter();
    String name = parameterName.apply(parameter);
    TypeName type = parameter.type;
    List<TypeName> thrownTypes = step.declaredExceptions();
    if (!step.nextStep.isPresent()) {
      thrownTypes = concat(StepV.thrownTypes.apply(goal), thrownTypes);
    }
    return methodBuilder(name)
        .returns(nextType(step))
        .addParameter(parameterSpec(type, name))
        .addExceptions(thrownTypes)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  private static final Function<SimpleGoal, List<TypeName>> thrownTypes =
      simpleGoalCases(
          regular -> regular.thrownTypes,
          bean -> Collections.emptyList());

  private static Optional<MethodSpec> emptyCollection(AbstractRegularStep step) {
    Optional<DtoStep.CollectionInfo> maybeEmptyOption = step.collectionInfo();
    if (!maybeEmptyOption.isPresent()) {
      return Optional.empty();
    }
    DtoStep.CollectionInfo collectionInfo = maybeEmptyOption.get();
    return Optional.of(methodBuilder(collectionInfo.name)
        .returns(nextType(step))
        .addModifiers(PUBLIC, ABSTRACT)
        .build());
  }

  private StepV() {
    throw new UnsupportedOperationException("no instances");
  }
}
