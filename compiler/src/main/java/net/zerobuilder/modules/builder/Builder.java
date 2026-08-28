package net.zerobuilder.modules.builder;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import io.jbock.simple.Inject;
import net.zerobuilder.compiler.generate.GoalDescription;
import net.zerobuilder.compiler.generate.ProjectedParameter;

import java.util.List;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;

record Builder(
    GoalDescription description,
    BuilderUtil util) {

  @Inject
  Builder {
  }

  TypeName nextType(int i) {
    if (i == description.parameters().size() - 1) {
      return description.details().goalType();
    }
    return parameterizedTypeName(
        util.stepType(i + 1),
        description.details().instanceTypeParameters());
  }

  List<FieldSpec> fields() {
    List<ProjectedParameter> steps = description.parameters();
    return steps.stream().limit(steps.size() - 1)
        .map(parameter -> FieldSpec.builder(parameter.type(), parameter.name(), PRIVATE).build())
        .toList();
  }

  MethodSpec steps(int i) {
    ProjectedParameter step = description.parameters().get(i);
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
    ProjectedParameter step = description.parameters().get(i);
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
    CodeBlock args = description.invocationParameters();
    return CodeBlock.builder()
        .addStatement("return new $T($L)", type, args)
        .build();
  }
}
