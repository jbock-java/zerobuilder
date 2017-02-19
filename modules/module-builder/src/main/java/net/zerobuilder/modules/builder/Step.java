package net.zerobuilder.modules.builder;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.builder.Builder.nextType;

final class Step {

  static IntFunction<TypeSpec> stepInterface(SimpleRegularGoalDescription description) {
    return i -> interfaceBuilder(upcase(description.parameters.get(i).name))
        .addMethod(stepMethod(i, description))
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec stepMethod(int i, SimpleRegularGoalDescription description) {
    SimpleParameter parameter = description.parameters.get(i);
    String name = parameter.name;
    TypeName type = parameter.type;
    List<TypeName> thrownTypes = i == description.parameters.size() - 1 ?
        description.thrownTypes :
        Collections.emptyList();
    return methodBuilder(name)
        .returns(nextType(i, description))
        .addParameter(parameterSpec(type, name))
        .addExceptions(thrownTypes)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  private Step() {
    throw new UnsupportedOperationException("no instances");
  }
}
