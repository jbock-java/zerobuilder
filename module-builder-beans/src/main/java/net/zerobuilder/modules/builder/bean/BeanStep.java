package net.zerobuilder.modules.builder.bean;

import io.jbock.javapoet.ClassName;
import io.jbock.javapoet.MethodSpec;
import io.jbock.javapoet.ParameterizedTypeName;
import io.jbock.javapoet.TypeName;
import io.jbock.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AccessorPair;
import net.zerobuilder.compiler.generate.DtoBeanParameter.LoneGetter;

import java.util.function.IntFunction;

import static io.jbock.javapoet.MethodSpec.methodBuilder;
import static io.jbock.javapoet.TypeSpec.interfaceBuilder;
import static io.jbock.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoBeanParameter.beanParameterCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

final class BeanStep {

  private static final ClassName ITERABLE = ClassName.get(Iterable.class);

  static IntFunction<TypeSpec> beanStepInterface(BeanGoalDescription description) {
    return i -> beanParameterCases(
        accessorPair -> interfaceBuilder(upcase(accessorPair.name()))
            .addMethod(regularMethod(accessorPair, i, description))
            .addModifiers(PUBLIC)
            .build(),
        loneGetter -> interfaceBuilder(upcase(loneGetter.name()))
            .addMethod(iterateCollection(loneGetter, i, description))
            .addModifiers(PUBLIC)
            .build()).apply(description.parameters.get(i));
  }

  private static MethodSpec regularMethod(AccessorPair step, int i, BeanGoalDescription description) {
    String name = step.name();
    TypeName type = step.type;
    return methodBuilder(name)
        .returns(nextType(i, description))
        .addParameter(parameterSpec(type, name))
        .addExceptions(step.setterThrownTypes)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  private static MethodSpec iterateCollection(LoneGetter step, int i, BeanGoalDescription description) {
    String name = step.name();
    TypeName type = ParameterizedTypeName.get(ITERABLE,
        subtypeOf(step.iterationType()));
    return methodBuilder(name)
        .addParameter(parameterSpec(type, name))
        .addExceptions(step.getterThrownTypes)
        .returns(nextType(i, description))
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  static TypeName nextType(int i, BeanGoalDescription description) {
    if (i < description.parameters.size() - 1) {
      return description.details.context.generatedType
          .nestedClass(upcase(description.details.name + "Builder"))
          .nestedClass(upcase(description.parameters.get(i + 1).name()));
    }
    return description.details.goalType;
  }
  
  private BeanStep() {
    throw new UnsupportedOperationException("no instances");
  }
}
