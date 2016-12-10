package net.zerobuilder.modules.updater;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput;
import net.zerobuilder.compiler.generate.DtoGoalDetails.InstanceMethodGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;

import java.util.HashSet;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.concat;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.modifiers;
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

  static DtoGeneratorOutput.BuilderMethod instanceGoalMethod(InstanceMethodGoalDetails details,
                                                             ProjectedRegularGoalDescription description) {
    TypeName factoryType = factoryType(details, description);
    ParameterSpec parameter = parameterSpec(description.context.type, "factory");
    MethodSpec method = methodBuilder(RegularUpdater.methodName(description) + "Factory")
        .addParameter(parameter)
        .addTypeVariables(new HashSet<>(concat(
            details.instanceTypeParameters,
            details.returnTypeParameters)))
        .returns(factoryType)
        .addCode(nullCheckingBlock(description))
        .addStatement("return new $T($N)", factoryType, parameter)
        .addModifiers(details.access(STATIC))
        .build();
    return new DtoGeneratorOutput.BuilderMethod(details.name, method);
  }

  private static TypeName factoryType(InstanceMethodGoalDetails details,
                                      ProjectedRegularGoalDescription description) {
    String implName = upcase(details.name) + upcase(moduleName) + "Factory";
    return parameterizedTypeName(
        description.context.generatedType.nestedClass(implName),
        details.instanceTypeParameters);
  }

  static TypeSpec factorySpec(InstanceMethodGoalDetails details,
                              ProjectedRegularGoalDescription description) {
    ParameterSpec updater = varUpdater(description);
    ParameterSpec factory = parameterSpec(description.context.type, "factory");
    return TypeSpec.classBuilder(simpleName(factoryType(details, description)))
        .addTypeVariables(details.instanceTypeParameters)
        .addField(fieldSpec(factory.type, "_factory", PRIVATE, FINAL))
        .addMethod(constructorBuilder()
            .addModifiers(PRIVATE)
            .addParameter(factory)
            .addStatement("this._factory = $N", factory)
            .build())
        .addMethod(methodBuilder("updater")
            .addExceptions(thrownByProjections(description))
            .addParameter(toBuilderParameter(description))
            .addTypeVariables(details.typeParameters)
            .returns(updater.type)
            .addCode(initVarUpdater(description, updater))
            .addStatement("$N._factory = this._factory", updater)
            .addCode(copyBlock(description))
            .addStatement("return $N", updater)
            .addModifiers(modifiers(details.access))
            .build())
        .addModifiers(PUBLIC, STATIC, FINAL)
        .build();
  }

  private InstanceWorld() {
    throw new UnsupportedOperationException("no instances");
  }
}
