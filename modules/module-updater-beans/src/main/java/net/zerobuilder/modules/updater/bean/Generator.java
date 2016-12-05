package net.zerobuilder.modules.updater.bean;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.LoneGetter;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;

import javax.lang.model.element.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static java.util.Arrays.asList;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoBeanParameter.beanParameterCases;
import static net.zerobuilder.compiler.generate.NullPolicy.ALLOW;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.flatList;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.modules.updater.bean.BeanUpdater.implType;

final class Generator {

  static BuilderMethod updaterMethod(BeanGoalDescription description) {
    String name = description.details.name;
    ClassName type = description.details.goalType;
    ParameterSpec varUpdater = updaterInstance(description);
    Modifier[] modifiers = description.details.access(STATIC);
    MethodSpec method = methodBuilder(downcase(name + "Updater"))
        .addParameter(parameterSpec(type, downcase(type.simpleName())))
        .returns(implType(description))
        .addExceptions(thrownTypes(description,
            asList(
                AbstractBeanParameter::getterThrownTypes,
                AbstractBeanParameter::setterThrownTypes)))
        .addCode(description.parameters.stream().map(nullChecks(description)).collect(joinCodeBlocks))
        .addCode(initVarUpdater(varUpdater))
        .addCode(description.parameters.stream().map(copy(description)).collect(joinCodeBlocks))
        .addStatement("return $N", varUpdater)
        .addModifiers(modifiers)
        .build();
    return new BuilderMethod(name, method);
  }

  private static Set<TypeName> thrownTypes(BeanGoalDescription description,
                                           List<Function<AbstractBeanParameter, List<TypeName>>> functions) {
    Set<TypeName> thrownTypes = new HashSet<>();
    for (Function<AbstractBeanParameter, List<TypeName>> function : functions) {
      thrownTypes.addAll(description.parameters.stream()
          .map(function)
          .collect(flatList()));
    }
    thrownTypes.addAll(description.thrownTypes);
    return thrownTypes;
  }

  private static Function<AbstractBeanParameter, CodeBlock> copy(BeanGoalDescription description) {
    return beanParameterCases(
        accessorPair -> copyRegular(description, accessorPair),
        loneGetter -> copyCollection(description, loneGetter));
  }

  private static Function<AbstractBeanParameter, CodeBlock> nullChecks(BeanGoalDescription description) {
    return beanParameterCases(
        accessorPair -> accessorPair.nullPolicy == ALLOW
            ? emptyCodeBlock
            : nullCheck(description, accessorPair),
        loneGetter -> nullCheck(description, loneGetter));
  }

  private static CodeBlock copyCollection(BeanGoalDescription description, LoneGetter step) {
    ClassName type = description.details.goalType;
    ParameterSpec parameter = parameterSpec(type, downcase(type.simpleName()));
    ParameterSpec iterationVar = step.iterationVar(parameter);
    return CodeBlock.builder()
        .beginControlFlow("for ($T $N : $N.$N())",
            iterationVar.type, iterationVar, parameter,
            step.getter)
        .addStatement("$N.$N.$N().add($N)", updaterInstance(description),
            downcase(type.simpleName()),
            step.getter,
            iterationVar)
        .endControlFlow()
        .build();
  }

  private static CodeBlock copyRegular(BeanGoalDescription description, DtoBeanParameter.AccessorPair step) {
    ClassName type = description.details.goalType;
    ParameterSpec parameter = parameterSpec(type, downcase(type.simpleName()));
    ParameterSpec updater = updaterInstance(description);
    return CodeBlock.builder()
        .addStatement("$N.$N.$L($N.$N())", updater,
            description.beanField,
            step.setterName(),
            parameter,
            step.getter)
        .build();
  }

  private static CodeBlock nullCheck(BeanGoalDescription description, AbstractBeanParameter beanParameter) {
    ClassName type = description.details.goalType;
    ParameterSpec parameter = parameterSpec(type, downcase(type.simpleName()));
    return CodeBlock.builder()
        .beginControlFlow("if ($N.$N() == null)", parameter,
            beanParameter.getter)
        .addStatement("throw new $T($S)",
            NullPointerException.class, beanParameter.name())
        .endControlFlow().build();
  }

  private static CodeBlock initVarUpdater(ParameterSpec varUpdater) {
    return statement("$T $N = new $T()", varUpdater.type, varUpdater, varUpdater.type);
  }

  private static ParameterSpec updaterInstance(BeanGoalDescription description) {
    TypeName updaterType = implType(description);
    return parameterSpec(updaterType, "_updater");
  }

  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
