package net.zerobuilder.modules.updater;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoModule.ProjectedModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;
import net.zerobuilder.compiler.generate.DtoProjectedGoal;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoal;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedConstructorGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularStep.AbstractRegularStep;

import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import static net.zerobuilder.compiler.generate.DtoGoalContext.context;
import static net.zerobuilder.compiler.generate.DtoProjectedGoal.goalType;
import static net.zerobuilder.compiler.generate.DtoProjectedGoal.projectedGoalCases;
import static net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.projectedRegularGoalContextCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.constructor;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.rawClassName;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

public final class Updater extends ProjectedModule {

  private static final String moduleName = "updater";

  private static final Function<ProjectedGoal, List<FieldSpec>> fields =
      projectedGoalCases(UpdaterV.fieldsV, UpdaterB.fieldsB);

  private static final Function<ProjectedGoal, List<MethodSpec>> stepMethods =
      projectedGoalCases(UpdaterV.stepMethodsV, UpdaterB.stepMethodsB);

  private static final Function<ProjectedGoal, BuilderMethod> updaterMethod =
      projectedGoalCases(GeneratorV::updaterMethodV, GeneratorB::updaterMethodB);

  private static final Function<ProjectedGoal, List<TypeName>> thrownInDone =
      projectedGoalCases(
          regular -> regular.thrownTypes,
          bean -> emptyList());

  private MethodSpec buildMethod(ProjectedGoal goal) {
    return methodBuilder("done")
        .addModifiers(PUBLIC)
        .addExceptions(thrownInDone.apply(goal))
        .returns(goalType.apply(goal))
        .addCode(invoke.apply(goal))
        .build();
  }

  private TypeSpec defineUpdater(ProjectedGoal projectedGoal) {
    return classBuilder(rawClassName(implType(projectedGoal)).get())
        .addFields(fields.apply(projectedGoal))
        .addMethods(stepMethods.apply(projectedGoal))
        .addTypeVariables(DtoProjectedGoal.instanceTypeParameters.apply(projectedGoal))
        .addMethod(buildMethod(projectedGoal))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(updaterConstructor.apply(projectedGoal))
        .build();
  }

  static TypeName implType(ProjectedGoal projectedGoal) {
    AbstractGoalContext goal = goalContext(projectedGoal);
    String implName = upcase(goal.name()) + upcase(moduleName);
    return parameterizedTypeName(context.apply(goal)
        .generatedType.nestedClass(implName), DtoProjectedGoal.instanceTypeParameters.apply(projectedGoal));
  }

  private static final Function<ProjectedRegularGoalContext, MethodSpec> regularConstructor =
      projectedRegularGoalContextCases(
          method -> constructor(PRIVATE),
          constructor -> constructor(PRIVATE));

  private static final Function<ProjectedGoal, MethodSpec> updaterConstructor =
      projectedGoalCases(
          Updater.regularConstructor,
          bean -> constructorBuilder()
              .addModifiers(PRIVATE)
              .addExceptions(bean.mayReuse()
                  ? emptyList()
                  : bean.thrownTypes)
              .addCode(bean.mayReuse()
                  ? emptyCodeBlock
                  : statement("this.$N = new $T()", bean.bean(), bean.type()))
              .build());

  private final Function<ProjectedRegularGoalContext, CodeBlock> regularInvoke =
      projectedRegularGoalContextCases(
          this::staticCall,
          this::constructorCall);

  private CodeBlock staticCall(ProjectedMethodGoalContext goal) {
    String method = goal.details.methodName;
    TypeName type = goal.details.goalType;
    ParameterSpec varGoal = parameterSpec(type, '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (goal.mayReuse()) {
      builder.addStatement("this._currently_in_use = false");
    }
    return builder
        .addStatement("$T $N = $T.$N($L)", varGoal.type, varGoal, goal.context.type,
            method, goal.invocationParameters())
        .add(free(goal.steps))
        .addStatement("return $N", varGoal)
        .build();
  }

  private CodeBlock constructorCall(ProjectedConstructorGoalContext goal) {
    TypeName type = goal.details.goalType;
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(rawClassName(type).get().simpleName()));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (goal.mayReuse()) {
      builder.addStatement("this._currently_in_use = false");
    }
    return builder
        .addStatement("$T $N = new $T($L)", varGoal.type, varGoal, type, goal.invocationParameters())
        .add(free(goal.steps))
        .addStatement("return $N", varGoal)
        .build();
  }

  private CodeBlock free(List<? extends AbstractRegularStep> steps) {
    return steps.stream()
        .map(step -> step.regularParameter())
        .filter(parameter -> !parameter.type.isPrimitive())
        .map(parameter -> statement("this.$N = null", parameter.name))
        .collect(joinCodeBlocks);
  }

  private CodeBlock returnBean(BeanGoalContext goal) {
    ClassName type = goal.details.goalType;
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(type.simpleName()));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (goal.mayReuse()) {
      builder.addStatement("this._currently_in_use = false");
    }
    builder.addStatement("$T $N = this.$N", varGoal.type, varGoal, goal.bean());
    if (goal.mayReuse()) {
      builder.addStatement("this.$N = null", goal.bean());
    }
    return builder.addStatement("return $N", varGoal).build();
  }

  private final Function<ProjectedGoal, CodeBlock> invoke =
      projectedGoalCases(regularInvoke, this::returnBean);

  static AbstractGoalContext goalContext(ProjectedGoal goal) {
    return DtoProjectedGoal.goalContext.apply(goal);
  }

  static String methodName(AbstractGoalContext goal) {
    return goal.name() + upcase(moduleName);
  }

  static FieldSpec cacheField(ProjectedGoal projectedGoal) {
    TypeName type = implType(projectedGoal);
    return FieldSpec.builder(type, downcase(rawClassName(type).get().simpleName()), PRIVATE)
        .initializer("new $T()", type)
        .build();
  }

  @Override
  protected ModuleOutput process(ProjectedGoal goal) {
    return new ModuleOutput(
        updaterMethod.apply(goal),
        singletonList(defineUpdater(goal)),
        singletonList(cacheField(goal)));
  }
}
