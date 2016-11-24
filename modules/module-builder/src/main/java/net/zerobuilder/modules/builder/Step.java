package net.zerobuilder.modules.builder;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoRegularGoal.SimpleRegularGoalContext;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;
import net.zerobuilder.compiler.generate.DtoSimpleGoal.SimpleGoal;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoParameter.parameterName;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.simpleGoalCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.nullCheck;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.builder.Builder.nextType;

final class Step {

  static IntFunction<TypeSpec> regularStepInterface(SimpleRegularGoalContext goal) {
    return i -> interfaceBuilder(upcase(goal.description().parameters().get(i).name))
        .addMethod(regularStepMethod(i, goal))
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec regularStepMethod(int i, SimpleRegularGoalContext goal) {
    SimpleParameter parameter = goal.description().parameters().get(i);
    String name = parameterName.apply(parameter);
    TypeName type = parameter.type;
    List<TypeName> thrownTypes = i == goal.description().parameters().size() ?
        goal.thrownTypes :
        Collections.emptyList();

    return methodBuilder(name)
        .returns(nextType(i, goal))
        .addParameter(parameterSpec(type, name))
        .addExceptions(thrownTypes)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  private static final Function<SimpleGoal, List<TypeName>> thrownTypes =
      simpleGoalCases(
          regular -> regular.thrownTypes,
          bean -> Collections.emptyList());

  static final Function<SimpleParameter, CodeBlock> nullCheck
      = parameter -> {
    if (!parameter.nullPolicy.check() || parameter.type.isPrimitive()) {
      return emptyCodeBlock;
    }
    String name = parameterName.apply(parameter);
    return nullCheck(name, name);
  };

  private Step() {
    throw new UnsupportedOperationException("no instances");
  }
}
