package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.analyse.DtoShared;
import net.zerobuilder.compiler.generate.DtoStep.AbstractStep;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.DtoStep.declaredExceptions;
import static net.zerobuilder.compiler.generate.DtoStep.validParameter;

final class StepContextV {

  static final Function<AbstractStep, TypeSpec> regularStepInterface
      = new Function<AbstractStep, TypeSpec>() {
    @Override
    public TypeSpec apply(AbstractStep step) {
      DtoShared.ValidParameter parameter = step.accept(validParameter);
      String name = parameter.name;
      TypeName type = parameter.type;
      return interfaceBuilder(step.thisType)
          .addMethod(methodBuilder(name)
              .returns(step.nextType)
              .addParameter(parameterSpec(type, name))
              .addExceptions(step.accept(declaredExceptions))
              .addModifiers(PUBLIC, ABSTRACT)
              .build())
          .addModifiers(PUBLIC)
          .build();
    }
  };

  private StepContextV() {
    throw new UnsupportedOperationException("no instances");
  }
}
