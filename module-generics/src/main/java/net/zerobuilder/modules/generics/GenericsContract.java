package net.zerobuilder.modules.generics;

import io.jbock.javapoet.ClassName;
import io.jbock.javapoet.TypeName;
import io.jbock.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import java.util.ArrayList;
import java.util.List;

import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

final class GenericsContract {

  static TypeName nextStepType(SimpleRegularGoalDescription description,
                               List<List<TypeVariableName>> typeParams,
                               int i) {
    if (i == description.parameters.size() - 1) {
      return description.details.type();
    }
    List<SimpleParameter> steps = description.parameters;
    SimpleParameter step = steps.get(i + 1);
    ClassName rawNext = description.context.generatedType
        .nestedClass(upcase(description.details.name() + "Builder"))
        .nestedClass(upcase(step.name));
    return parameterizedTypeName(rawNext, typeParams.get(i + 1));
  }

  static ClassName implType(SimpleRegularGoalDescription description) {
    String contractName = upcase(description.details.name) + "Builder";
    return description.context
        .generatedType.nestedClass(contractName);
  }

  static List<TypeName> stepTypes(SimpleRegularGoalDescription description) {
    List<TypeName> builder = new ArrayList<>(description.parameters.size() + 1);
    description.parameters.stream().map(step -> step.type)
        .forEach(builder::add);
    builder.add(description.details.type());
    return builder;
  }
}
