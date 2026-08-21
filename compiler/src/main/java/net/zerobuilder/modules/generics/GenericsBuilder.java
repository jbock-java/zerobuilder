package net.zerobuilder.modules.generics;

import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import java.util.List;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.ConstructorGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.InstanceMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoGoalDetails.StaticMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoModule.RegularSimpleModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import static net.zerobuilder.compiler.generate.DtoGoalDetails.isInstance;
import static net.zerobuilder.compiler.generate.ZeroUtil.concat;
import static net.zerobuilder.compiler.generate.ZeroUtil.cons;
import static net.zerobuilder.modules.generics.GenericsContract.stepTypes;

public final class GenericsBuilder implements RegularSimpleModule {

  private static List<TypeVariableName> allTypeParameters(AbstractRegularDetails details) {
    return switch (details) {
      case ConstructorGoalDetails constructor -> constructor.instanceTypeParameters;
      case StaticMethodGoalDetails staticMethod -> staticMethod.typeParameters;
      case InstanceMethodGoalDetails instanceMethod -> concat(
          instanceMethod.instanceTypeParameters, instanceMethod.typeParameters);
    };
  }

  private static List<TypeName> extendedStepTypes(AbstractRegularDetails details, SimpleRegularGoalDescription description) {
    return switch (details) {
      case ConstructorGoalDetails constructor -> stepTypes(description);
      case StaticMethodGoalDetails staticMethod -> stepTypes(description);
      case InstanceMethodGoalDetails instanceMethod -> cons(
              description.context.type,
              stepTypes(description));
    };
  }

  @Override
  public ModuleOutput process(SimpleRegularGoalDescription description) {
    AbstractRegularDetails details = description.details;
    List<TypeVariableName> typeParameters = allTypeParameters(details);
    VarLife varLife = VarLife.create(
        typeParameters,
        extendedStepTypes(description.details, description),
        isInstance(details));
    GenericsGenerator generator = GenericsGenerator.create(description, varLife);
    return new ModuleOutput(
        generator.builderMethod(description, varLife),
        List.of(generator.defineImpl()),
        List.of());
  }
}
