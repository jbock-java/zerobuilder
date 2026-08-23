package net.zerobuilder.modules.builder;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import java.util.List;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.BuilderGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;
import net.zerobuilder.compiler.generate.GoalDetails;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.builder.RegularBuilder.implType;

final class BuilderMethod {

  static MethodSpec builderMethod(BuilderGoalDescription description) {
    GoalDetails goalDetails = description.details();
    List<SimpleParameter> steps = description.parameters();
    return methodBuilder(RegularBuilder.methodName(description))
        .returns(description.context().generatedType().nestedClass(upcase(steps.getFirst().name())))
        .addModifiers(goalDetails.access(STATIC))
        .addCode(returnRegular(description))
        .build();
  }

  private static CodeBlock returnRegular(BuilderGoalDescription description) {
    ParameterSpec varBuilder = builderInstance(description);
    return statement("return new $T()", varBuilder.type());
  }

  private static ParameterSpec builderInstance(BuilderGoalDescription description) {
    return parameterSpec(implType(description), "_builder");
  }

  private BuilderMethod() {
    throw new UnsupportedOperationException("no instances");
  }
}
