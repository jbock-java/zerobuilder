package net.zerobuilder.modules.builder.bean;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.builder.bean.BeanBuilder.implType;

final class Generator {

  static BuilderMethod builderMethod(BeanGoalContext goal) {
    String name = goal.details.name;
    MethodSpec method = methodBuilder(BeanBuilder.methodName(goal))
        .returns(BeanBuilder.contractType(goal).nestedClass(upcase(goal.description().parameters().get(0).name())))
        .addModifiers(goal.details.access(STATIC))
        .addExceptions(goal.description().thrownTypes)
        .addCode(returnBuilder(goal))
        .build();
    return new BuilderMethod(name, method);
  }

  private static CodeBlock returnBuilder(BeanGoalContext goal) {
    ClassName implType = implType(goal);
    return statement("return new $T()", implType);
  }

  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
