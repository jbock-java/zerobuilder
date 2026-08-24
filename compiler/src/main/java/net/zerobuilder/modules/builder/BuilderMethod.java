package net.zerobuilder.modules.builder;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import io.jbock.simple.Inject;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.BuilderGoalDescription;
import net.zerobuilder.compiler.generate.GoalDetails;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.modules.builder.BuilderUtil.MODULE_NAME;

final class BuilderMethod {
  private final BuilderGoalDescription description;
  private final BuilderUtil util;

  @Inject
  BuilderMethod(BuilderGoalDescription description, BuilderUtil util) {
    this.description = description;
    this.util = util;
  }

  private String methodName() {
    return description.details().name() + MODULE_NAME;
  }

  MethodSpec builderMethod() {
    GoalDetails goalDetails = description.details();
    return methodBuilder(methodName())
        .returns(util.stepType(0))
        .addModifiers(goalDetails.access(STATIC))
        .addCode(returnRegular())
        .build();
  }

  private CodeBlock returnRegular() {
    return statement("return new $T()", util.implType());
  }
}
