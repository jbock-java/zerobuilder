package net.zerobuilder.modules.builder.bean;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.builder.bean.BeanBuilder.implType;

final class Generator {

  static BuilderMethod builderMethod(BeanGoalDescription description) {
    String name = description.details.name;
    MethodSpec method = methodBuilder(BeanBuilder.methodName(description))
        .returns(BeanBuilder.contractType(description)
            .nestedClass(upcase(description.parameters.get(0).name())))
        .addModifiers(description.details.access(STATIC))
        .addExceptions(description.thrownTypes)
        .addCode(returnBuilder(description))
        .build();
    return new BuilderMethod(name, method);
  }

  private static CodeBlock returnBuilder(BeanGoalDescription description) {
    ClassName implType = implType(description);
    return statement("return new $T()", implType);
  }

  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
