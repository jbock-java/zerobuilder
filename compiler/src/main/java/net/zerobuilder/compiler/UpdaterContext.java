package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.GoalContext.GoalCases;
import net.zerobuilder.compiler.GoalContextFactory.GoalKind;

import static com.google.common.base.Optional.absent;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.GoalContext.always;
import static net.zerobuilder.compiler.GoalContext.contractUpdaterName;
import static net.zerobuilder.compiler.GoalContextFactory.GoalKind.INSTANCE_METHOD;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.upcase;

final class UpdaterContext {

  private static final String UPDATER_IMPL = "UpdaterImpl";

  static final GoalCases<ClassName> typeName = always(new Function<GoalContext, ClassName>() {
    @Override
    public ClassName apply(GoalContext goal) {
      return goal.generatedType.nestedClass(UPDATER_IMPL);
    }
  });

  private static final GoalCases<ImmutableList<FieldSpec>> fields = new GoalCases<ImmutableList<FieldSpec>>() {
    @Override
    ImmutableList<FieldSpec> regularGoal(GoalContext goal, GoalKind kind) {
      ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
      if (kind == INSTANCE_METHOD) {
        ClassName receiverType = goal.config.annotatedType;
        builder.add(FieldSpec.builder(receiverType, '_' + downcase(receiverType.simpleName()), PRIVATE).build());
      }
      for (ParameterContext parameter : goal.goalParameters) {
        String name = parameter.validParameter.name;
        TypeName type = parameter.validParameter.type;
        builder.add(FieldSpec.builder(type, name, PRIVATE).build());
      }
      return builder.build();
    }
    @Override
    ImmutableList<FieldSpec> fieldGoal(GoalContext goal, ClassName goalType) {
      FieldSpec field = FieldSpec.builder(goal.goalType, downcase(goalType.simpleName()))
          .build();
      return ImmutableList.of(field);
    }
  };


  private static final GoalCases<ImmutableList<MethodSpec>> updaterMethods = new GoalCases<ImmutableList<MethodSpec>>() {
    @Override
    ImmutableList<MethodSpec> regularGoal(GoalContext goal, GoalKind kind) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (ParameterContext parameter : goal.goalParameters) {
        String name = parameter.validParameter.name;
        TypeName type = parameter.validParameter.type;
        builder.add(methodBuilder(name)
            .addAnnotation(Override.class)
            .returns(goal.accept(contractUpdaterName))
            .addParameter(ParameterSpec.builder(type, name).build())
            .addStatement("this.$N = $N", name, name)
            .addStatement("return this")
            .addModifiers(PUBLIC)
            .build());
      }
      return builder.build();
    }
    @Override
    ImmutableList<MethodSpec> fieldGoal(GoalContext goal, ClassName goalType) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (ParameterContext parameter : goal.goalParameters) {
        String name = parameter.validParameter.name;
        TypeName type = parameter.validParameter.type;
        builder.add(methodBuilder(name)
            .addAnnotation(Override.class)
            .returns(goal.accept(contractUpdaterName))
            .addParameter(ParameterSpec.builder(type, name).build())
            .addStatement("this.$N.set$L($N)", downcase(goalType.simpleName()), upcase(name), name)
            .addStatement("return this")
            .addModifiers(PUBLIC)
            .build());
      }
      return builder.build();
    }
  };

  private static final GoalCases<MethodSpec> buildMethod = always(new Function<GoalContext, MethodSpec>() {
    @Override
    public MethodSpec apply(GoalContext goal) {
      return methodBuilder("build")
          .addAnnotation(Override.class)
          .addModifiers(PUBLIC)
          .returns(goal.goalType)
          .addCode(goal.goalCall)
          .addExceptions(goal.thrownTypes)
          .build();
    }
  });

  static Optional<TypeSpec> buildUpdaterImpl(GoalContext goal) {
    if (!goal.toBuilder) {
      return absent();
    }
    return Optional.of(classBuilder(goal.accept(typeName))
        .addSuperinterface(goal.accept(contractUpdaterName))
        .addFields(goal.accept(fields))
        .addMethods(goal.accept(updaterMethods))
        .addMethod(goal.accept(buildMethod))
        .addModifiers(FINAL, STATIC)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build());
  }

  private UpdaterContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
