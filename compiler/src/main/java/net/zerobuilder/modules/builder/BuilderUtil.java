package net.zerobuilder.modules.builder;

import com.palantir.javapoet.ClassName;
import io.jbock.simple.Inject;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.BuilderGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter;

import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

final class BuilderUtil {
  static final String MODULE_NAME = "Builder";
  private final BuilderGoalDescription description;

  @Inject
  BuilderUtil(BuilderGoalDescription description) {
    this.description = description;
  }

  ClassName stepType(int i) {
    DtoRegularParameter.SimpleParameter parameter = description.parameters().get(i);
    return description.context().generatedType()
        .nestedClass(upcase(parameter.name()) + "Step");
  }

  ClassName implType() {
    String contractName = upcase(description.details().name()) + MODULE_NAME;
    return description.context().generatedType().nestedClass(contractName);
  }
}
