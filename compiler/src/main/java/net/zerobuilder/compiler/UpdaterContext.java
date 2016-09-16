package net.zerobuilder.compiler;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.GoalContext.GoalCases;
import net.zerobuilder.compiler.GoalContext.GoalFunction;
import net.zerobuilder.compiler.GoalContextFactory.GoalKind;
import net.zerobuilder.compiler.ParameterContext.RegularParameterContext;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.GoalContext.always;
import static net.zerobuilder.compiler.GoalContextFactory.GoalKind.INSTANCE_METHOD;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.upcase;

final class UpdaterContext {

  static final GoalCases<ClassName> typeName = always(new GoalFunction<ClassName>() {
    @Override
    public ClassName apply(GoalContext goal, TypeName goalType, ImmutableList<? extends ParameterContext> parameters) {
      return goal.config.generatedType.nestedClass(upcase(goal.goalName + "Updater"));
    }
  });

  private static final GoalCases<ImmutableList<FieldSpec>> fields = new GoalCases<ImmutableList<FieldSpec>>() {
    @Override
    ImmutableList<FieldSpec> regularGoal(GoalContext goal, TypeName goalType, GoalKind kind, ImmutableList<RegularParameterContext> parameters) {
      ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
      if (kind == INSTANCE_METHOD) {
        ClassName receiverType = goal.config.annotatedType;
        builder.add(FieldSpec.builder(receiverType, '_' + downcase(receiverType.simpleName()), PRIVATE).build());
      }
      for (RegularParameterContext parameter : parameters) {
        String name = parameter.parameter.name;
        TypeName type = parameter.parameter.type;
        builder.add(FieldSpec.builder(type, name, PRIVATE).build());
      }
      return builder.build();
    }
    @Override
    ImmutableList<FieldSpec> fieldGoal(GoalContext goal, ClassName goalType, ImmutableList<ParameterContext.BeansParameterContext> parameters) {
      FieldSpec field = FieldSpec.builder(goalType, downcase(goalType.simpleName()))
          .build();
      return ImmutableList.of(field);
    }
  };


  private static final GoalCases<ImmutableList<MethodSpec>> updaterMethods = new GoalCases<ImmutableList<MethodSpec>>() {
    @Override
    ImmutableList<MethodSpec> regularGoal(GoalContext goal, TypeName goalType, GoalKind kind, ImmutableList<ParameterContext.RegularParameterContext> parameters) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (ParameterContext.RegularParameterContext parameter : parameters) {
        String name = parameter.parameter.name;
        TypeName type = parameter.parameter.type;
        builder.add(methodBuilder(name)
            .returns(goal.accept(typeName))
            .addParameter(ParameterSpec.builder(type, name).build())
            .addStatement("this.$N = $N", name, name)
            .addStatement("return this")
            .addModifiers(goal.maybeAddPublic())
            .build());
      }
      return builder.build();
    }
    @Override
    ImmutableList<MethodSpec> fieldGoal(GoalContext goal, ClassName goalType, ImmutableList<ParameterContext.BeansParameterContext> parameters) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (ParameterContext.BeansParameterContext parameter : parameters) {
        String name = parameter.parameter.name;
        TypeName type = parameter.parameter.type;
        Optional<ClassName> setterlessCollection = parameter.parameter.setterlessCollection;
        if (setterlessCollection.isPresent()) {
          String iterationVarName = downcase(setterlessCollection.get().simpleName());
          builder.add(methodBuilder(name)
              .returns(goal.accept(typeName))
              .addParameter(ParameterSpec.builder(type, name).build())
              .addStatement("this.$N.$N().clear()", downcase(goalType.simpleName()),
                  parameter.parameter.projectionMethodName)
              .beginControlFlow("for ($T $N : $N)", setterlessCollection.get(), iterationVarName, name)
              .addStatement("this.$N.$N().add($N)", downcase(goalType.simpleName()),
                  parameter.parameter.projectionMethodName, iterationVarName)
              .endControlFlow()
              .addStatement("return this")
              .addModifiers(goal.maybeAddPublic())
              .build());
        } else {
          builder.add(methodBuilder(name)
              .returns(goal.accept(typeName))
              .addParameter(ParameterSpec.builder(type, name).build())
              .addStatement("this.$N.set$L($N)", downcase(goalType.simpleName()), upcase(name), name)
              .addStatement("return this")
              .addModifiers(goal.maybeAddPublic())
              .build());
        }
      }
      return builder.build();
    }
  };

  private static final GoalCases<MethodSpec> buildMethod = always(new GoalFunction<MethodSpec>() {
    @Override
    public MethodSpec apply(GoalContext goal, TypeName goalType, ImmutableList<? extends ParameterContext> parameters) {
      return methodBuilder("build")
          .addModifiers(goal.maybeAddPublic())
          .returns(goalType)
          .addCode(goal.goalCall)
          .addExceptions(goal.thrownTypes)
          .build();
    }
  });

  static TypeSpec buildUpdaterImpl(GoalContext goal) {
    return classBuilder(goal.accept(typeName))
        .addFields(goal.accept(fields))
        .addMethods(goal.accept(updaterMethods))
        .addMethod(goal.accept(buildMethod))
        .addModifiers(FINAL, STATIC)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build();
  }

  private UpdaterContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
