package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoStep.EmptyOption;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.presentInstances;
import static com.google.common.collect.ImmutableList.of;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.fieldSpec;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.DtoRegularGoalContext.isInstance;
import static net.zerobuilder.compiler.generate.StepContext.nullCheck;
import static net.zerobuilder.compiler.generate.UpdaterContext.updaterType;

final class UpdaterContextV {

  static final Function<RegularGoalContext, ImmutableList<FieldSpec>> fields
      = new Function<RegularGoalContext, ImmutableList<FieldSpec>>() {
    @Override
    public ImmutableList<FieldSpec> apply(RegularGoalContext goal) {
      ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
      builder.addAll(goal.acceptRegular(isInstance)
          ? ImmutableList.of(goal.builders.field)
          : ImmutableList.<FieldSpec>of());
      for (RegularStep step : goal.steps) {
        String name = step.validParameter.name;
        TypeName type = step.validParameter.type;
        builder.add(fieldSpec(type, name, PRIVATE));
      }
      return builder.build();
    }
  };

  static final Function<RegularGoalContext, ImmutableList<MethodSpec>> updateMethods
      = new Function<RegularGoalContext, ImmutableList<MethodSpec>>() {
    @Override
    public ImmutableList<MethodSpec> apply(RegularGoalContext goal) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (RegularStep step : goal.steps) {
        builder.addAll(updateMethods(goal, step));
      }
      return builder.build();
    }
  };

  private static ImmutableList<MethodSpec> updateMethods(RegularGoalContext goal, RegularStep step) {
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    builder.add(normalUpdate(goal, step));
    builder.addAll(presentInstances(of(regularEmptyCollection(goal, step))));
    return builder.build();
  }

  private static Optional<MethodSpec> regularEmptyCollection(RegularGoalContext goal, RegularStep step) {
    Optional<EmptyOption> maybeEmptyOption = step.emptyOption();
    if (!maybeEmptyOption.isPresent()) {
      return absent();
    }
    EmptyOption emptyOption = maybeEmptyOption.get();
    return Optional.of(methodBuilder(emptyOption.name)
        .returns(updaterType.apply(goal))
        .addStatement("this.$N = $L",
            step.field(), emptyOption.initializer)
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build());
  }

  private static MethodSpec normalUpdate(RegularGoalContext goal, RegularStep step) {
    String name = step.validParameter.name;
    TypeName type = step.validParameter.type;
    ParameterSpec parameter = parameterSpec(type, name);
    return methodBuilder(name)
        .returns(updaterType.apply(goal))
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
