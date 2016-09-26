package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.ConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.MethodGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContextCases;
import net.zerobuilder.compiler.generate.DtoStep.RegularStep;

import static com.google.common.base.Optional.presentInstances;
import static com.google.common.collect.ImmutableList.of;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.StepContext.nullCheck;
import static net.zerobuilder.compiler.generate.UpdaterContext.typeName;

final class UpdaterContextV {

  static final Function<RegularGoalContext, ImmutableList<FieldSpec>> fields
      = new Function<RegularGoalContext, ImmutableList<FieldSpec>>() {
    @Override
    public ImmutableList<FieldSpec> apply(RegularGoalContext goal) {
      ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
      builder.addAll(presentInstances(of(goal.acceptRegular(instanceField))));
      for (RegularStep step : goal.steps) {
        String name = step.validParameter.name;
        TypeName type = step.validParameter.type;
        builder.add(FieldSpec.builder(type, name, PRIVATE).build());
      }
      return builder.build();
    }
  };

  static final RegularGoalContextCases<Optional<FieldSpec>> instanceField
      = new RegularGoalContextCases<Optional<FieldSpec>>() {
    @Override
    public Optional<FieldSpec> constructorGoal(ConstructorGoalContext goal) {
      return Optional.absent();
    }
    @Override
    public Optional<FieldSpec> methodGoal(MethodGoalContext goal) {
      return goal.goal.instance
          ? Optional.of(goal.builders.field)
          : Optional.<FieldSpec>absent();
    }
  };

  static final Function<RegularGoalContext, ImmutableList<MethodSpec>> updateMethods
      = new Function<RegularGoalContext, ImmutableList<MethodSpec>>() {
    @Override
    public ImmutableList<MethodSpec> apply(RegularGoalContext goal) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (RegularStep step : goal.steps) {
        String name = step.validParameter.name;
        ParameterSpec parameter = step.parameter();
        builder.add(methodBuilder(name)
            .returns(goal.accept(typeName))
            .addParameter(parameter)
            .addCode(step.accept(nullCheck))
            .addStatement("this.$N = $N", step.field, parameter)
            .addStatement("return this")
            .addModifiers(PUBLIC)
            .build());
      }
      return builder.build();
    }
  };

  private UpdaterContextV() {
    throw new UnsupportedOperationException("no instances");
  }
}
