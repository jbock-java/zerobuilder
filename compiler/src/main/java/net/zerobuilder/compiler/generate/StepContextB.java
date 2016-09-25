package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.analyse.DtoShared.ValidBeanParameter;
import net.zerobuilder.compiler.generate.DtoStep.BeanStep;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.StepContextV.regularStepInterface;

final class StepContextB {

  static final Function<BeanStep, TypeSpec> beanStepInterface
      = new Function<BeanStep, TypeSpec>() {
    @Override
    public TypeSpec apply(BeanStep step) {
      ValidBeanParameter parameter = step.validParameter;
      String name = parameter.name;
      if (parameter.collectionType.isPresent()) {
        TypeName collectionType = parameter.collectionType.get();
        ParameterizedTypeName iterable = ParameterizedTypeName.get(ClassName.get(Iterable.class),
            subtypeOf(collectionType));
        TypeSpec.Builder builder = interfaceBuilder(step.thisType)
            .addModifiers(PUBLIC)
            .addMethod(methodBuilder(name)
                .addParameter(parameterSpec(iterable, name))
                .returns(step.nextType)
                .addModifiers(PUBLIC, ABSTRACT)
                .build())
            .addMethod(methodBuilder(name)
                .returns(step.nextType)
                .addModifiers(PUBLIC, ABSTRACT)
                .build());
        if (parameter.collectionType.allowShortcut) {
          builder.addMethod(methodBuilder(name)
              .addParameter(parameterSpec(collectionType, name))
              .returns(step.nextType)
              .addModifiers(PUBLIC, ABSTRACT)
              .build());
        }
        return builder.build();
      } else {
        return regularStepInterface.apply(step);
      }
    }
  };

  private StepContextB() {
    throw new UnsupportedOperationException("no instances");
  }
}
