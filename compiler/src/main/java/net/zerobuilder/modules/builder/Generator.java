package net.zerobuilder.modules.builder;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import java.util.List;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.GoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.BuilderGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.builder.RegularBuilder.implType;

final class Generator {

  static BuilderMethod builderMethod(BuilderGoalDescription description) {
    GoalDetails goalDetails = description.details();
    List<SimpleParameter> steps = description.parameters();
    MethodSpec.Builder method = methodBuilder(RegularBuilder.methodName(description))
        .returns(RegularBuilder.contractType(description).nestedClass(upcase(steps.getFirst().name())))
        .addModifiers(goalDetails.access(STATIC));
    CodeBlock returnBlock = returnRegular(description);
    method.addCode(returnBlock);
    return new BuilderMethod(description.details().name(), method.build());
  }

  private static CodeBlock returnRegular(BuilderGoalDescription description) {
    ParameterSpec varBuilder = builderInstance(description);
    return statement("return new $T()", varBuilder.type());
  }

  private static ParameterSpec builderInstance(BuilderGoalDescription description) {
    return parameterSpec(implType(description), "_builder");
  }

  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
