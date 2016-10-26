package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoModuleOutput.SimpleModuleOutput;
import net.zerobuilder.compiler.generate.DtoProjectedGoal.ProjectedGoal;
import net.zerobuilder.compiler.generate.DtoProjectedModule.ProjectedSimpleModule;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularGoal.AbstractRegularGoalContext;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoProjectedGoal.goalType;
import static net.zerobuilder.compiler.generate.DtoProjectedGoal.projectedGoalCases;
import static net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.goalDetails;
import static net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.projectedRegularGoalContextCases;
import static net.zerobuilder.compiler.generate.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.Utilities.statement;

public final class Updater extends ProjectedSimpleModule {

  private Function<ProjectedGoal, List<FieldSpec>> fields(UpdaterB updaterB, UpdaterV updaterV) {
    return projectedGoalCases(updaterV.fieldsV, updaterB.fieldsB);
  }

  private Function<ProjectedGoal, List<MethodSpec>> updateMethods(UpdaterB updaterB, UpdaterV updaterV) {
    return projectedGoalCases(updaterV.updateMethodsV, updaterB.updateMethodsB);
  }

  private Function<ProjectedGoal, DtoGeneratorOutput.BuilderMethod> goalToUpdater(
      GeneratorBU generatorBU, GeneratorVU generatorVU) {
    return projectedGoalCases(generatorVU::goalToUpdaterV, generatorBU::goalToUpdaterB);
  }

  private MethodSpec buildMethod(ProjectedGoal goal) {
    return methodBuilder("done")
        .addModifiers(PUBLIC)
        .returns(goalType.apply(goal))
        .addCode(invoke.apply(goal))
        .build();
  }

  private TypeSpec defineUpdater(ProjectedGoal projectedGoal) {
    UpdaterB updaterB = new UpdaterB(this);
    UpdaterV updaterV = new UpdaterV(this);
    return classBuilder(implType(projectedGoal))
        .addFields(fields(updaterB, updaterV).apply(projectedGoal))
        .addMethods(updateMethods(updaterB, updaterV).apply(projectedGoal))
        .addMethod(buildMethod(projectedGoal))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(builderConstructor.apply(projectedGoal))
        .build();
  }

  private static final Function<ProjectedGoal, MethodSpec> builderConstructor =
      projectedGoalCases(
          DtoProjectedRegularGoalContext.builderConstructor,
          bean -> constructorBuilder()
              .addModifiers(PRIVATE)
              .addExceptions(bean.context.lifecycle == REUSE_INSTANCES
                  ? Collections.emptyList()
                  : bean.thrownTypes)
              .addCode(bean.context.lifecycle == REUSE_INSTANCES
                  ? emptyCodeBlock
                  : statement("this.$N = new $T()", bean.bean(), bean.type()))
              .build());

  private static final Function<ProjectedRegularGoalContext, CodeBlock> regularInvoke =
      projectedRegularGoalContextCases(
          goal -> goal.methodGoalInvocation(),
          goal -> statement("return new $T($L)", goalDetails.apply(goal).goalType,
              goal.invocationParameters()));


  private static final Function<BeanGoalContext, CodeBlock> returnBean
      = goal -> statement("return this.$N", goal.bean());

  private static final Function<ProjectedGoal, CodeBlock> invoke
      = projectedGoalCases(regularInvoke, returnBean);

  @Override
  public String name() {
    return "updater";
  }

  @Override
  protected SimpleModuleOutput process(ProjectedGoal goal) {
    GeneratorBU generatorBU = new GeneratorBU(this);
    GeneratorVU generatorVU = new GeneratorVU(this);
    return new SimpleModuleOutput(
        goalToUpdater(generatorBU, generatorVU).apply(goal),
        defineUpdater(goal));
  }
}
