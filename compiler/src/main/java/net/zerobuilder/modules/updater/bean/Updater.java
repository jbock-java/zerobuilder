package net.zerobuilder.modules.updater.bean;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AccessorPair;
import net.zerobuilder.compiler.generate.DtoBeanParameter.LoneGetter;

import java.util.List;
import java.util.function.Function;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static com.palantir.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.modules.updater.bean.BeanUpdater.implType;

final class Updater {

  private static final ClassName ITERABLE = ClassName.get(Iterable.class);

  final static Function<BeanGoalDescription, List<FieldSpec>> fields =
      description -> List.of(description.beanField);

  final static Function<BeanGoalDescription, List<MethodSpec>> stepMethods =
      description -> description.parameters.stream()
          .map(parameter -> stepToMethods(parameter, description))
          .toList();

  private static MethodSpec stepToMethods(AbstractBeanParameter parameter, BeanGoalDescription description) {
    return switch (parameter) {
      case AccessorPair accessorPair -> normalUpdate(accessorPair, description);
      case LoneGetter loneGetter -> iterateCollection(description, loneGetter);
    };
  }

  private static MethodSpec normalUpdate(AccessorPair step, BeanGoalDescription description) {
    String name = step.name();
    ParameterSpec parameter = parameterSpec(step.type(), step.name());
    return methodBuilder(name)
        .returns(implType(description))
        .addExceptions(step.setterThrownTypes)
        .addParameter(parameter)
        .addStatement("this.$N.$L($N)",
            description.beanField, step.setterName(), parameter)
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private static MethodSpec iterateCollection(BeanGoalDescription description, LoneGetter step) {
    ParameterizedTypeName iterable = ParameterizedTypeName.get(ITERABLE,
        subtypeOf(step.iterationType()));
    String name = step.name();
    ParameterSpec parameter = parameterSpec(iterable, name);
    ParameterSpec iterationVar = step.iterationVar(parameter);
    return methodBuilder(name)
        .returns(implType(description))
        .addParameter(parameter)
        .addExceptions(step.getterThrownTypes)
        .addCode(clearCollection(description, step))
        .beginControlFlow("for ($T $N : $N)", iterationVar.type(), iterationVar, name)
        .addStatement("this.$N.$N().add($N)",
            description.beanField, step.getter(), iterationVar)
        .endControlFlow()
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private static CodeBlock clearCollection(BeanGoalDescription description, LoneGetter step) {
    return CodeBlock.builder().addStatement("this.$N.$N().clear()",
        description.beanField, step.getter()).build();
  }

  private Updater() {
    throw new UnsupportedOperationException("no instances");
  }
}
