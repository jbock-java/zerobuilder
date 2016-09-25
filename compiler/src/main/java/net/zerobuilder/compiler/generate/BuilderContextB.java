package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.StepContext.BeansStep;

import static com.google.common.collect.Iterables.getLast;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.iterationVarName;
import static net.zerobuilder.compiler.Utilities.nullCheck;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.Utilities.upcase;
import static net.zerobuilder.compiler.generate.StepContext.maybeIterationNullCheck;
import static net.zerobuilder.compiler.generate.StepContext.maybeNullCheck;

final class BuilderContextB {

  static final Function<BeanGoalContext, ImmutableList<FieldSpec>> fields
      = new Function<BeanGoalContext, ImmutableList<FieldSpec>>() {
    @Override
    public ImmutableList<FieldSpec> apply(BeanGoalContext goal) {
      return ImmutableList.of(goal.field);
    }
  };

  static final Function<BeanGoalContext, ImmutableList<MethodSpec>> stepsButLast
      = new Function<BeanGoalContext, ImmutableList<MethodSpec>>() {
    @Override
    public ImmutableList<MethodSpec> apply(BeanGoalContext goal) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      CodeBlock finalBlock = CodeBlock.builder().addStatement("return this").build();
      for (BeansStep parameter : goal.steps.subList(0, goal.steps.size() - 1)) {
        if (parameter.validBeanParameter.collectionType.isPresent()) {
          builder.addAll(beanCollectionMethods(parameter, goal, finalBlock));
          if (parameter.validBeanParameter.collectionType.allowShortcut) {
            builder.add(beanCollectionShortcut(parameter, goal, finalBlock));
          }
        } else {
          builder.add(beanRegularMethod(parameter, goal, finalBlock));
        }
      }
      return builder.build();
    }
  };

  static final Function<BeanGoalContext, ImmutableList<MethodSpec>> lastStep
      = new Function<BeanGoalContext, ImmutableList<MethodSpec>>() {
    @Override
    public ImmutableList<MethodSpec> apply(BeanGoalContext goal) {
      BeansStep parameter = getLast(goal.steps);
      if (parameter.validBeanParameter.collectionType.isPresent()) {
        ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
        builder.addAll(beanCollectionMethods(parameter, goal, invoke.apply(goal)));
        if (parameter.validBeanParameter.collectionType.allowShortcut) {
          builder.add(beanCollectionShortcut(parameter, goal, invoke.apply(goal)));
        }
        return builder.build();
      } else {
        return ImmutableList.of(beanRegularMethod(parameter, goal, invoke.apply(goal)));
      }
    }
  };

  private static ImmutableList<MethodSpec> beanCollectionMethods(BeansStep parameter, BeanGoalContext goal, CodeBlock finalBlock) {
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
        .addStatement("this.$N.$N().add($N)", goal.field,
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

  private static MethodSpec beanCollectionShortcut(BeansStep parameter, BeanGoalContext goal, CodeBlock finalBlock) {
    String name = parameter.validBeanParameter.name;
    TypeName collectionType = parameter.validBeanParameter.collectionType.get();
    return methodBuilder(name)
        .addAnnotation(Override.class)
        .returns(parameter.nextType)
        .addParameter(parameterSpec(collectionType, name))
        .addCode(parameter.accept(maybeNullCheck))
        .addStatement("this.$N.$N().add($N)", goal.field,
            parameter.validBeanParameter.projectionMethodName, name)
        .addCode(finalBlock)
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec beanRegularMethod(BeansStep parameter, BeanGoalContext goal, CodeBlock finalBlock) {
    String name = parameter.validBeanParameter.name;
    TypeName type = parameter.validBeanParameter.type;
    return methodBuilder(parameter.validBeanParameter.name)
        .addAnnotation(Override.class)
        .addParameter(parameterSpec(type, name))
        .addModifiers(PUBLIC)
        .returns(parameter.nextType)
        .addCode(parameter.accept(maybeNullCheck))
        .addStatement("this.$N.set$L($N)", goal.field, upcase(name), name)
        .addCode(finalBlock).build();
  }

  static final Function<BeanGoalContext, CodeBlock> invoke
      = new Function<BeanGoalContext, CodeBlock>() {
    @Override
    public CodeBlock apply(BeanGoalContext goal) {
      return CodeBlock.builder()
          .addStatement("return $N", goal.field)
          .build();
    }
  };

  private BuilderContextB() {
    throw new UnsupportedOperationException("no instances");
  }
}
