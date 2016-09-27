package net.zerobuilder.compiler.generate;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.AccessorPairStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.BeanStepCases;
import net.zerobuilder.compiler.generate.DtoBeanStep.LoneGetterStep;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.Utilities.ClassNames.ITERABLE;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.analyse.DtoBeanParameter.beanParameterName;
import static net.zerobuilder.compiler.generate.StepContextV.regularStepInterface;

final class StepContextB {

  static final Function<AbstractBeanStep, TypeSpec> beanStepInterface
      = new Function<AbstractBeanStep, TypeSpec>() {
    @Override
    public TypeSpec apply(AbstractBeanStep step) {
      return step.acceptBean(beanStepInterfaceCases);
    }
  };

  private static final BeanStepCases<TypeSpec> beanStepInterfaceCases
      = new BeanStepCases<TypeSpec>() {
    @Override
    public TypeSpec accessorPair(AccessorPairStep step) {
      return regularStepInterface.apply(step);
    }
    @Override
    public TypeSpec loneGetter(LoneGetterStep step) {
      return interfaceBuilder(step.thisType)
          .addModifiers(PUBLIC)
          .addMethods(collectionMethods(step))
          .build();
    }
  };

  private static ImmutableList<MethodSpec> collectionMethods(LoneGetterStep step) {
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    builder.add(iterateCollection(step), emptyCollection(step));
    return builder.build();
  }

  private static MethodSpec emptyCollection(LoneGetterStep step) {
    return methodBuilder(step.emptyMethod)
        .returns(step.nextType)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  private static MethodSpec iterateCollection(LoneGetterStep step) {
    String name = step.loneGetter.accept(beanParameterName);
    TypeName type = ParameterizedTypeName.get(ITERABLE,
        subtypeOf(step.loneGetter.iterationType()));
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
