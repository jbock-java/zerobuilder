package net.zerobuilder.modules.generics;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.List;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.BuilderGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.AbstractParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

final class GenericsContract {

  static TypeName nextStepType(
      BuilderGoalDescription description,
      List<List<TypeVariableName>> typeParams,
      int i) {
    if (i == description.parameters().size() - 1) {
      return description.details().goalType();
    }
    List<SimpleParameter> steps = description.parameters();
    SimpleParameter step = steps.get(i + 1);
    ClassName rawNext = description.context().generatedType()
        .nestedClass(upcase(description.details().name() + "Builder"))
        .nestedClass(upcase(step.name()));
    return parameterizedTypeName(rawNext, typeParams.get(i + 1));
  }

  static ClassName implType(BuilderGoalDescription description) {
    String contractName = upcase(description.details().name()) + "Builder";
    return description.context()
        .generatedType().nestedClass(contractName);
  }

  static List<TypeName> stepTypes(BuilderGoalDescription description) {
    List<TypeName> builder = new ArrayList<>(description.parameters().size() + 1);
    description.parameters().stream().map(AbstractParameter::type)
        .forEach(builder::add);
    builder.add(description.details().goalType());
    return builder;
  }
}
