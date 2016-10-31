package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.DescriptionInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.AbstractGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.GeneratorInput;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorOutput;
import net.zerobuilder.compiler.generate.DtoInputOutput.AbstractInputOutput;
import net.zerobuilder.compiler.generate.DtoInputOutput.InputOutput;
import net.zerobuilder.compiler.generate.DtoInputOutput.ProjectedInputOutput;
import net.zerobuilder.compiler.generate.DtoModule.ContractModule;
import net.zerobuilder.compiler.generate.DtoModule.ModuleCases;
import net.zerobuilder.compiler.generate.DtoModule.SimpleModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.AbstractModuleOutput;
import net.zerobuilder.compiler.generate.DtoProjectedModule.ProjectedContractModule;
import net.zerobuilder.compiler.generate.DtoProjectedModule.ProjectedModuleCases;
import net.zerobuilder.compiler.generate.DtoProjectedModule.ProjectedSimpleModule;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoContext.BuilderLifecycle.NEW_INSTANCE;
import static net.zerobuilder.compiler.generate.DtoGeneratorInput.goalInputCases;
import static net.zerobuilder.compiler.generate.DtoModuleOutput.moduleOutputCases;
import static net.zerobuilder.compiler.generate.GoalContextFactory.prepare;
import static net.zerobuilder.compiler.generate.ZeroUtil.flatList;
import static net.zerobuilder.compiler.generate.ZeroUtil.listCollector;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;

public final class Generator {

  /**
   * Entry point for code generation.
   *
   * @param generatorInput Goal descriptions
   * @return a GeneratorOutput
   */
  public static GeneratorOutput generate(GeneratorInput generatorInput) {
    List<DescriptionInput> goals = generatorInput.goals;
    BuildersContext context = generatorInput.context;
    return goals.stream()
        .map(prepare(context))
        .map(process)
        .collect(collectOutput(context));
  }

  private static Collector<AbstractInputOutput, List<AbstractInputOutput>, GeneratorOutput> collectOutput(BuildersContext context) {
    return listCollector(tmpOutputs ->
        GeneratorOutput.create(
            methods(tmpOutputs),
            types(tmpOutputs),
            fields(context, tmpOutputs),
            context));
  }

  private static List<BuilderMethod> methods(List<AbstractInputOutput> inputOutputs) {
    return transform(inputOutputs, tmp -> tmp.output.method);
  }

  private static List<TypeSpec> types(List<AbstractInputOutput> inputOutputs) {
    return inputOutputs.stream()
        .map(tmp -> nestedTypes.apply(tmp.output))
        .collect(flatList());
  }

  private static List<FieldSpec> fields(BuildersContext context, List<AbstractInputOutput> inputOutputs) {
    if (context.lifecycle == NEW_INSTANCE) {
      return emptyList();
    }
    List<FieldSpec> fields = new ArrayList<>(inputOutputs.size() + 1);
    fields.add(context.cache.get());
    fields.add(FieldSpec.builder(TypeName.INT, "refs", PRIVATE).build());
    inputOutputs.forEach(tmp ->
        fields.add(tmp.cacheField()));
    return fields;
  }

  private static final Function<AbstractGoalInput, AbstractInputOutput> process =
      goalInputCases(
          simpleInput -> new InputOutput(simpleInput.module, simpleInput.goal,
              simpleInput.module.accept(new ModuleCases<AbstractModuleOutput, Void>() {
                @Override
                public AbstractModuleOutput simple(SimpleModule module, Void o) {
                  return module.process(simpleInput.goal);
                }
                @Override
                public AbstractModuleOutput contract(ContractModule module, Void o) {
                  return module.process(simpleInput.goal);
                }
              }, null)),
          projectedInput -> new ProjectedInputOutput(projectedInput.module, projectedInput.goal,
              projectedInput.module.accept(new ProjectedModuleCases<AbstractModuleOutput, Void>() {
                @Override
                public AbstractModuleOutput simple(ProjectedSimpleModule module, Void aVoid) {
                  return module.process(projectedInput.goal);
                }
                @Override
                public AbstractModuleOutput contract(ProjectedContractModule module, Void aVoid) {
                  return module.process(projectedInput.goal);
                }
              }, null)));

  private static final Function<AbstractModuleOutput, List<TypeSpec>> nestedTypes =
      moduleOutputCases(
          simple -> singletonList(simple.impl),
          contract -> asList(contract.impl, contract.contract));

  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
