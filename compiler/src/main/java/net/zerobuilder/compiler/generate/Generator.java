package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import net.zerobuilder.RecordBuilder;
import net.zerobuilder.modules.builder.BuilderComponent;
import net.zerobuilder.modules.updater.UpdaterComponent;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class Generator {

  /**
   * Entry point for code generation.
   *
   * @param goal inputs, may not be empty, must all have the same goal context
   * @return a GeneratorOutput
   * @throws IllegalArgumentException if input is invalid
   */
  public static GeneratorOutput generate(GoalDescription goal) {
    ModuleOutput moduleOutput = Generator.process(goal);
    return new GeneratorOutput(goal, moduleOutput);
  }

  private static ModuleOutput process(GoalDescription description) {
    RecordBuilder annotation = requireNonNull(description.details().tel().getAnnotation(RecordBuilder.class));
    List<MethodSpec> methods = new ArrayList<>();
    List<TypeSpec> typeSpecs = new ArrayList<>();
    if (!annotation.updateOnly()) {
      ModuleOutput builderOutput = BuilderComponent.process(description);
      methods.addAll(builderOutput.method());
      typeSpecs.addAll(builderOutput.typeSpecs());
    }
    if (!annotation.createOnly()) {
      ModuleOutput updaterOutput = UpdaterComponent.process(description);
      methods.addAll(updaterOutput.method());
      typeSpecs.addAll(updaterOutput.typeSpecs());
    }
    return new ModuleOutput(methods, typeSpecs);
  }

  private Generator() {
  }
}
