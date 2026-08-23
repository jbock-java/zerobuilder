package net.zerobuilder.modules.builder;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import io.jbock.simple.Inject;
import java.util.List;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.BuilderGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;
import net.zerobuilder.compiler.generate.GoalDetails;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.modules.builder.RegularBuilder.implType;
import static net.zerobuilder.modules.builder.RegularBuilder.stepInterfaceName;

final class BuilderMethod {
  private final BuilderGoalDescription description;

  @Inject
  BuilderMethod(BuilderGoalDescription description) {
    this.description = description;
  }

  MethodSpec builderMethod() {
    GoalDetails goalDetails = description.details();
    List<SimpleParameter> steps = description.parameters();
    return methodBuilder(RegularBuilder.methodName(description))
        .returns(description.context().generatedType().nestedClass(stepInterfaceName(steps.getFirst())))
        .addModifiers(goalDetails.access(STATIC))
        .addCode(returnRegular())
        .build();
  }

  private CodeBlock returnRegular() {
    ParameterSpec varBuilder = builderInstance();
    return statement("return new $T()", varBuilder.type());
  }

  private ParameterSpec builderInstance() {
    return parameterSpec(implType(description), "_builder");
  }
}
