package net.zerobuilder.modules.builder;

import com.palantir.javapoet.ClassName;
import net.zerobuilder.compiler.generate.DtoModule.BuilderModule;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.BuilderGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;
import net.zerobuilder.compiler.generate.ModuleOutput;

import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

public final class RegularBuilder implements BuilderModule {

  static final String MODULE_NAME = "Builder";

  static String stepInterfaceName(SimpleParameter parameter) {
    return upcase(parameter.name()) + "Step";
  }

  static ClassName implType(BuilderGoalDescription description) {
    ClassName contract = contractType(description);
    return description.context().generatedType().nestedClass(contract.simpleName() + "Impl");
  }

  static String methodName(BuilderGoalDescription description) {
    return description.details().name() + MODULE_NAME;
  }

  static ClassName contractType(BuilderGoalDescription description) {
    String contractName = upcase(description.details().name()) + MODULE_NAME;
    return description.context().generatedType().nestedClass(contractName);
  }

  @Override
  public ModuleOutput process(BuilderGoalDescription description) {
    return BuilderComponent_Impl.factory().create(description).createBuilderFactory().process();
  }
}
