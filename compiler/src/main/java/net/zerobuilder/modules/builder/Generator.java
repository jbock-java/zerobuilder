package net.zerobuilder.modules.builder;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import java.util.List;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.builder.RegularBuilder.implType;

final class Generator {

  static BuilderMethod builderMethod(SimpleRegularGoalDescription description) {
    AbstractRegularDetails abstractRegularDetails = description.details;
    List<SimpleParameter> steps = description.parameters;
    MethodSpec.Builder method = methodBuilder(RegularBuilder.methodName(description))
        .returns(RegularBuilder.contractType(description).nestedClass(upcase(steps.getFirst().name)))
        .addModifiers(abstractRegularDetails.access(STATIC));
    CodeBlock returnBlock = returnRegular(description);
    method.addCode(returnBlock);
    return new BuilderMethod(description.details.name(), method.build());
  }

  private static CodeBlock returnRegular(SimpleRegularGoalDescription description) {
    ParameterSpec varBuilder = builderInstance(description);
    return statement("return new $T()", varBuilder.type());
  }

  private static ParameterSpec builderInstance(SimpleRegularGoalDescription description) {
    return parameterSpec(implType(description), "_builder");
  }

  static FieldSpec instanceField(SimpleRegularGoalDescription description) {
    TypeName type = description.context.type;
    String name = '_' + downcase(simpleName(type));
    return fieldSpec(type, name, PRIVATE, FINAL);
  }

  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
