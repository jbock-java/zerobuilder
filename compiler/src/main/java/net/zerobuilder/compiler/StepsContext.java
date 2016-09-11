package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.GoalContext.GoalCases;
import net.zerobuilder.compiler.GoalContextFactory.GoalKind;

import static com.google.common.collect.Iterables.getLast;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.GoalContext.always;
import static net.zerobuilder.compiler.GoalContext.stepInterfaceNames;
import static net.zerobuilder.compiler.GoalContext.stepsImplTypeName;
import static net.zerobuilder.compiler.GoalContextFactory.GoalKind.INSTANCE_METHOD;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.upcase;

final class StepsContext {

  private static final GoalCases<ImmutableList<FieldSpec>> fields
      = new GoalCases<ImmutableList<FieldSpec>>() {
    @Override
    ImmutableList<FieldSpec> regularGoal(GoalContext goal, GoalKind kind) {
      ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
      if (kind == INSTANCE_METHOD) {
        ClassName receiverType = goal.config.annotatedType;
        builder.add(FieldSpec.builder(receiverType, '_' + downcase(receiverType.simpleName()), PRIVATE).build());
      }
      for (ParameterContext parameter : goal.goalParameters.subList(0, goal.goalParameters.size() - 1)) {
        String name = parameter.validParameter.name;
        builder.add(FieldSpec.builder(parameter.validParameter.type, name, PRIVATE).build());
      }
      return builder.build();
    }
    @Override
    ImmutableList<FieldSpec> fieldGoal(GoalContext goal, ClassName goalType) {
      FieldSpec field = FieldSpec.builder(goalType, downcase(goalType.simpleName()))
          .build();
      return ImmutableList.of(field);
    }
  };

  private static final GoalCases<ImmutableList<MethodSpec>> stepsButLast
      = new GoalCases<ImmutableList<MethodSpec>>() {
    @Override
    ImmutableList<MethodSpec> regularGoal(GoalContext goal, GoalKind kind) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (ParameterContext parameter : goal.goalParameters.subList(0, goal.goalParameters.size() - 1)) {
        String name = parameter.validParameter.name;
        TypeName type = parameter.validParameter.type;
        builder.add(methodBuilder(name)
            .addAnnotation(Override.class)
            .returns(parameter.returnType)
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
      for (ParameterContext parameter : goal.goalParameters.subList(0, goal.goalParameters.size() - 1)) {
        String name = parameter.validParameter.name;
        TypeName type = parameter.validParameter.type;
        builder.add(methodBuilder(name)
            .addAnnotation(Override.class)
            .returns(parameter.returnType)
            .addParameter(ParameterSpec.builder(type, name).build())
            .addStatement("this.$N.set$L($N)", downcase(goalType.simpleName()), upcase(name), name)
            .addStatement("return this")
            .addModifiers(PUBLIC)
            .build());
      }
      return builder.build();
    }
  };

  private static final GoalCases<MethodSpec> lastStep = new GoalCases<MethodSpec>() {
    @Override
    MethodSpec regularGoal(GoalContext goal, GoalKind kind) {
      ParameterContext parameter = getLast(goal.goalParameters);
      String name = parameter.validParameter.name;
      TypeName type = parameter.validParameter.type;
      return methodBuilder(parameter.validParameter.name)
          .addAnnotation(Override.class)
          .addParameter(ParameterSpec.builder(type, name).build())
          .addExceptions(goal.thrownTypes)
          .addModifiers(PUBLIC)
          .returns(goal.goalType)
          .addCode(goal.goalCall).build();
    }
    @Override
    MethodSpec fieldGoal(GoalContext goal, ClassName goalType) {
      ParameterContext parameter = getLast(goal.goalParameters);
      String name = parameter.validParameter.name;
      TypeName type = parameter.validParameter.type;
      return methodBuilder(parameter.validParameter.name)
          .addAnnotation(Override.class)
          .addParameter(ParameterSpec.builder(type, name).build())
          .addExceptions(goal.thrownTypes)
          .addModifiers(PUBLIC)
          .returns(goal.goalType)
          .addStatement("this.$N.set$L($N)", downcase(goalType.simpleName()), upcase(name), name)
          .addCode(goal.goalCall).build();
    }
  };

  static TypeSpec buildStepsImpl(GoalContext goal) {
    return classBuilder(goal.accept(stepsImplTypeName))
        .addSuperinterfaces(goal.accept(stepInterfaceNames))
        .addFields(goal.accept(fields))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .addMethods(goal.accept(stepsButLast))
        .addMethod(goal.accept(lastStep))
        .addModifiers(FINAL, STATIC)
        .build();
  }

  private StepsContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
