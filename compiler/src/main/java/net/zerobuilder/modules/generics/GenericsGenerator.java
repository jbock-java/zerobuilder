package net.zerobuilder.modules.generics;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;
import java.util.List;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.SimpleRegularGoalDescription;

import static com.palantir.javapoet.MethodSpec.constructorBuilder;
import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static com.palantir.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.generics.GenericsContract.implType;

final class GenericsGenerator {

  private final ClassName contractType;
  private final List<TypeSpec> stepImpls;
  private final SimpleRegularGoalDescription description;

  private GenericsGenerator(
      SimpleRegularGoalDescription description,
      ClassName contractType,
      List<TypeSpec> stepImpls) {
    this.contractType = contractType;
    this.description = description;
    this.stepImpls = stepImpls;
  }

  TypeSpec defineImpl() {
    return classBuilder(contractType)
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addFields(firstStepCache())
        .addTypes(stepImpls)
        .addMethod(constructorBuilder()
            .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
            .addModifiers(PRIVATE)
            .build())
        .build();
  }

  private List<FieldSpec> firstStepCache() {
    ClassName firstImplType = contractType.nestedClass(stepImpls.getFirst().name());
    return List.of(FieldSpec.builder(firstImplType,
            downcase(firstImplType.simpleName()), PRIVATE, STATIC, FINAL)
        .initializer("new $T()", firstImplType).build());
  }

  DtoGeneratorOutput.BuilderMethod builderMethod(
      SimpleRegularGoalDescription description,
      VarLife life) {
    List<List<TypeVariableName>> typeParams = life.typeParams();
    MethodSpec.Builder builder = methodBuilder(description.details.name() + "Builder")
        .addModifiers(description.details.access(STATIC))
        .returns(parameterizedTypeName(
            contractType.nestedClass(upcase(description.parameters.getFirst().name)),
            typeParams.getFirst()));
    builder.addTypeVariables(typeParams.getFirst());
    builder.addCode(statement("return $T.$L", contractType, downcase(stepImpls.getFirst().name())));
    return new DtoGeneratorOutput.BuilderMethod(
        description.details.name(),
        builder.build());
  }

  static GenericsGenerator create(SimpleRegularGoalDescription description, VarLife lifes) {
    List<List<TypeVariableName>> typeParams = lifes.typeParams();
    List<List<TypeVariableName>> methodParams = lifes.methodParams();
    ClassName contractType = implType(description);
    GenericsImpl genericsImpl = new GenericsImpl(contractType, description);
    List<TypeSpec> stepImpls = genericsImpl.stepImpls(methodParams, typeParams);
    return new GenericsGenerator(description, contractType, stepImpls);
  }
}
