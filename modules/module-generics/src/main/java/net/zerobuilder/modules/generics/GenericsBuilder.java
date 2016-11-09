package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;
import net.zerobuilder.compiler.generate.DtoRegularStep.SimpleRegularStep;
import net.zerobuilder.compiler.generate.ZeroUtil;

import java.util.ArrayList;
import java.util.List;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.context;
import static net.zerobuilder.compiler.generate.DtoStep.AbstractStep.nextType;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.rawClassName;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.generics.VarLife.methodParams;
import static net.zerobuilder.modules.generics.VarLife.typeParams;
import static net.zerobuilder.modules.generics.VarLife.varLifes;

public final class GenericsBuilder extends DtoModule.RegularContractModule {

  private List<TypeSpec> stepInterfaces(SimpleStaticMethodGoalContext goal) {
    List<List<TypeVariableName>> lifes = varLifes(VarLife.typeVars(goal.details.typeParameters), stepTypes(goal));
    List<List<TypeVariableName>> typeParams = typeParams(lifes);
    List<List<TypeVariableName>> methodParams = methodParams(lifes);
    ArrayList<TypeSpec> builder = new ArrayList<>();
    for (int i = 0; i < goal.steps.size(); i++) {
      SimpleRegularStep step = goal.steps.get(i);
      builder.add(TypeSpec.interfaceBuilder(step.thisType)
          .addTypeVariables(typeParams.get(i))
          .addMethod(MethodSpec.methodBuilder(downcase(step.thisType))
              .addTypeVariables(methodParams.get(i))
              .addModifiers(PUBLIC, ABSTRACT)
              .returns(step.isLast() ?
                  nextType(step) :
                  ParameterizedTypeName.get(rawClassName(nextType(step)).get(),
                      methodParams.get(i).toArray(new TypeVariableName[0])))
              .addParameter(parameterSpec(step.parameter.type, step.parameter.name))
              .build())
          .build());
    }
    return builder;
  }

  private TypeSpec defineContract(SimpleStaticMethodGoalContext goal) {
    return classBuilder(contractType(goal))
        .addTypes(stepInterfaces(goal))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructorBuilder()
            .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
            .addModifiers(PRIVATE)
            .build())
        .build();
  }

  private TypeSpec defineImpl(SimpleStaticMethodGoalContext goal) {
    return TypeSpec.classBuilder(implType(goal)).build();
  }

  private ClassName contractType(SimpleStaticMethodGoalContext goal) {
    String contractName = upcase(goal.details.name) + "Builder";
    return context.apply(goal)
        .generatedType.nestedClass(contractName);
  }

  private ClassName implType(SimpleStaticMethodGoalContext goal) {
    String contractName = upcase(goal.details.name) + "BuilderImpl";
    return context.apply(goal)
        .generatedType.nestedClass(contractName);
  }

  private DtoGeneratorOutput.BuilderMethod builderMethod(SimpleStaticMethodGoalContext goal) {
    return new DtoGeneratorOutput.BuilderMethod(
        goal.details.name,
        MethodSpec.methodBuilder(goal.details.name + "Builder")
            .addModifiers(goal.details.access(STATIC))
            .build());
  }


  @Override
  protected ModuleOutput process(SimpleStaticMethodGoalContext goal) {
    return new ModuleOutput(
        builderMethod(goal),
        asList(defineImpl(goal), defineContract(goal)),
        emptyList());
  }

  private List<TypeName> stepTypes(SimpleStaticMethodGoalContext goal) {
    List<TypeName> builder = new ArrayList<>(goal.steps.size() + 1);
    goal.steps.stream().map(step -> step.parameter.type).forEach(builder::add);
    builder.add(goal.details.goalType);
    return builder;
  }

}
