package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.UberGoalContext.GoalKind;

import static com.google.common.collect.Iterables.getLast;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.UberGoalContext.GoalKind.INSTANCE_METHOD;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.upcase;

final class StepsContext {

  private final GoalContext goal;

  StepsContext(GoalContext goal) {
    this.goal = goal;
  }

  private ImmutableList<FieldSpec> fields() {
    return goal.accept(new GoalContext.Cases<ImmutableList<FieldSpec>>() {
      @Override
      ImmutableList<FieldSpec> regularGoal(GoalKind kind) {
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
      ImmutableList<FieldSpec> fieldGoal(ClassName goalType) {
        FieldSpec field = FieldSpec.builder(goalType, downcase(goalType.simpleName()))
            .build();
        return ImmutableList.of(field);
      }
    });
  }

  private ImmutableList<MethodSpec> stepsButLast() {
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    for (final ParameterContext parameter : goal.goalParameters.subList(0, goal.goalParameters.size() - 1)) {
      final String name = parameter.validParameter.name;
      CodeBlock update = goal.accept(new GoalContext.Cases<CodeBlock>() {
        @Override
        CodeBlock regularGoal(GoalKind kind) {
          return CodeBlock.of("this.$N = $N;\n", name, name);
        }
        @Override
        CodeBlock fieldGoal(ClassName goalType) {
          return CodeBlock.of("this.$N.set$L($N);\n", downcase(goalType.simpleName()), upcase(name), name);
        }
      });
      builder.add(methodBuilder(parameter.validParameter.name)
          .addAnnotation(Override.class)
          .returns(parameter.returnType)
          .addParameter(parameter.parameter())
          .addCode(update)
          .addStatement("return this")
          .addModifiers(PUBLIC)
          .build());
    }
    return builder.build();
  }

  private MethodSpec lastStep() {
    ParameterContext parameter = getLast(goal.goalParameters);
    return methodBuilder(parameter.validParameter.name)
        .addAnnotation(Override.class)
        .addParameter(parameter.parameter())
        .addExceptions(goal.thrownTypes)
        .addModifiers(PUBLIC)
        .returns(goal.goalType)
        .addCode(goal.goalCall).build();
  }

  TypeSpec buildStepsImpl() {
    return classBuilder(goal.stepsImplTypeName())
        .addSuperinterfaces(goal.stepInterfaceNames())
        .addFields(fields())
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .addMethods(stepsButLast())
        .addMethod(lastStep())
        .addModifiers(FINAL, STATIC)
        .build();
  }

}
