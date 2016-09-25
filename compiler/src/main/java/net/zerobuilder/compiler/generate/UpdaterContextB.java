package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.StepContext.BeansStep;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.iterationVarName;
import static net.zerobuilder.compiler.Utilities.nullCheck;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.Utilities.upcase;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorB.ITERABLE;
import static net.zerobuilder.compiler.generate.StepContext.maybeIterationNullCheck;
import static net.zerobuilder.compiler.generate.StepContext.maybeNullCheck;
import static net.zerobuilder.compiler.generate.UpdaterContext.typeName;

final class UpdaterContextB {

  static final Function<BeanGoalContext, ImmutableList<FieldSpec>> fields
      = new Function<BeanGoalContext, ImmutableList<FieldSpec>>() {
    @Override
    public ImmutableList<FieldSpec> apply(BeanGoalContext goal) {
      FieldSpec field = FieldSpec.builder(goal.goal.goalType, downcase(goal.goal.goalType.simpleName()))
          .build();
      return ImmutableList.of(field);
    }
  };

  static final Function<BeanGoalContext, ImmutableList<MethodSpec>> updateMethods
      = new Function<BeanGoalContext, ImmutableList<MethodSpec>>() {
    @Override
    public ImmutableList<MethodSpec> apply(BeanGoalContext goal) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (BeansStep parameter : goal.steps) {
        String name = parameter.validBeanParameter.name;
        TypeName type = parameter.validBeanParameter.type;
        if (parameter.validBeanParameter.collectionType.isPresent()) {
          TypeName collectionType = parameter.validBeanParameter.collectionType.get();
          ParameterizedTypeName iterable = ParameterizedTypeName.get(ITERABLE, subtypeOf(collectionType));
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

  private UpdaterContextB() {
    throw new UnsupportedOperationException("no instances");
  }
}
