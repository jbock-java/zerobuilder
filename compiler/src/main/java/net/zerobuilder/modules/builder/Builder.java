package net.zerobuilder.modules.builder;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.BuilderGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static java.util.Collections.emptyList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

final class Builder {

  static TypeName nextType(int i, BuilderGoalDescription description) {
    if (i < description.parameters().size() - 1) {
      return description.context().generatedType()
          .nestedClass(upcase(description.details().name() + "Builder"))
          .nestedClass(upcase(description.parameters().get(i + 1).name()));
    }
    return description.details().goalType();
  }

  static final Function<BuilderGoalDescription, List<FieldSpec>> fields
      = description -> {
    List<SimpleParameter> steps = description.parameters();
    ArrayList<FieldSpec> builder = new ArrayList<>(steps.size() + 2);
    steps.stream()
        .limit(steps.size() - 1)
        .map(parameter -> fieldSpec(parameter.type(), parameter.name(), PRIVATE))
        .forEach(builder::add);
    return builder;
  };

  static MethodSpec steps(BuilderGoalDescription description, int i) {
    SimpleParameter step = description.parameters().get(i);
    TypeName type = step.type();
    String name = step.name();
    ParameterSpec parameter = parameterSpec(type, name);
    List<TypeName> thrownTypes = i < description.parameters().size() - 1 ?
        emptyList() :
        description.thrownTypes();
    TypeName nextType = nextType(i, description);
    return methodBuilder(step.name())
        .addAnnotation(Override.class)
        .addParameter(parameter)
        .returns(nextType)
        .addCode(normalAssignment(i, description))
        .addModifiers(PUBLIC)
        .addExceptions(thrownTypes)
        .build();

  }

  private static CodeBlock normalAssignment(int i, BuilderGoalDescription description) {
    SimpleParameter step = description.parameters().get(i);
    TypeName type = step.type();
    String name = step.name();
    ParameterSpec parameter = parameterSpec(type, name);
    if (i == description.parameters().size() - 1) {
      return constructorCall(description);
    } else {
      return CodeBlock.builder()
          .addStatement("this.$N = $N", fieldSpec(step.type(), step.name()), parameter)
          .addStatement("return this")
          .build();
    }
  }

  private static CodeBlock constructorCall(
      BuilderGoalDescription description) {
    TypeName type = description.details().goalType();
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    CodeBlock args = description.invocationParameters();
    builder.addStatement("$T $N = new $T($L)", varGoal.type(), varGoal, type, args);
    return builder.addStatement("return $N", varGoal).build();
  }

  private Builder() {
    throw new UnsupportedOperationException("no instances");
  }
}
