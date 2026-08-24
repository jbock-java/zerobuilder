package net.zerobuilder.modules.builder;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import io.jbock.simple.Inject;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.BuilderGoalDescription;

import java.util.List;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;

final class BuilderMethod {
  private final BuilderGoalDescription description;
  private final BuilderUtil util;

  @Inject
  BuilderMethod(BuilderGoalDescription description, BuilderUtil util) {
    this.description = description;
    this.util = util;
  }

  MethodSpec builderMethod() {
    List<TypeVariableName> typeVars = description.details().instanceTypeParameters();
    return methodBuilder("builder")
        .addTypeVariables(typeVars)
        .returns(parameterizedTypeName(util.stepType(0), typeVars))
        .addModifiers(description.details().access(STATIC))
        .addCode(returnRegular())
        .build();
  }

  private CodeBlock returnRegular() {
    TypeName typeName = parameterizedTypeName(
        util.implType(),
        description.details().instanceTypeParameters());
    return statement("return new $T()", typeName);
  }
}
