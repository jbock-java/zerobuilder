package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.Analyser.AnalysisResult;
import net.zerobuilder.compiler.GoalContextFactory.GoalKind;

import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.presentInstances;
import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Iterables.toArray;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.anonymousClassBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.GoalContext.contractUpdaterName;
import static net.zerobuilder.compiler.GoalContext.goalTypeName;
import static net.zerobuilder.compiler.GoalContext.maybeAddPublic;
import static net.zerobuilder.compiler.GoalContext.stepsImplTypeName;
import static net.zerobuilder.compiler.GoalContextFactory.GoalKind.INSTANCE_METHOD;
import static net.zerobuilder.compiler.GoalContextFactory.builderImpl;
import static net.zerobuilder.compiler.Messages.JavadocMessages.generatedAnnotations;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.upcase;

final class Generator {

  private final Elements elements;

  private static final String TL = "INSTANCE";

  Generator(Elements elements) {
    this.elements = elements;
  }

  TypeSpec generate(AnalysisResult analysisResult) {
    return classBuilder(analysisResult.config.generatedType)
        .addFields(instanceFields(analysisResult))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .addFields(presentInstances(of(threadLocalField(analysisResult.config))))
        .addMethods(builderMethods(analysisResult))
        .addMethods(toBuilderMethods(analysisResult))
        .addAnnotations(generatedAnnotations(elements))
        .addModifiers(toArray(maybeAddPublic(analysisResult.config.isPublic, FINAL), Modifier.class))
        .addTypes(builderImpls(analysisResult.goals))
        .build();
  }

  private ImmutableList<TypeSpec> builderImpls(ImmutableList<GoalContext> goals) {
    ImmutableList.Builder<TypeSpec> builder = ImmutableList.builder();
    for (GoalContext goal : goals) {
      builder.add(goal.accept(builderImpl));
    }
    return builder.build();
  }

  private Optional<FieldSpec> threadLocalField(BuilderContext config) {
    if (!config.recycle) {
      return absent();
    }
    ClassName generatedTypeName = config.generatedType;
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

  private ImmutableList<MethodSpec> toBuilderMethods(final AnalysisResult analysisResult) {
    return FluentIterable.from(analysisResult.goals).filter(new Predicate<GoalContext>() {
      @Override
      public boolean apply(GoalContext goal) {
        return goal.toBuilder;
      }
    }).transform(new Function<GoalContext, MethodSpec>() {
      @Override
      public MethodSpec apply(GoalContext goal) {
        final String instance = downcase(goal.accept(goalTypeName));
        String methodName = downcase(goal.goalName + "ToBuilder");
        final MethodSpec.Builder method = methodBuilder(methodName)
            .addParameter(goal.goalType, instance);
        final String updater = "updater";
        ClassName updaterType = goal.accept(UpdaterContext.typeName);
        if (analysisResult.config.recycle) {
          method.addStatement("$T $L = $L.get().$N", updaterType, updater,
              TL, updaterField(goal));
        } else {
          method.addStatement("$T $L = new $T()", updaterType, updater,
              updaterType);
        }
        CodeBlock statements = goal.accept(new GoalContext.GoalCases<CodeBlock>() {
          @Override
          CodeBlock regularGoal(GoalContext goal, GoalKind kind) {
            CodeBlock.Builder builder = CodeBlock.builder();
            for (ParameterContext parameter : goal.goalParameters) {
              if (parameter.validParameter.projectionMethodName.isPresent()) {
                builder.addStatement("$N.$N = $N.$N()", updater, parameter.validParameter.name,
                    instance, parameter.validParameter.projectionMethodName.get());
              } else {
                builder.addStatement("$N.$N = $N.$N", updater, parameter.validParameter.name,
                    instance, parameter.validParameter.name);
              }
            }
            return builder.build();
          }
          @Override
          CodeBlock fieldGoal(GoalContext goal, ClassName goalType) {
            CodeBlock.Builder builder = CodeBlock.builder();
            String instance = downcase(goalType.simpleName());
            builder.addStatement("$N.$N = new $T()", updater, instance, goalType);
            for (ParameterContext parameter : goal.goalParameters) {
              String parameterName = upcase(parameter.validParameter.name);
              builder.addStatement("$N.$N.set$L($N.$L())", updater,
                  instance,
                  parameterName,
                  instance,
                  parameter.validParameter.projectionMethodName.get());
            }
            return builder.build();
          }
        });
        method.addCode(statements);
        method.addStatement("return $L", updater);
        return method
            .returns(goal.accept(contractUpdaterName))
            .addModifiers(goal.maybeAddPublic(STATIC)).build();
      }
    }).toList();
  }

  private ImmutableList<MethodSpec> builderMethods(final AnalysisResult analysisResult) {
    return FluentIterable.from(analysisResult.goals)
        .transform(new Function<GoalContext, MethodSpec>() {
          public MethodSpec apply(GoalContext goal) {
            final ClassName stepsType = goal.accept(stepsImplTypeName);
            final MethodSpec.Builder method = methodBuilder(
                downcase(goal.goalName + "Builder"))
                .returns(goal.goalParameters.get(0).stepContract)
                .addModifiers(goal.maybeAddPublic(STATIC));
            final String steps = downcase(stepsType.simpleName());
            method.addCode(analysisResult.config.recycle
                ? CodeBlock.of("$T $N = $N.get().$N;\n", stepsType, steps, TL, stepsField(goal))
                : CodeBlock.of("$T $N = new $T();\n", stepsType, steps, stepsType));
            return goal
                .accept(new GoalContext.GoalCases<MethodSpec.Builder>() {
                  @Override
                  MethodSpec.Builder regularGoal(GoalContext goal, GoalKind kind) {
                    if (kind != INSTANCE_METHOD) {
                      return method;
                    }
                    ClassName instanceType = goal.config.annotatedType;
                    String instance = downcase(instanceType.simpleName());
                    return method
                        .addParameter(ParameterSpec.builder(instanceType, instance).build())
                        .addStatement("$N.$N = $N", steps, '_' + instance, instance);
                  }
                  @Override
                  MethodSpec.Builder fieldGoal(GoalContext goal, ClassName goalType) {
                    return method.addStatement("$N.$N = new $T()", steps, downcase(goalType.simpleName()), goalType);
                  }
                })
                .addStatement("return $N", steps)
                .build();
          }
        })
        .toList();
  }

  private ImmutableList<FieldSpec> instanceFields(AnalysisResult analysisResult) {
    if (!analysisResult.config.recycle) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
    for (GoalContext goal : analysisResult.goals) {
      String goalName = goal.goalName;
      if (goal.toBuilder) {
        ClassName updaterType = goal.accept(UpdaterContext.typeName);
        builder.add(FieldSpec.builder(updaterType,
            downcase(goalName + "Updater"), PRIVATE, FINAL)
            .initializer("new $T()", updaterType).build());
      }
      ClassName stepsType = goal.accept(stepsImplTypeName);
      builder.add(FieldSpec.builder(stepsType,
          downcase(goalName + "Steps"), PRIVATE, FINAL)
          .initializer("new $T()", stepsType).build());
    }
    return builder.build();
  }

  private String updaterField(GoalContext goal) {
    return downcase(goal.goalName + "Updater");
  }

  private String stepsField(GoalContext goal) {
    return downcase(goal.goalName + "Steps");
  }

}
