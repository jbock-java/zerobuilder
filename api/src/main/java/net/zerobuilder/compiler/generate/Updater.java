package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.ProjectedSimpleModuleOutput;
import net.zerobuilder.compiler.generate.DtoGoalContext.AbstractGoalContext;
import net.zerobuilder.compiler.generate.DtoModuleOutput.SimpleModuleOutput;
import net.zerobuilder.compiler.generate.DtoRegularGoal.AbstractRegularGoalContext;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Collections.singletonList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoGoalContext.goalCases;
import static net.zerobuilder.compiler.generate.DtoRegularGoal.regularGoalContextCases;
import static net.zerobuilder.compiler.generate.Utilities.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.Utilities.statement;

public final class Updater extends DtoModule.SimpleModule {

  private Function<AbstractGoalContext, List<FieldSpec>> fields(UpdaterB updaterB, UpdaterV updaterV) {
    return goalCases(updaterV.fieldsV, updaterB.fieldsB);
  }

  private Function<AbstractGoalContext, List<MethodSpec>> updateMethods(UpdaterB updaterB, UpdaterV updaterV) {
    return goalCases(updaterV.updateMethodsV, updaterB.updateMethodsB);
  }

  private Function<AbstractGoalContext, DtoGeneratorOutput.BuilderMethod> goalToUpdater(
      GeneratorBU generatorBU, GeneratorVU generatorVU) {
    return goalCases(generatorVU::goalToUpdaterV, generatorBU::goalToUpdaterB);
  }

  private MethodSpec buildMethod(AbstractGoalContext goal) {
    return methodBuilder("done")
        .addModifiers(PUBLIC)
        .returns(goal.goalType())
        .addCode(invoke.apply(goal))
        .build();
  }

  private TypeSpec defineUpdater(AbstractGoalContext goal) {
    UpdaterB updaterB = new UpdaterB(this);
    UpdaterV updaterV = new UpdaterV(this);
    return classBuilder(implType(goal))
        .addFields(fields(updaterB, updaterV).apply(goal))
        .addMethods(updateMethods(updaterB, updaterV).apply(goal))
        .addMethod(buildMethod(goal))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(builderConstructor.apply(goal))
        .build();
  }

  private static final Function<AbstractGoalContext, MethodSpec> builderConstructor =
      goalCases(
          AbstractRegularGoalContext::builderConstructor,
          bGoal -> constructorBuilder()
              .addModifiers(PRIVATE)
              .addExceptions(bGoal.context.lifecycle == REUSE_INSTANCES
                  ? Collections.emptyList()
                  : bGoal.thrownTypes)
              .addCode(bGoal.context.lifecycle == REUSE_INSTANCES
                  ? emptyCodeBlock
                  : statement("this.$N = new $T()", bGoal.bean(), bGoal.type()))
              .build());

  private static final Function<AbstractRegularGoalContext, CodeBlock> regularInvoke =
      regularGoalContextCases(
          goal -> statement("return new $T($L)", goal.type(),
              goal.invocationParameters()),
          goal -> goal.methodGoalInvocation());


  private static final Function<BeanGoalContext, CodeBlock> returnBean
      = goal -> statement("return this.$N", goal.bean());

  private static final Function<AbstractGoalContext, CodeBlock> invoke
      = goalCases(regularInvoke, returnBean);

  @Override
  public String name() {
    return "updater";
  }

  @Override
  protected SimpleModuleOutput process(AbstractGoalContext goal) {
    GeneratorBU generatorBU = new GeneratorBU(this);
    GeneratorVU generatorVU = new GeneratorVU(this);
    return new SimpleModuleOutput(
        goalToUpdater(generatorBU, generatorVU).apply(goal),
        defineUpdater(goal));
  }
}
