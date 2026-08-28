package net.zerobuilder.modules.builder;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import io.jbock.simple.Inject;
import net.zerobuilder.compiler.generate.GoalDescription;

import java.util.List;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;

record BuilderMethod(
    GoalDescription description,
    BuilderUtil util) {
  @Inject
  BuilderMethod {
  }

  MethodSpec builderMethod() {
    List<TypeVariableName> typeVars = description.details().instanceTypeParameters();
    return methodBuilder("builder")
        .addTypeVariables(typeVars)
        .returns(parameterizedTypeName(util.stepType(0), typeVars))
        .addModifiers(description.details().getAccess(STATIC))
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
