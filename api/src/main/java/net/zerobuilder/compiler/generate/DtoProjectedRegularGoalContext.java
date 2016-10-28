package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractRegularGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoal.MethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoal;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoalCases;
import net.zerobuilder.compiler.generate.DtoRegularGoalContext.RegularGoalContext;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeName.VOID;
import static java.util.Optional.empty;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoGoal.GoalMethodType.INSTANCE_METHOD;
import static net.zerobuilder.compiler.generate.DtoRegularStep.ProjectedRegularStep;
import static net.zerobuilder.compiler.generate.Utilities.asPredicate;
import static net.zerobuilder.compiler.generate.Utilities.constructor;
import static net.zerobuilder.compiler.generate.Utilities.downcase;
import static net.zerobuilder.compiler.generate.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.Utilities.fieldSpec;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.Utilities.statement;

public final class DtoProjectedRegularGoalContext {

  interface ProjectedRegularGoalContextCases<R> {
    R method(ProjectedMethodGoalContext method);
    R constructor(ProjectedConstructorGoalContext constructor);
  }

  static abstract class ProjectedRegularGoalContext extends RegularGoalContext
      implements ProjectedGoal {

    ProjectedRegularGoalContext(List<TypeName> thrownTypes) {
      super(thrownTypes);
    }

    abstract <R> R acceptRegularProjected(ProjectedRegularGoalContextCases<R> cases);

    @Override
    public final <R> R acceptProjected(ProjectedGoalCases<R> cases) {
      return cases.regular(this);
    }

    @Override
    <R> R acceptRegular(DtoRegularGoalContext.RegularGoalContextCases<R> cases) {
      return cases.projected(this);
    }

    final CodeBlock invocationParameters() {
      return CodeBlock.of(String.join(", ", goalDetails.apply(this).parameterNames));
    }
  }

  static <R> Function<ProjectedRegularGoalContext, R> asFunction(ProjectedRegularGoalContextCases<R> cases) {
    return goal -> goal.acceptRegularProjected(cases);
  }

  static <R> Function<ProjectedRegularGoalContext, R> projectedRegularGoalContextCases(
      Function<ProjectedMethodGoalContext, R> methodFunction,
      Function<ProjectedConstructorGoalContext, R> constructorFunction) {
    return asFunction(new ProjectedRegularGoalContextCases<R>() {
      @Override
      public R method(ProjectedMethodGoalContext method) {
        return methodFunction.apply(method);
      }
      @Override
      public R constructor(ProjectedConstructorGoalContext constructor) {
        return constructorFunction.apply(constructor);
      }
    });
  }

  static final class ProjectedMethodGoalContext extends ProjectedRegularGoalContext {
    final List<ProjectedRegularStep> steps;
    final BuildersContext context;
    final MethodGoalDetails details;

    FieldSpec field() {
      ClassName type = context.type;
      String name = '_' + downcase(type.simpleName());
      return context.lifecycle == REUSE_INSTANCES
          ? fieldSpec(type, name, PRIVATE)
          : fieldSpec(type, name, PRIVATE, FINAL);
    }

    CodeBlock methodGoalInvocation() {
      TypeName type = details.goalType;
      String method = details.methodName;
      return details.methodType == INSTANCE_METHOD ?
          statement("return this.$N.$N($L)", field(), method, invocationParameters()) :
          CodeBlock.builder()
              .add(VOID.equals(type) ? emptyCodeBlock : CodeBlock.of("return "))
              .addStatement("$T.$N($L)", context.type, method, invocationParameters())
              .build();
    }

    ProjectedMethodGoalContext(
        BuildersContext context,
        MethodGoalDetails details,
        List<ProjectedRegularStep> steps,
        List<TypeName> thrownTypes) {
      super(thrownTypes);
      this.context = context;
      this.details = details;
      this.steps = steps;
    }

    @Override
    public <R> R acceptRegularProjected(ProjectedRegularGoalContextCases<R> cases) {
      return cases.method(this);
    }
  }

  static final class ProjectedConstructorGoalContext
      extends ProjectedRegularGoalContext {

    final BuildersContext context;
    final DtoGoal.ConstructorGoalDetails details;
    final List<ProjectedRegularStep> steps;

    ProjectedConstructorGoalContext(BuildersContext context,
                                    DtoGoal.ConstructorGoalDetails details,
                                    List<ProjectedRegularStep> steps,
                                    List<TypeName> thrownTypes) {
      super(thrownTypes);
      this.context = context;
      this.details = details;
      this.steps = steps;
    }

    @Override
    public <R> R acceptRegularProjected(ProjectedRegularGoalContextCases<R> cases) {
      return cases.constructor(this);
    }
  }

  static final Function<ProjectedRegularGoalContext, MethodSpec> builderConstructor =
      DtoProjectedRegularGoalContext.projectedRegularGoalContextCases(
          method -> {
            if (method.details.methodType != INSTANCE_METHOD
                || method.context.lifecycle == REUSE_INSTANCES) {
              return constructor(PRIVATE);
            }
            ClassName type = method.context.type;
            ParameterSpec parameter = parameterSpec(type, downcase(type.simpleName()));
            return constructorBuilder()
                .addParameter(parameter)
                .addStatement("this.$N = $N", method.field(), parameter)
                .addModifiers(PRIVATE)
                .build();
          },
          constructor -> constructor(PRIVATE));

  private static final Predicate<ProjectedRegularGoalContext> isInstance =
      asPredicate(DtoProjectedRegularGoalContext.projectedRegularGoalContextCases(
          method -> method.details.methodType == INSTANCE_METHOD,
          constructor -> false));

  static final Function<ProjectedRegularGoalContext, AbstractRegularGoalDetails> goalDetails =
      projectedRegularGoalContextCases(
          method -> method.details,
          constructor -> constructor.details);

  static final Function<ProjectedRegularGoalContext, List<ProjectedRegularStep>> steps = DtoProjectedRegularGoalContext.projectedRegularGoalContextCases(
      method -> method.steps,
      constructor -> constructor.steps);

  static final Function<ProjectedRegularGoalContext, Optional<FieldSpec>> fields =
      projectedRegularGoalContextCases(
          method -> isInstance.test(method) ?
              Optional.of(method.field()) :
              empty(),
          constructor -> empty());

  private DtoProjectedRegularGoalContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
