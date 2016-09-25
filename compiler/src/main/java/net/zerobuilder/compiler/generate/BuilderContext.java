package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
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
import net.zerobuilder.compiler.generate.DtoGoal.GoalContextCommon;
import net.zerobuilder.compiler.generate.DtoGoal.RegularGoalContext;
import net.zerobuilder.compiler.generate.StepContext.BeansStep;
import net.zerobuilder.compiler.generate.StepContext.RegularStep;

import static com.google.common.collect.Iterables.getLast;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeName.VOID;
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
import static net.zerobuilder.compiler.generate.DtoGoal.always;
import static net.zerobuilder.compiler.generate.DtoGoal.builderImplName;
import static net.zerobuilder.compiler.generate.DtoGoal.stepInterfaceNames;
import static net.zerobuilder.compiler.generate.StepContext.asStepInterface;
import static net.zerobuilder.compiler.generate.StepContext.maybeIterationNullCheck;
import static net.zerobuilder.compiler.generate.StepContext.maybeNullCheck;

final class BuilderContext {

  private static final GoalCases<ImmutableList<FieldSpec>> fields
      = new GoalCases<ImmutableList<FieldSpec>>() {
    @Override
    public ImmutableList<FieldSpec> regularGoal(RegularGoalContext goal) {
      ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
      if (goal.goal.kind == INSTANCE_METHOD) {
        builder.add(goal.builders.field);
      }
      for (RegularStep parameter : goal.steps.subList(0, goal.steps.size() - 1)) {
        String name = parameter.parameter.name;
        builder.add(FieldSpec.builder(parameter.parameter.type, name, PRIVATE).build());
      }
      return builder.build();
    }
    @Override
    public ImmutableList<FieldSpec> beanGoal(BeanGoalContext goal) {
      FieldSpec field = FieldSpec.builder(goal.goal.goalType, downcase(goal.goal.goalType.simpleName()))
          .build();
      return ImmutableList.of(field);
    }
  };

  private static final GoalCases<ImmutableList<TypeSpec>> stepInterfaces
      = always(new Function<GoalContextCommon, ImmutableList<TypeSpec>>() {
    @Override
    public ImmutableList<TypeSpec> apply(GoalContextCommon goal) {
      return FluentIterable.from(goal.parameters).transform(asStepInterface).toList();
    }
  });

  private static final GoalCases<ImmutableList<MethodSpec>> stepsButLast
      = new GoalCases<ImmutableList<MethodSpec>>() {
    @Override
    public ImmutableList<MethodSpec> regularGoal(RegularGoalContext goal) {
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
    public ImmutableList<MethodSpec> beanGoal(BeanGoalContext goal) {
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
    public ImmutableList<MethodSpec> regularGoal(RegularGoalContext goal) {
      RegularStep parameter = getLast(goal.steps);
      return ImmutableList.of(regularMethod(parameter, goal.accept(invoke), goal.thrownTypes));
    }

    @Override
    public ImmutableList<MethodSpec> beanGoal(BeanGoalContext goal) {
      BeansStep parameter = getLast(goal.steps);
      if (parameter.validBeanParameter.collectionType.isPresent()) {
        ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
        builder.addAll(beanCollectionMethods(parameter, goal.goal.goalType, goal.accept(invoke)));
        if (parameter.validBeanParameter.collectionType.allowShortcut) {
          builder.add(beanCollectionShortcut(parameter, goal.goal.goalType, goal.accept(invoke)));
        }
        return builder.build();
      } else {
        return ImmutableList.of(beanRegularMethod(parameter, goal.goal.goalType, goal.accept(invoke)));
      }
    }
  };

  static TypeSpec defineBuilderImpl(AbstractGoalContext goal) {
    return classBuilder(goal.accept(builderImplName))
        .addSuperinterfaces(goal.accept(stepInterfaceNames))
        .addFields(goal.accept(fields))
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .addMethods(goal.accept(stepsButLast))
        .addMethods(goal.accept(lastStep))
        .addModifiers(FINAL, STATIC)
        .build();
  }

  static TypeSpec defineContract(AbstractGoalContext goal) {
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

  static final GoalCases<CodeBlock> invoke
      = new GoalCases<CodeBlock>() {
    @Override
    public CodeBlock regularGoal(RegularGoalContext goal) {
      CodeBlock parameters = CodeBlock.of(Joiner.on(", ").join(goal.goal.parameterNames));
      CodeBlock.Builder builder = CodeBlock.builder();
      builder.add(VOID.equals(goal.goal.goalType) ?
          CodeBlock.of("") :
          CodeBlock.of("return "));
      switch (goal.goal.kind) {
        case CONSTRUCTOR:
          return builder
              .addStatement("new $T($L)",
                  goal.goal.goalType, parameters).build();
        case INSTANCE_METHOD:
          return builder.addStatement("$N.$N($L)",
              goal.builders.field, goal.goal.methodName, parameters).build();
        case STATIC_METHOD:
          return builder.addStatement("$T.$N($L)",
              goal.builders.type, goal.goal.methodName, parameters).build();
        default:
          throw new IllegalStateException("unknown kind: " + goal.goal.kind);
      }
    }
    @Override
    public CodeBlock beanGoal(BeanGoalContext goal) {
      return CodeBlock.builder()
          .addStatement("return $L", downcase(goal.goal.goalType.simpleName()))
          .build();
    }
  };

  private BuilderContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
