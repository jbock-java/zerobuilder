package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoRegularGoal.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.CollectionInfo;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.regularSteps;
import static net.zerobuilder.compiler.generate.Step.nullCheck;
import static net.zerobuilder.compiler.generate.Updater.updaterType;
import static net.zerobuilder.compiler.generate.Utilities.fieldSpec;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.presentInstances;

final class UpdaterV {

  static final Function<RegularGoalContext, List<FieldSpec>> fields
      = goal -> {
    List<FieldSpec> builder = new ArrayList<>();
    builder.addAll(presentInstances(DtoRegularGoal.fields.apply(goal)));
    for (RegularStep step : regularSteps.apply(goal)) {
      String name = step.validParameter.name;
      TypeName type = step.validParameter.type;
      builder.add(fieldSpec(type, name, PRIVATE));
    }
    return builder;
  };

  static final Function<RegularGoalContext, List<MethodSpec>> updateMethods
      = goal ->
      regularSteps.apply(goal).stream()
          .map(step -> updateMethods(goal, step))
          .map(List::stream)
          .flatMap(Function.identity())
          .collect(toList());

  private static List<MethodSpec> updateMethods(RegularGoalContext goal, RegularStep step) {
    List<MethodSpec> builder = new ArrayList<>();
    builder.add(normalUpdate(goal, step));
    builder.addAll(presentInstances(regularEmptyCollection(goal, step)));
    return builder;
  }

  private static Optional<MethodSpec> regularEmptyCollection(RegularGoalContext goal, RegularStep step) {
    Optional<CollectionInfo> maybeEmptyOption = step.collectionInfo();
    if (!maybeEmptyOption.isPresent()) {
      return Optional.empty();
    }
    CollectionInfo collectionInfo = maybeEmptyOption.get();
    return Optional.of(methodBuilder(collectionInfo.name)
        .returns(updaterType(goal))
        .addStatement("this.$N = $L",
            step.field(), collectionInfo.initializer)
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build());
  }

  private static MethodSpec normalUpdate(RegularGoalContext goal, RegularStep step) {
    String name = step.validParameter.name;
    TypeName type = step.validParameter.type;
    ParameterSpec parameter = parameterSpec(type, name);
    return methodBuilder(name)
        .returns(updaterType(goal))
        .addParameter(parameter)
        .addCode(nullCheck.apply(step))
        .addStatement("this.$N = $N", step.field(), parameter)
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private UpdaterV() {
    throw new UnsupportedOperationException("no instances");
  }
}
