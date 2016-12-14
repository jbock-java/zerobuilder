package net.zerobuilder.modules.updater.bean;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AccessorPair;
import net.zerobuilder.compiler.generate.DtoBeanParameter.LoneGetter;
import net.zerobuilder.compiler.generate.NullPolicy;
import net.zerobuilder.compiler.generate.ZeroUtil;

import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoBeanParameter.beanParameterCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.modules.updater.bean.BeanUpdater.implType;

final class Updater {

  private static final ClassName ITERABLE = ClassName.get(Iterable.class);

  final static Function<BeanGoalDescription, List<FieldSpec>> fields =
      description -> singletonList(description.beanField);

  final static Function<BeanGoalDescription, List<MethodSpec>> stepMethods =
      description -> description.parameters.stream()
          .map(stepToMethods(description))
          .collect(toList());

  private static Function<AbstractBeanParameter, MethodSpec> stepToMethods(BeanGoalDescription description) {
    return beanParameterCases(
        accessorPair -> normalUpdate(accessorPair, description),
        loneGetter -> iterateCollection(description, loneGetter));
  }

  private static MethodSpec normalUpdate(AccessorPair step, BeanGoalDescription description) {
    String name = step.name();
    ParameterSpec parameter = parameterSpec(step.type, step.name());
    return methodBuilder(name)
        .returns(implType(description))
        .addExceptions(step.setterThrownTypes)
        .addParameter(parameter)
        .addCode(step.nullPolicy == NullPolicy.ALLOW ?
            emptyCodeBlock :
            CodeBlock.builder()
                .beginControlFlow("if ($N == null)", parameter)
                .addStatement("throw new $T($S)", NullPointerException.class, parameter.name)
                .endControlFlow().build())
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
        .addCode(ZeroUtil.nullCheck(name, name))
        .addCode(clearCollection(description, step))
        .beginControlFlow("for ($T $N : $N)", iterationVar.type, iterationVar, name)
        .addStatement("this.$N.$N().add($N)",
            description.beanField, step.getter, iterationVar)
        .endControlFlow()
        .addStatement("return this")
        .addModifiers(PUBLIC)
        .build();
  }

  private static CodeBlock clearCollection(BeanGoalDescription description, LoneGetter step) {
    return CodeBlock.builder().addStatement("this.$N.$N().clear()",
        description.beanField, step.getter).build();
  }

  private Updater() {
    throw new UnsupportedOperationException("no instances");
  }
}
