package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.analyse.DtoShared.AnalysisResult;
import net.zerobuilder.compiler.generate.DtoBuilders.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoGoal.GoalCases;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalContext;
import net.zerobuilder.compiler.generate.StepContext.BeansStep;
import net.zerobuilder.compiler.generate.StepContext.RegularStep;

import javax.lang.model.util.Elements;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.presentInstances;
import static com.google.common.collect.ImmutableList.of;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.anonymousClassBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Messages.JavadocMessages.generatedAnnotations;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.iterationVarName;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.Utilities.statement;
import static net.zerobuilder.compiler.Utilities.upcase;
import static net.zerobuilder.compiler.analyse.GoalContextFactory.GoalKind.INSTANCE_METHOD;
import static net.zerobuilder.compiler.generate.BuilderContext.defineBuilderImpl;
import static net.zerobuilder.compiler.generate.BuilderContext.defineContract;
import static net.zerobuilder.compiler.generate.DtoGoal.builderImplName;
import static net.zerobuilder.compiler.generate.DtoGoal.getGoalName;
import static net.zerobuilder.compiler.generate.DtoGoal.goalCasesFunction;
import static net.zerobuilder.compiler.generate.StepContext.maybeIterationNullCheck;
import static net.zerobuilder.compiler.generate.UpdaterContext.defineUpdater;

/**
 * Generates an xyzBuilders class for each {@link net.zerobuilder.Builders} annotated class Xyz.
 */
public final class Generator {

  private final Elements elements;

  /**
   * Name of a {@code static ThreadLocal } that holds an instance of the generated type, if {@code recycle}.
   */
  private static final String TL = "INSTANCE";

  public Generator(Elements elements) {
    this.elements = elements;
  }

  public TypeSpec generate(AnalysisResult analysisResult) {
    return classBuilder(analysisResult.builders.generatedType)
        .addFields(instanceFields(analysisResult))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .addFields(presentInstances(of(threadLocalField(analysisResult.builders))))
        .addMethods(builderMethods(analysisResult))
        .addMethods(toBuilderMethods(analysisResult))
        .addAnnotations(generatedAnnotations(elements))
        .addModifiers(PUBLIC, FINAL)
        .addTypes(nestedGoalTypes(analysisResult.goals))
        .build();
  }

  private ImmutableList<TypeSpec> nestedGoalTypes(ImmutableList<AbstractGoalContext> goals) {
    ImmutableList.Builder<TypeSpec> builder = ImmutableList.builder();
    for (AbstractGoalContext goal : goals) {
      if (goal.toBuilder) {
        builder.add(defineUpdater(goal));
      }
      if (goal.builder) {
        builder.add(defineBuilderImpl(goal));
        builder.add(defineContract(goal));
      }
    }
    return builder.build();
  }

  private Optional<FieldSpec> threadLocalField(BuildersContext builders) {
    if (!builders.recycle) {
      return absent();
    }
    ClassName generatedTypeName = builders.generatedType;
    TypeName threadLocal = ParameterizedTypeName.get(ClassName.get(ThreadLocal.class),
        generatedTypeName);
    MethodSpec initialValue = methodBuilder("initialValue")
        .addAnnotation(Override.class)
        .addModifiers(PROTECTED)
        .returns(generatedTypeName)
        .addStatement("return new $T()", generatedTypeName)
        .build();
    return Optional.of(FieldSpec.builder(threadLocal, TL)
        .initializer("$L", anonymousClassBuilder("")
            .addSuperinterface(threadLocal)
            .addMethod(initialValue)
            .build())
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build());
  }

  private ImmutableList<MethodSpec> toBuilderMethods(AnalysisResult analysisResult) {
    return FluentIterable.from(analysisResult.goals)
        .filter(new Predicate<AbstractGoalContext>() {
          @Override
          public boolean apply(AbstractGoalContext goal) {
            return goal.toBuilder;
          }
        })
        .transform(goalCasesFunction(goalToToBuilder))
        .toList();
  }

