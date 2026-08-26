package net.zerobuilder.modules.builder;

import com.palantir.javapoet.ClassName;
import io.jbock.simple.Inject;
import net.zerobuilder.compiler.generate.GoalDescription;
import net.zerobuilder.compiler.generate.ProjectedParameter;

import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

final class BuilderUtil {

  private final GoalDescription description;

  @Inject
  BuilderUtil(GoalDescription description) {
    this.description = description;
  }

  ClassName stepType(int i) {
    ProjectedParameter parameter = description.parameters().get(i);
    return description.generatedType()
        .nestedClass(upcase(parameter.name()) + "Step");
  }

  ClassName implType() {
    String contractName = description.details().tel().getSimpleName() + "Builder";
    return description.generatedType().nestedClass(contractName);
  }
}
