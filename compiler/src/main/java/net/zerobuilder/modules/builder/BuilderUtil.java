package net.zerobuilder.modules.builder;

import com.palantir.javapoet.ClassName;
import io.jbock.simple.Inject;
import net.zerobuilder.compiler.generate.GoalDescription;
import net.zerobuilder.compiler.generate.ProjectedParameter;

import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

record BuilderUtil(GoalDescription description) {

  @Inject
  BuilderUtil {
  }

  ClassName stepType(int i) {
    ProjectedParameter parameter = description.parameters().get(i);
    return description.generatedType()
        .nestedClass(upcase(parameter.stepName()) + "Step");
  }

  ClassName implType() {
    String contractName = description.details().tel().getSimpleName() + "Builder";
    return description.generatedType().nestedClass(contractName);
  }
}
