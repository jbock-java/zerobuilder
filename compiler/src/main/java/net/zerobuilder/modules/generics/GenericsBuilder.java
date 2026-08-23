package net.zerobuilder.modules.generics;

import com.palantir.javapoet.TypeVariableName;
import java.util.List;
import net.zerobuilder.compiler.generate.GoalDetails;
import net.zerobuilder.compiler.generate.DtoModule.BuilderModule;
import net.zerobuilder.compiler.generate.ModuleOutput;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.BuilderGoalDescription;

import static net.zerobuilder.modules.generics.GenericsContract.stepTypes;

public final class GenericsBuilder implements BuilderModule {

  @Override
  public ModuleOutput process(BuilderGoalDescription description) {
    GoalDetails details = description.details();
    List<TypeVariableName> typeParameters = details.instanceTypeParameters();
    VarLife varLife = VarLife.create(
        typeParameters,
        stepTypes(description));
    GenericsGenerator generator = GenericsGenerator.create(description, varLife);
    return new ModuleOutput(
        generator.builderMethod(description, varLife),
        List.of(generator.defineImpl()),
        List.of());
  }
}