  private ImmutableList<MethodSpec> builderMethods(AnalysisResult analysisResult) {
    return FluentIterable.from(analysisResult.goals)
        .filter(new Predicate<AbstractGoalContext>() {
          @Override
          public boolean apply(AbstractGoalContext goal) {
            return goal.builder;
          }
        })
        .transform(new Function<AbstractGoalContext, MethodSpec>() {
          public MethodSpec apply(AbstractGoalContext goal) {
            return goal
                .accept(goalToBuilder);
          }
        })
        .toList();
  }

  private ImmutableList<FieldSpec> instanceFields(AnalysisResult analysisResult) {
    if (!analysisResult.builders.recycle) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
    for (AbstractGoalContext goal : analysisResult.goals) {
      if (goal.toBuilder) {
        ClassName updaterType = goal.accept(UpdaterContext.typeName);
        builder.add(FieldSpec.builder(updaterType,
            updaterField(goal), PRIVATE, FINAL)
            .initializer("new $T()", updaterType).build());
      }
      if (goal.builder) {
        ClassName stepsType = goal.accept(builderImplName);
        builder.add(FieldSpec.builder(stepsType,
            stepsField(goal), PRIVATE, FINAL)
            .initializer("new $T()", stepsType).build());
      }
    }
    return builder.build();
  }

  private static final GoalCases<MethodSpec> goalToToBuilder = new GoalCases<MethodSpec>() {
    @Override
    MethodSpec regularGoal(RegularGoalContext goal) {
      String instance = downcase(((ClassName) goal.goal.goalType.box()).simpleName());
      String methodName = goal.goal.name + "ToBuilder";
      MethodSpec.Builder method = methodBuilder(methodName)
          .addParameter(goal.goal.goalType, instance);
      String updater = "updater";
      ClassName updaterType = goal.accept(UpdaterContext.typeName);
      if (goal.builders.recycle) {
        method.addStatement("$T $L = $L.get().$N", updaterType, updater,
            TL, updaterField(goal));
      } else {
        method.addStatement("$T $L = new $T()", updaterType, updater,
            updaterType);
      }
      CodeBlock.Builder builder = CodeBlock.builder();
      for (RegularStep parameter : goal.steps) {
        if (parameter.parameter.projectionMethodName.isPresent()) {
          if (parameter.parameter.nonNull) {
            builder.add(CodeBlock.builder()
                .beginControlFlow("if ($N.$N() == null)", instance, parameter.parameter.projectionMethodName.get())
                .addStatement("throw new $T($S)", NullPointerException.class, parameter.parameter.name)
                .endControlFlow().build());
          }
          builder.addStatement("$N.$N = $N.$N()", updater, parameter.parameter.name,
              instance, parameter.parameter.projectionMethodName.get());
        } else {
          if (parameter.parameter.nonNull) {
            builder.add(CodeBlock.builder()
                .beginControlFlow("if ($N.$N == null)", instance, parameter.parameter.name)
                .addStatement("throw new $T($S)", NullPointerException.class, parameter.parameter.name)
                .endControlFlow().build());
          }
          builder.add(CodeBlock.builder()
              .addStatement("$N.$N = $N.$N", updater, parameter.parameter.name,
                  instance, parameter.parameter.name).build());
        }
      }
      method.addCode(builder.build());
      method.addStatement("return $L", updater);
      return method
          .returns(goal.accept(UpdaterContext.typeName))
          .addModifiers(PUBLIC, STATIC).build();
    }
    @Override
    MethodSpec beanGoal(BeanGoalContext goal) {
      String instance = downcase(goal.goal.goalType.simpleName());
      String methodName = downcase(goal.goal.name + "ToBuilder");
      CodeBlock.Builder builder = CodeBlock.builder();
      MethodSpec.Builder method = methodBuilder(methodName)
          .addParameter(goal.goal.goalType, instance);
      String updater = "updater";
      ClassName updaterType = goal.accept(UpdaterContext.typeName);
      if (goal.builders.recycle) {
        method.addStatement("$T $L = $L.get().$N", updaterType, updater,
            TL, updaterField(goal));
      } else {
        method.addStatement("$T $L = new $T()", updaterType, updater,
            updaterType);
      }
      builder.addStatement("$N.$N = new $T()", updater, instance, goal.goal.goalType);
      for (BeansStep parameter : goal.steps) {
        String parameterName = upcase(parameter.validBeanParameter.name);
        CodeBlock nullCheck = CodeBlock.builder()
            .beginControlFlow("if ($N.$N() == null)", instance,
                parameter.validBeanParameter.projectionMethodName)
            .addStatement("throw new $T($S)",
                NullPointerException.class, parameter.validBeanParameter.name)
            .endControlFlow().build();
        if (parameter.validBeanParameter.collectionType.isPresent()) {
          TypeName collectionType = parameter.validBeanParameter.collectionType.get();
          builder.add(nullCheck)
              .beginControlFlow("for ($T $N : $N.$N())",
                  collectionType, iterationVarName, instance,
                  parameter.validBeanParameter.projectionMethodName)
              .add(parameter.accept(maybeIterationNullCheck))
              .addStatement("$N.$N.$N().add($N)", updater,
                  downcase(goal.goal.goalType.simpleName()),
                  parameter.validBeanParameter.projectionMethodName,
                  iterationVarName)
              .endControlFlow();
        } else {
          if (parameter.validBeanParameter.nonNull) {
            builder.add(nullCheck);
          }
          builder.addStatement("$N.$N.set$L($N.$N())", updater,
              instance,
              parameterName,
              instance,
              parameter.validBeanParameter.projectionMethodName);
        }
      }
      method.addCode(builder.build());
      method.addStatement("return $L", updater);
      return method
          .returns(goal.accept(UpdaterContext.typeName))
          .addModifiers(PUBLIC, STATIC).build();
    }
  };

