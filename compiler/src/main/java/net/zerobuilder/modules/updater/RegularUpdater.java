package net.zerobuilder.modules.updater;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;
import java.util.List;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractRegularDetails;
import net.zerobuilder.compiler.generate.DtoModule.ProjectedModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.ProjectedRegularGoalDescription;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static com.palantir.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.constructor;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.updater.Generator.goalMethod;

public final class RegularUpdater implements ProjectedModule {

  static final String moduleName = "updater";

  private MethodSpec doneMethod(ProjectedRegularGoalDescription description) {
    return methodBuilder("build")
        .addModifiers(PUBLIC)
        .addExceptions(description.thrownTypes)
        .returns(description.details.type())
        .addCode(constructorCall(description.details))
        .build();
  }

  private TypeSpec defineUpdater(ProjectedRegularGoalDescription description) {
    return classBuilder(simpleName(implType(description)))
        .addFields(Updater.fields(description))
        .addMethods(Updater.stepMethods(description))
        .addTypeVariables(implTypeParameters(description.details))
        .addMethod(doneMethod(description))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructor(PRIVATE))
        .build();
  }

  static TypeName implType(ProjectedRegularGoalDescription description) {
    return parameterizedTypeName(
        description.context.generatedType.nestedClass(implTypeName(description)),
        implTypeParameters(description.details));
  }

  private static String implTypeName(ProjectedRegularGoalDescription description) {
    return upcase(description.details.name()) + upcase(moduleName);
  }

  private static List<TypeVariableName> implTypeParameters(AbstractRegularDetails details) {
    return details.instanceTypeParameters;
  }

  private CodeBlock constructorCall(
      AbstractRegularDetails details) {
    TypeName type = details.goalType;
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    return builder.addStatement("$T $N = new $T($L)", varGoal.type(), varGoal, type,
            details.invocationParameters())
        .addStatement("return $N", varGoal)
        .build();
  }

  static String methodName(ProjectedRegularGoalDescription description) {
    return description.details.name() + upcase(moduleName);
  }

  @Override
  public ModuleOutput process(ProjectedRegularGoalDescription description) {
    return new ModuleOutput(
        goalMethod(description),
        List.of(defineUpdater(description)),
        List.of());
  }
}
