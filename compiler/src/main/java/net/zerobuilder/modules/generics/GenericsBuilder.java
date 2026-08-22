package net.zerobuilder.modules.generics;

import com.palantir.javapoet.TypeVariableName;
import java.util.List;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoModule.RegularSimpleModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import static net.zerobuilder.modules.generics.GenericsContract.stepTypes;

public final class GenericsBuilder implements RegularSimpleModule {

  @Override
  public ModuleOutput process(SimpleRegularGoalDescription description) {
    AbstractRegularDetails details = description.details;
    List<TypeVariableName> typeParameters = details.instanceTypeParameters;
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
