package net.zerobuilder.compiler.generate;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.GoalContext.AbstractContext;
import net.zerobuilder.compiler.generate.GoalContext.BeanGoalContext;
import net.zerobuilder.compiler.generate.GoalContext.GoalCases;
import net.zerobuilder.compiler.generate.GoalContext.RegularGoalContext;
import net.zerobuilder.compiler.generate.StepContext.AbstractStep;
import net.zerobuilder.compiler.generate.StepContext.BeansStep;
import net.zerobuilder.compiler.generate.StepContext.RegularStep;

import static com.google.common.collect.Iterables.getLast;
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
import static net.zerobuilder.compiler.generate.GoalContext.always;
import static net.zerobuilder.compiler.generate.GoalContext.builderImplName;
import static net.zerobuilder.compiler.generate.GoalContext.stepInterfaceNames;
import static net.zerobuilder.compiler.generate.StepContext.asStepInterface;
import static net.zerobuilder.compiler.generate.StepContext.maybeIterationNullCheck;
import static net.zerobuilder.compiler.generate.StepContext.maybeNullCheck;

final class BuilderImplContext {

  private static final GoalCases<ImmutableList<FieldSpec>> fields
      = new GoalCases<ImmutableList<FieldSpec>>() {
    @Override
    ImmutableList<FieldSpec> regularGoal(RegularGoalContext goal) {
      ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
      if (goal.goal.kind == INSTANCE_METHOD) {
        ClassName receiverType = goal.config.annotatedType;
        builder.add(FieldSpec.builder(receiverType, '_' + downcase(receiverType.simpleName()), PRIVATE).build());
      }
      for (RegularStep parameter : goal.steps.subList(0, goal.steps.size() - 1)) {
        String name = parameter.parameter.name;
        builder.add(FieldSpec.builder(parameter.parameter.type, name, PRIVATE).build());
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

  private static final GoalCases<ImmutableList<TypeSpec>> stepInterfaces = always(new GoalContext.GoalFunction<ImmutableList<TypeSpec>>() {
    @Override
    public ImmutableList<TypeSpec> apply(AbstractContext goal, TypeName goalType, ImmutableList<? extends AbstractStep> parameters) {
      return FluentIterable.from(parameters).transform(asStepInterface).toList();
    }
  });

  private static final GoalCases<ImmutableList<MethodSpec>> stepsButLast
      = new GoalCases<ImmutableList<MethodSpec>>() {
    @Override
    ImmutableList<MethodSpec> regularGoal(RegularGoalContext goal) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (RegularStep parameter : goal.steps.subList(0, goal.steps.size() - 1)) {
        String name = parameter.parameter.name;
        CodeBlock finalBlock = CodeBlock.builder()
            .addStatement("this.$N = $N", name, name)
            .addStatement("return this")
            .build();
        builder.add(regularMethod(parameter, finalBlock, ImmutableList.<TypeName>of()));
      }
      return builder.build();
    }
    @Override
    ImmutableList<MethodSpec> beanGoal(BeanGoalContext goal) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      CodeBlock finalBlock = CodeBlock.builder().addStatement("return this").build();
      for (BeansStep parameter : goal.steps.subList(0, goal.steps.size() - 1)) {
        if (parameter.validBeanParameter.collectionType.isPresent()) {
          builder.addAll(beanCollectionMethods(parameter, goal.goal.goalType, finalBlock));
          if (parameter.validBeanParameter.collectionType.allowShortcut) {
            builder.add(beanCollectionShortcut(parameter, goal.goal.goalType, finalBlock));
          }
        } else {
          builder.add(beanRegularMethod(parameter, goal.goal.goalType, finalBlock));
        }
      }
      return builder.build();
    }
  };

  private static final GoalCases<ImmutableList<MethodSpec>> lastStep = new GoalCases<ImmutableList<MethodSpec>>() {
    @Override
    ImmutableList<MethodSpec> regularGoal(RegularGoalContext goal) {
      RegularStep parameter = getLast(goal.steps);
      return ImmutableList.of(regularMethod(parameter, goal.goalCall, goal.thrownTypes));
    }

    @Override
    ImmutableList<MethodSpec> beanGoal(BeanGoalContext goal) {
      BeansStep parameter = getLast(goal.steps);
      if (parameter.validBeanParameter.collectionType.isPresent()) {
        ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
        builder.addAll(beanCollectionMethods(parameter, goal.goal.goalType, goal.goalCall));
        if (parameter.validBeanParameter.collectionType.allowShortcut) {
          builder.add(beanCollectionShortcut(parameter, goal.goal.goalType, goal.goalCall));
        }
        return builder.build();
      } else {
        return ImmutableList.of(beanRegularMethod(parameter, goal.goal.goalType, goal.goalCall));
      }
    }
  };

  static TypeSpec defineBuilderImpl(AbstractContext goal) {
    return classBuilder(goal.accept(builderImplName))
        .addSuperinterfaces(goal.accept(stepInterfaceNames))
        .addFields(goal.accept(fields))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .addMethods(goal.accept(stepsButLast))
        .addMethods(goal.accept(lastStep))
        .addModifiers(FINAL, STATIC)
        .build();
  }

  static TypeSpec defineContract(AbstractContext goal) {
    return classBuilder(goal.contractName)
        .addTypes(goal.accept(stepInterfaces))
        .addModifiers(PUBLIC, FINAL, STATIC)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build();
  }

  private static MethodSpec regularMethod(RegularStep parameter,
                                          CodeBlock finalBlock, ImmutableList<TypeName> thrownTypes) {
    String name = parameter.parameter.name;
    TypeName type = parameter.parameter.type;
    return methodBuilder(parameter.parameter.name)
        .addAnnotation(Override.class)
        .addParameter(parameterSpec(type, name))
        .addExceptions(thrownTypes)
        .addModifiers(PUBLIC)
        .returns(parameter.nextType)
        .addCode(parameter.accept(maybeNullCheck))
        .addCode(finalBlock).build();
  }

  private static ImmutableList<MethodSpec> beanCollectionMethods(BeansStep parameter, ClassName goalType, CodeBlock finalBlock) {
    String name = parameter.validBeanParameter.name;
    TypeName collectionType = parameter.validBeanParameter.collectionType.get();
    ParameterizedTypeName iterable = ParameterizedTypeName.get(ClassName.get(Iterable.class),
        subtypeOf(collectionType));
    MethodSpec fromIterable = methodBuilder(name)
        .addAnnotation(Override.class)
        .returns(parameter.nextType)
        .addParameter(parameterSpec(iterable, name))
        .addCode(nullCheck(name, name))
        .beginControlFlow("for ($T $N : $N)",
            collectionType, iterationVarName, name)
        .addCode(parameter.accept(maybeIterationNullCheck))
        .addStatement("this.$N.$N().add($N)", downcase(goalType.simpleName()),
            parameter.validBeanParameter.projectionMethodName, iterationVarName)
        .endControlFlow()
        .addCode(finalBlock)
        .addModifiers(PUBLIC)
        .build();
    MethodSpec fromEmpty = methodBuilder(name)
        .addAnnotation(Override.class)
        .returns(parameter.nextType)
        .addCode(finalBlock)
        .addModifiers(PUBLIC)
        .build();
    return ImmutableList.of(fromIterable, fromEmpty);
  }

  private static MethodSpec beanCollectionShortcut(BeansStep parameter, ClassName goalType, CodeBlock finalBlock) {
    String name = parameter.validBeanParameter.name;
    TypeName collectionType = parameter.validBeanParameter.collectionType.get();
    return methodBuilder(name)
        .addAnnotation(Override.class)
        .returns(parameter.nextType)
        .addParameter(parameterSpec(collectionType, name))
        .addCode(parameter.accept(maybeNullCheck))
        .addStatement("this.$N.$N().add($N)", downcase(goalType.simpleName()),
            parameter.validBeanParameter.projectionMethodName, name)
        .addCode(finalBlock)
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec beanRegularMethod(BeansStep parameter, ClassName goalType, CodeBlock finalBlock) {
    String name = parameter.validBeanParameter.name;
    TypeName type = parameter.validBeanParameter.type;
    return methodBuilder(parameter.validBeanParameter.name)
        .addAnnotation(Override.class)
        .addParameter(parameterSpec(type, name))
        .addModifiers(PUBLIC)
        .returns(parameter.nextType)
        .addCode(parameter.accept(maybeNullCheck))
        .addStatement("this.$N.set$L($N)", downcase(goalType.simpleName()), upcase(name), name)
        .addCode(finalBlock).build();
  }

  private BuilderImplContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
