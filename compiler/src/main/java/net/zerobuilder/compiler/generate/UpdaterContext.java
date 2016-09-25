package net.zerobuilder.compiler.generate;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoGoal.GoalCases;
import net.zerobuilder.compiler.generate.DtoGoal.GoalFunction;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalContext;
import net.zerobuilder.compiler.generate.StepContext.AbstractStep;
import net.zerobuilder.compiler.generate.StepContext.BeansStep;
import net.zerobuilder.compiler.generate.StepContext.RegularStep;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.iterationVarName;
import static net.zerobuilder.compiler.Utilities.nullCheck;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.Utilities.upcase;
import static net.zerobuilder.compiler.analyse.GoalContextFactory.GoalKind.INSTANCE_METHOD;
import static net.zerobuilder.compiler.generate.BuilderContext.invoke;
import static net.zerobuilder.compiler.generate.DtoGoal.always;
import static net.zerobuilder.compiler.generate.StepContext.maybeIterationNullCheck;
import static net.zerobuilder.compiler.generate.StepContext.maybeNullCheck;

final class UpdaterContext {

  static final GoalCases<ClassName> typeName = always(new GoalFunction<ClassName>() {
    @Override
    public ClassName apply(AbstractGoalContext goal, TypeName goalType, ImmutableList<? extends AbstractStep> parameters) {
      return goal.builders.generatedType.nestedClass(upcase(goal.accept(DtoGoal.getGoalName) + "Updater"));
    }
  });

  private static final GoalCases<ImmutableList<FieldSpec>> fields = new GoalCases<ImmutableList<FieldSpec>>() {
    @Override
    ImmutableList<FieldSpec> regularGoal(RegularGoalContext goal) {
      ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
      if (goal.goal.kind == INSTANCE_METHOD) {
        builder.add(goal.builders.field);
      }
      for (RegularStep parameter : goal.steps) {
        String name = parameter.parameter.name;
        TypeName type = parameter.parameter.type;
        builder.add(FieldSpec.builder(type, name, PRIVATE).build());
      }
      return builder.build();
    }
    @Override
    ImmutableList<FieldSpec> beanGoal(BeanGoalContext goal) {
      FieldSpec field = FieldSpec.builder(goal.goal.goalType, downcase(goal.goal.goalType.simpleName()))
          .build();
      return ImmutableList.of(field);
    }
  };


  private static final GoalCases<ImmutableList<MethodSpec>> updateMethods = new GoalCases<ImmutableList<MethodSpec>>() {
    @Override
    ImmutableList<MethodSpec> regularGoal(RegularGoalContext goal) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (RegularStep parameter : goal.steps) {
        String name = parameter.parameter.name;
        TypeName type = parameter.parameter.type;
        builder.add(methodBuilder(name)
            .returns(goal.accept(typeName))
            .addParameter(parameterSpec(type, name))
            .addCode(parameter.accept(maybeNullCheck))
            .addStatement("this.$N = $N", name, name)
            .addStatement("return this")
            .addModifiers(PUBLIC)
            .build());
      }
      return builder.build();
    }
    @Override
    ImmutableList<MethodSpec> beanGoal(BeanGoalContext goal) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (BeansStep parameter : goal.steps) {
        String name = parameter.validBeanParameter.name;
        TypeName type = parameter.validBeanParameter.type;
        if (parameter.validBeanParameter.collectionType.isPresent()) {
          TypeName collectionType = parameter.validBeanParameter.collectionType.get();
          ParameterizedTypeName iterable = ParameterizedTypeName.get(ClassName.get(Iterable.class),
              subtypeOf(collectionType));
          CodeBlock clearCollection = CodeBlock.builder().addStatement("this.$N.$N().clear()",
              downcase(goal.goal.goalType.simpleName()),
              parameter.validBeanParameter.projectionMethodName).build();
          builder.add(methodBuilder(name)
              .returns(goal.accept(typeName))
              .addParameter(parameterSpec(iterable, name))
              .addCode(nullCheck(name, name))
              .addCode(clearCollection)
              .beginControlFlow("for ($T $N : $N)", collectionType, iterationVarName, name)
              .addCode(parameter.accept(maybeIterationNullCheck))
              .addStatement("this.$N.$N().add($N)",
                  downcase(goal.goal.goalType.simpleName()),
                  parameter.validBeanParameter.projectionMethodName, iterationVarName)
              .endControlFlow()
              .addStatement("return this")
              .addModifiers(PUBLIC)
              .build());
          builder.add(methodBuilder(name)
              .returns(goal.accept(typeName))
              .addCode(clearCollection)
              .addStatement("return this")
              .addModifiers(PUBLIC)
              .build());
          if (parameter.validBeanParameter.collectionType.allowShortcut) {
            builder.add(methodBuilder(name)
                .returns(goal.accept(typeName))
                .addParameter(parameterSpec(collectionType, name))
                .addCode(parameter.accept(maybeNullCheck))
                .addCode(clearCollection)
                .addStatement("this.$N.$N().add($N)",
                    downcase(goal.goal.goalType.simpleName()),
                    parameter.validBeanParameter.projectionMethodName, name)
                .addStatement("return this")
                .addModifiers(PUBLIC)
                .build());
          }
        } else {
          builder.add(methodBuilder(name)
              .returns(goal.accept(typeName))
              .addParameter(parameterSpec(type, name))
              .addStatement("this.$N.set$L($N)",
                  downcase(goal.goal.goalType.simpleName()), upcase(name), name)
              .addStatement("return this")
              .addModifiers(PUBLIC)
              .build());
        }
      }
      return builder.build();
    }
  };

  private static final GoalCases<MethodSpec> buildMethod = new GoalCases<MethodSpec>() {
    @Override
    MethodSpec regularGoal(RegularGoalContext goal) {
      return methodBuilder("build")
          .addModifiers(PUBLIC)
          .returns(goal.goal.goalType)
          .addCode(goal.accept(invoke))
          .addExceptions(goal.thrownTypes)
          .build();
    }
    @Override
    MethodSpec beanGoal(BeanGoalContext goal) {
      return methodBuilder("build")
          .addModifiers(PUBLIC)
          .returns(goal.goal.goalType)
          .addCode(goal.accept(invoke))
          .build();
    }
  };

  static TypeSpec defineUpdater(AbstractGoalContext goal) {
    return classBuilder(goal.accept(typeName))
        .addFields(goal.accept(fields))
        .addMethods(goal.accept(updateMethods))
        .addMethod(goal.accept(buildMethod))
        .addModifiers(PUBLIC, FINAL, STATIC)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build();
  }

  private UpdaterContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
