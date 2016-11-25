package net.zerobuilder.modules.builder.bean;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AccessorPair;
import net.zerobuilder.compiler.generate.DtoBeanParameter.LoneGetter;

import java.util.function.IntFunction;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoBeanParameter.beanParameterCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.ClassNames.ITERABLE;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

final class BeanStep {

  static IntFunction<TypeSpec> beanStepInterface(BeanGoalContext goal) {
    return i -> beanParameterCases(
        accessorPair -> interfaceBuilder(upcase(accessorPair.name()))
            .addMethod(regularMethod(accessorPair, i, goal))
            .addModifiers(PUBLIC)
            .build(),
        loneGetter -> interfaceBuilder(upcase(loneGetter.name()))
            .addMethod(iterateCollection(loneGetter, i, goal))
            .addModifiers(PUBLIC)
            .build()).apply(goal.description().parameters().get(i));
  }

  private static MethodSpec regularMethod(AccessorPair step, int i, BeanGoalContext goal) {
    String name = step.name();
    TypeName type = step.type;
    return methodBuilder(name)
        .returns(nextType(i, goal))
        .addParameter(parameterSpec(type, name))
        .addExceptions(step.setterThrownTypes)
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  private static MethodSpec iterateCollection(LoneGetter step, int i, BeanGoalContext goal) {
    String name = step.name();
    TypeName type = ParameterizedTypeName.get(ITERABLE,
        subtypeOf(step.iterationType()));
    return methodBuilder(name)
        .addParameter(parameterSpec(type, name))
        .addExceptions(step.getterThrownTypes)
        .returns(nextType(i, goal))
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
  }

  static TypeName nextType(int i, BeanGoalContext goal) {
    if (i < goal.description().parameters().size() - 1) {
      return goal.context.generatedType
          .nestedClass(upcase(goal.details.name() + "Builder"))
          .nestedClass(upcase(goal.description().parameters().get(i + 1).name()));
    }
    return goal.details.type();
  }


  private BeanStep() {
    throw new UnsupportedOperationException("no instances");
  }
}
