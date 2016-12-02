package net.zerobuilder.modules.updater.bean;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.LoneGetter;
import net.zerobuilder.compiler.generate.DtoContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;

import javax.lang.model.element.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static java.util.Arrays.asList;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.NullPolicy.ALLOW;
import static net.zerobuilder.compiler.generate.DtoBeanParameter.beanParameterCases;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.flatList;
import static net.zerobuilder.compiler.generate.ZeroUtil.joinCodeBlocks;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.rawClassName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.modules.updater.bean.BeanUpdater.implType;

final class Generator {

  static BuilderMethod updaterMethod(BeanGoalContext goal) {
    String name = goal.details.name;
    ClassName type = goal.details.goalType;
    ParameterSpec varUpdater = updaterInstance(goal);
    Modifier[] modifiers = goal.details.access(STATIC);
    MethodSpec method = methodBuilder(downcase(name + "Updater"))
        .addParameter(parameterSpec(type, downcase(type.simpleName())))
        .returns(implType(goal))
        .addExceptions(thrownTypes(goal,
            asList(
                AbstractBeanParameter::getterThrownTypes,
                AbstractBeanParameter::setterThrownTypes)))
        .addCode(goal.description().parameters().stream().map(nullChecks(goal)).collect(joinCodeBlocks))
        .addCode(initVarUpdater(goal, varUpdater))
        .addCode(goal.description().parameters().stream().map(copy(goal)).collect(joinCodeBlocks))
        .addStatement("return $N", varUpdater)
        .addModifiers(modifiers)
        .build();
    return new BuilderMethod(name, method);
  }

  private static Set<TypeName> thrownTypes(BeanGoalContext goal,
                                           List<Function<AbstractBeanParameter, List<TypeName>>> functions) {
    Set<TypeName> thrownTypes = new HashSet<>();
    for (Function<AbstractBeanParameter, List<TypeName>> function : functions) {
      thrownTypes.addAll(goal.description().parameters().stream()
          .map(function)
          .collect(flatList()));
    }
    thrownTypes.addAll(goal.description().thrownTypes);
    return thrownTypes;
  }

  private static Function<AbstractBeanParameter, CodeBlock> copy(BeanGoalContext goal) {
    return beanParameterCases(
        accessorPair -> copyRegular(goal, accessorPair),
        loneGetter -> copyCollection(goal, loneGetter));
  }

  private static Function<AbstractBeanParameter, CodeBlock> nullChecks(BeanGoalContext goal) {
    return beanParameterCases(
        accessorPair -> accessorPair.nullPolicy == ALLOW
            ? emptyCodeBlock
            : nullCheck(goal, accessorPair),
        loneGetter -> nullCheck(goal, loneGetter));
  }

  private static CodeBlock copyCollection(BeanGoalContext goal, LoneGetter step) {
    ClassName type = goal.details.goalType;
    ParameterSpec parameter = parameterSpec(type, downcase(type.simpleName()));
    ParameterSpec iterationVar = step.iterationVar(parameter);
    return CodeBlock.builder()
        .beginControlFlow("for ($T $N : $N.$N())",
            iterationVar.type, iterationVar, parameter,
            step.getter)
        .addStatement("$N.$N.$N().add($N)", updaterInstance(goal),
            downcase(type.simpleName()),
            step.getter,
            iterationVar)
        .endControlFlow()
        .build();
  }

  private static CodeBlock copyRegular(BeanGoalContext goal, DtoBeanParameter.AccessorPair step) {
    ClassName type = goal.details.goalType;
    ParameterSpec parameter = parameterSpec(type, downcase(type.simpleName()));
    ParameterSpec updater = updaterInstance(goal);
    return CodeBlock.builder()
        .addStatement("$N.$N.$L($N.$N())", updater,
            goal.bean(),
            step.setterName(),
            parameter,
            step.getter)
        .build();
  }

  private static CodeBlock nullCheck(BeanGoalContext goal, AbstractBeanParameter beanParameter) {
    ClassName type = goal.details.goalType;
    ParameterSpec parameter = parameterSpec(type, downcase(type.simpleName()));
    return CodeBlock.builder()
        .beginControlFlow("if ($N.$N() == null)", parameter,
            beanParameter.getter)
        .addStatement("throw new $T($S)",
            NullPointerException.class, beanParameter.name())
        .endControlFlow().build();
  }

  private static CodeBlock initVarUpdater(BeanGoalContext goal, ParameterSpec varUpdater) {
    DtoContext.GoalContext context = goal.context;
    if (goal.isReuse()) {
      FieldSpec cache = context.cache(rawClassName(varUpdater.type));
      return CodeBlock.builder()
          .addStatement("$T $N = $N.get()", varUpdater.type, varUpdater, cache)
          .beginControlFlow("if ($N._currently_in_use)", varUpdater)
          .addStatement("$N.remove()", cache)
          .addStatement("$N = $N.get()", varUpdater, cache)
          .endControlFlow()
          .addStatement("$N.$N = new $T()", varUpdater, goal.bean(), goal.details.goalType)
          .addStatement("$N._currently_in_use = true", varUpdater)
          .build();
    }
    return statement("$T $N = new $T()", varUpdater.type, varUpdater, varUpdater.type);
  }

  private static ParameterSpec updaterInstance(BeanGoalContext goal) {
    TypeName updaterType = implType(goal);
    return parameterSpec(updaterType, "_updater");
  }

  private Generator() {
    throw new UnsupportedOperationException("no instances");
  }
}