  private static final GoalCases<MethodSpec> goalToBuilder = new GoalCases<MethodSpec>() {
    @Override
    MethodSpec regularGoal(RegularGoalContext goal) {
      ClassName stepsType = goal.accept(builderImplName);
      MethodSpec.Builder method = methodBuilder(goal.accept(getGoalName) + "Builder")
          .returns(goal.steps.get(0).thisType)
          .addModifiers(PUBLIC, STATIC);
      String steps = downcase(stepsType.simpleName());
      method.addCode(goal.builders.recycle
          ? statement("$T $N = $N.get().$N", stepsType, steps, TL, stepsField(goal))
          : statement("$T $N = new $T()", stepsType, steps, stepsType));
      if (goal.goal.kind == INSTANCE_METHOD) {
        ClassName instanceType = goal.builders.type;
        String instance = downcase(instanceType.simpleName());
        return method
            .addParameter(parameterSpec(instanceType, instance))
            .addStatement("$N.$N = $N", steps, goal.builders.field, instance)
            .addStatement("return $N", steps)
            .build();
      } else {
        return method.addStatement("return $N", steps).build();
      }
    }
    @Override
    MethodSpec beanGoal(BeanGoalContext goal) {
      ClassName stepsType = goal.accept(builderImplName);
      MethodSpec.Builder method = methodBuilder(goal.goal.name + "Builder")
          .returns(goal.steps.get(0).thisType)
          .addModifiers(PUBLIC, STATIC);
      String steps = downcase(stepsType.simpleName());
      method.addCode(goal.builders.recycle
          ? statement("$T $N = $N.get().$N", stepsType, steps, TL, stepsField(goal))
          : statement("$T $N = new $T()", stepsType, steps, stepsType));
      return method.addStatement("$N.$N = new $T()", steps,
          downcase(goal.goal.goalType.simpleName()), goal.goal.goalType)
          .addStatement("return $N", steps)
          .build();
    }
  };

  private static String updaterField(AbstractGoalContext goal) {
    return downcase(goal.accept(getGoalName) + "Updater");
  }

  private static String stepsField(AbstractGoalContext goal) {
    return downcase(goal.accept(getGoalName) + "BuilderImpl");
  }

}
