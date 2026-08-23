package net.zerobuilder.modules.builder.bean;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AccessorPair;
import net.zerobuilder.compiler.generate.DtoBeanParameter.LoneGetter;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static com.palantir.javapoet.TypeSpec.interfaceBuilder;
import static com.palantir.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

final class BeanStep {

  private static final ClassName ITERABLE = ClassName.get(Iterable.class);

  static TypeSpec beanStepInterface(AbstractBeanParameter parameter, BeanGoalDescription description, int i) {
    return switch (parameter) {
      case AccessorPair accessorPair -> interfaceBuilder(upcase(accessorPair.name()))
          .addMethod(regularMethod(accessorPair, i, description))
          .addModifiers(PUBLIC)
          .build();
      case LoneGetter loneGetter -> interfaceBuilder(upcase(loneGetter.name()))
          .addMethod(iterateCollection(loneGetter, i, description))
          .addModifiers(PUBLIC)
          .build();
    };
  }

  private static MethodSpec regularMethod(AccessorPair step, int i, BeanGoalDescription description) {
    String name = step.name();
    TypeName type = step.type();
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
