package net.zerobuilder.modules.updater;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;
import net.zerobuilder.compiler.generate.DtoRegularStep.ProjectedRegularStep;
import net.zerobuilder.compiler.generate.DtoStep.CollectionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.steps;
import static net.zerobuilder.compiler.generate.Step.nullCheck;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.flatList;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.presentInstances;

final class UpdaterV {

  private final Updater updater;

  UpdaterV(Updater updater) {
    this.updater = updater;
  }

  final Function<ProjectedRegularGoalContext, List<FieldSpec>> fieldsV
      = goal -> {
    List<FieldSpec> builder = new ArrayList<>();
    for (ProjectedRegularStep step : steps.apply(goal)) {
      String name = step.regularParameter().name;
      TypeName type = step.regularParameter().type;
      builder.add(fieldSpec(type, name, PRIVATE));
    }
    return builder;
  };

  final Function<ProjectedRegularGoalContext, List<MethodSpec>> updateMethodsV
      = goal ->
      steps.apply(goal).stream()
          .map(updateMethods(goal))
          .collect(flatList());


  private Function<AbstractRegularStep, List<MethodSpec>> updateMethods(ProjectedRegularGoalContext goal) {
    return step -> Stream.concat(
        Stream.of(normalUpdate(goal, step)),
        presentInstances(emptyCollection(goal, step)).stream())
        .collect(toList());
  }

  private Optional<MethodSpec> emptyCollection(ProjectedRegularGoalContext goal, AbstractRegularStep step) {
    Optional<CollectionInfo> maybeEmptyOption = step.collectionInfo();
    if (!maybeEmptyOption.isPresent()) {
      return Optional.empty();
    }
    CollectionInfo collectionInfo = maybeEmptyOption.get();
    return Optional.of(methodBuilder(collectionInfo.name)
        .returns(updater.legacyImplType(goal))
        .addStatement("this.$N = $L",
            step.field(), collectionInfo.initializer)
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build());
  }

  private MethodSpec normalUpdate(ProjectedRegularGoalContext goal, AbstractRegularStep step) {
    String name = step.regularParameter().name;
    TypeName type = step.regularParameter().type;
    ParameterSpec parameter = parameterSpec(type, name);
    return methodBuilder(name)
        .returns(updater.legacyImplType(goal))
        .addParameter(parameter)
        .addCode(nullCheck.apply(step))
        .addStatement("this.$N = $N", step.field(), parameter)
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }
}
