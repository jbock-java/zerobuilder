package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.CollectionInfo;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static java.util.Collections.singletonList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoRegularGoalContext.isInstance;
import static net.zerobuilder.compiler.generate.DtoRegularGoalContext.regularSteps;
import static net.zerobuilder.compiler.generate.StepContext.nullCheck;
import static net.zerobuilder.compiler.generate.UpdaterContext.updaterType;
import static net.zerobuilder.compiler.generate.Utilities.fieldSpec;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.presentInstances;

final class UpdaterContextV {

  static final Function<RegularGoalContext, List<FieldSpec>> fields
      = goal -> {
    DtoBuildersContext.BuildersContext buildersContext = DtoRegularGoalContext.buildersContext.apply(goal);
    List<FieldSpec> builder = new ArrayList<>();
    builder.addAll(isInstance.test(goal)
        ? singletonList(buildersContext.field)
        : Collections.emptyList());
    for (RegularStep step : regularSteps.apply(goal)) {
      String name = step.validParameter.name;
      TypeName type = step.validParameter.type;
      builder.add(fieldSpec(type, name, PRIVATE));
    }
    return builder;
  };

  static final Function<RegularGoalContext, List<MethodSpec>> updateMethods
      = goal -> {
    List<MethodSpec> builder = new ArrayList<>();
    for (RegularStep step : regularSteps.apply(goal)) {
      builder.addAll(updateMethods(goal, step));
    }
    return builder;
  };

  private static List<MethodSpec> updateMethods(RegularGoalContext goal, RegularStep step) {
    List<MethodSpec> builder = new ArrayList<>();
    builder.add(normalUpdate(goal, step));
    builder.addAll(presentInstances(regularEmptyCollection(goal, step)));
    return builder;
  }

  private static Optional<MethodSpec> regularEmptyCollection(RegularGoalContext goal, RegularStep step) {
    Optional<CollectionInfo> maybeEmptyOption = step.emptyOption();
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

  private UpdaterContextV() {
    throw new UnsupportedOperationException("no instances");
  }
}
