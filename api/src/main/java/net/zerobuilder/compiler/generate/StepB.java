package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.AccessorPairStep;
import net.zerobuilder.compiler.generate.DtoBeanStep.BeanStepCases;
import net.zerobuilder.compiler.generate.DtoBeanStep.LoneGetterStep;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.Utilities.ClassNames.ITERABLE;
import static net.zerobuilder.compiler.generate.Utilities.parameterSpec;
import static net.zerobuilder.compiler.generate.DtoBeanStep.asFunction;
import static net.zerobuilder.compiler.generate.StepV.regularStepInterface;

final class StepB {

  static final Function<AbstractBeanStep, TypeSpec> beanStepInterface
      = asFunction(new BeanStepCases<TypeSpec>() {
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
  });

  private static List<MethodSpec> collectionMethods(LoneGetterStep step) {
    List<MethodSpec> builder = new ArrayList<>();
    builder.add(iterateCollection(step));
    builder.add(emptyCollection(step));
    return builder;
  }

  private static MethodSpec emptyCollection(LoneGetterStep step) {
    return methodBuilder(step.emptyMethod)
        .returns(step.nextType)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  private static MethodSpec iterateCollection(LoneGetterStep step) {
    String name = step.loneGetter.name();
    TypeName type = ParameterizedTypeName.get(ITERABLE,
        subtypeOf(step.loneGetter.iterationType()));
    return methodBuilder(name)
        .addParameter(parameterSpec(type, name))
        .returns(step.nextType)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  private StepB() {
    throw new UnsupportedOperationException("no instances");
  }
}
