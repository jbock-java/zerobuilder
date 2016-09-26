package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.analyse.DtoShared.ValidBeanParameter;
import net.zerobuilder.compiler.analyse.DtoShared.ValidBeanParameter.CollectionType;
import net.zerobuilder.compiler.generate.DtoStep.BeanStep;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorB.ITERABLE;
import static net.zerobuilder.compiler.generate.StepContextV.regularStepInterface;

final class StepContextB {

  static final Function<BeanStep, TypeSpec> beanStepInterface
      = new Function<BeanStep, TypeSpec>() {
    @Override
    public TypeSpec apply(BeanStep step) {
      CollectionType collectionType = step.validParameter.collectionType;
      if (collectionType.isPresent()) {
        return interfaceBuilder(step.thisType)
            .addModifiers(PUBLIC)
            .addMethods(collectionMethods(step))
            .build();
      } else {
        return regularStepInterface.apply(step);
      }
    }
  };

  private static ImmutableList<MethodSpec> collectionMethods(BeanStep step) {
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    builder.add(iterateCollection(step), emptyCollection(step));
    if (step.validParameter.collectionType.allowShortcut) {
      builder.add(singletonCollection(step));
    }
    return builder.build();
  }


  private static MethodSpec singletonCollection(BeanStep step) {
    String name = step.validParameter.name;
    TypeName type = step.validParameter.collectionType.getType();
    return methodBuilder(name)
        .addParameter(parameterSpec(type, name))
        .returns(step.nextType)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  private static MethodSpec emptyCollection(BeanStep step) {
    ValidBeanParameter parameter = step.validParameter;
    String name = parameter.name;
    return methodBuilder(name)
        .returns(step.nextType)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  private static MethodSpec iterateCollection(BeanStep step) {
    ValidBeanParameter parameter = step.validParameter;
    String name = parameter.name;
    TypeName type = ParameterizedTypeName.get(ITERABLE,
        subtypeOf(parameter.collectionType.getType()));
    return methodBuilder(name)
        .addParameter(parameterSpec(type, name))
        .returns(step.nextType)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  private StepContextB() {
    throw new UnsupportedOperationException("no instances");
  }
}
