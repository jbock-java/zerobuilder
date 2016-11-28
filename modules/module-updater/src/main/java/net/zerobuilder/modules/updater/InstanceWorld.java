package net.zerobuilder.modules.updater;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoalDetails;
import net.zerobuilder.compiler.generate.DtoProjectedRegularGoalContext.ProjectedInstanceMethodGoalContext;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.updater.Generator.copyBlock;
import static net.zerobuilder.modules.updater.Generator.initVarUpdater;
import static net.zerobuilder.modules.updater.Generator.nullCheckingBlock;
import static net.zerobuilder.modules.updater.Generator.thrownByProjections;
import static net.zerobuilder.modules.updater.Generator.toBuilderParameter;
import static net.zerobuilder.modules.updater.Generator.varUpdater;
import static net.zerobuilder.modules.updater.RegularUpdater.moduleName;

final class InstanceWorld {

  static DtoGeneratorOutput.BuilderMethod instanceGoalMethod(ProjectedInstanceMethodGoalContext goal) {
    DtoGoalDetails.AbstractRegularDetails details = goal.details();
    TypeName factoryType = factoryType(goal);
    ParameterSpec parameter = parameterSpec(goal.context.type, "factory");
    MethodSpec method = methodBuilder(RegularUpdater.methodName(goal))
        .addParameter(parameter)
        .addTypeVariables(goal.instanceTypeParameters())
        .returns(factoryType)
        .addCode(nullCheckingBlock(goal))
        .addStatement("return new $T($N)", factoryType, parameter)
        .addModifiers(details.access(STATIC))
        .build();
    return new DtoGeneratorOutput.BuilderMethod(details.name, method);
  }

  private static TypeName factoryType(ProjectedInstanceMethodGoalContext goal) {
    String implName = upcase(goal.description().details().name()) + upcase(moduleName) + "Factory";
    return parameterizedTypeName(
        goal.context().generatedType.nestedClass(implName),
        goal.instanceTypeParameters());
  }

  static TypeSpec factorySpec(ProjectedInstanceMethodGoalContext goal) {
    ParameterSpec updater = varUpdater(goal);
    ParameterSpec factory = parameterSpec(goal.context.type, "factory");
    return TypeSpec.classBuilder(simpleName(factoryType(goal)))
        .addTypeVariables(goal.instanceTypeParameters())
        .addField(fieldSpec(factory.type, "_factory", PRIVATE, FINAL))
        .addMethod(constructorBuilder()
            .addModifiers(PRIVATE)
            .addParameter(factory)
            .addStatement("this._factory = $N", factory)
            .build())
        .addMethod(methodBuilder("updater")
            .addExceptions(thrownByProjections(goal))
            .addParameter(toBuilderParameter(goal))
            .returns(updater.type)
            .addCode(initVarUpdater(goal, updater))
            .addStatement("$N._factory = this._factory", updater)
            .addCode(copyBlock(goal))
            .addStatement("return $N", updater)
            .addModifiers(goal.details.access())
            .build())
        .addModifiers(PUBLIC, STATIC, FINAL)
        .build();
  }

  private InstanceWorld() {
    throw new UnsupportedOperationException("no instances");
  }
}
