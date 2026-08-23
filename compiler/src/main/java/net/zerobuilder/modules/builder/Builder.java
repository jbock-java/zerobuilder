package net.zerobuilder.modules.builder;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import io.jbock.simple.Inject;
import java.util.ArrayList;
import java.util.List;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.BuilderGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.modules.builder.RegularBuilder.stepInterfaceName;

final class Builder {
  private final BuilderGoalDescription description;

  @Inject
  Builder(BuilderGoalDescription description) {
    this.description = description;
  }

  TypeName nextType(int i) {
    if (i == description.parameters().size() - 1) {
      return description.details().goalType();
    }
    return description.context().generatedType()
        .nestedClass(stepInterfaceName(description.parameters().get(i + 1)));
  }

  List<FieldSpec> fields() {
    List<SimpleParameter> steps = description.parameters();
    List<FieldSpec> builder = new ArrayList<>(steps.size() + 2);
    steps.stream()
        .limit(steps.size() - 1)
        .map(parameter -> FieldSpec.builder(parameter.type(), parameter.name(), PRIVATE).build())
        .forEach(builder::add);
    return builder;
  }

  MethodSpec steps(int i) {
    SimpleParameter step = description.parameters().get(i);
    ParameterSpec parameter = parameterSpec(step.type(), step.name());
    List<TypeName> thrownTypes = i < description.parameters().size() - 1 ?
        List.of() :
        description.thrownTypes();
    TypeName nextType = nextType(i);
    return methodBuilder(step.name())
        .addAnnotation(Override.class)
        .addParameter(parameter)
        .returns(nextType)
        .addCode(normalAssignment(i))
        .addModifiers(PUBLIC)
        .addExceptions(thrownTypes)
        .build();
  }

  private CodeBlock normalAssignment(int i) {
    SimpleParameter step = description.parameters().get(i);
    ParameterSpec parameter = parameterSpec(step.type(), step.name());
    if (i == description.parameters().size() - 1) {
      return constructorCall();
    }
    return CodeBlock.builder()
        .addStatement("this.$N = $N", fieldSpec(step.type(), step.name()), parameter)
        .addStatement("return this")
        .build();
  }

  private CodeBlock constructorCall() {
    TypeName type = description.details().goalType();
    CodeBlock.Builder builder = CodeBlock.builder();
    CodeBlock args = description.invocationParameters();
    builder.addStatement("return new $T($L)", type, args);
    return builder.build();
  }
}
