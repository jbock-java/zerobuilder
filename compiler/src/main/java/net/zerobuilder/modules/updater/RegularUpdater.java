package net.zerobuilder.modules.updater;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import java.util.List;
import net.zerobuilder.compiler.generate.DtoModule.UpdaterModule;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.UpdaterGoalDescription;
import net.zerobuilder.compiler.generate.GoalDetails;
import net.zerobuilder.compiler.generate.ModuleOutput;

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
import static net.zerobuilder.modules.updater.UpdaterMethod.updaterMethod;

public final class RegularUpdater implements UpdaterModule {

  private static final String MODULE_NAME = "Updater";

  private MethodSpec doneMethod(UpdaterGoalDescription description) {
    return methodBuilder("build")
        .addModifiers(PUBLIC)
        .addExceptions(description.thrownTypes())
        .returns(description.details().goalType())
        .addCode(constructorCall(description.details()))
        .build();
  }

  private TypeSpec defineUpdater(UpdaterGoalDescription description) {
    return classBuilder(simpleName(implType(description)))
        .addFields(Updater.fields(description))
        .addMethods(Updater.stepMethods(description))
        .addTypeVariables(description.details().instanceTypeParameters())
        .addMethod(doneMethod(description))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructor(PRIVATE))
        .build();
  }

  static TypeName implType(UpdaterGoalDescription description) {
    return parameterizedTypeName(
        description.context().generatedType().nestedClass(implTypeName(description)),
        description.details().instanceTypeParameters());
  }

  private static String implTypeName(UpdaterGoalDescription description) {
    return upcase(description.details().name()) + MODULE_NAME;
  }

  private CodeBlock constructorCall(
      GoalDetails details) {
    TypeName type = details.goalType();
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(simpleName(type)));
    CodeBlock.Builder builder = CodeBlock.builder();
    return builder.addStatement("$T $N = new $T($L)", varGoal.type(), varGoal, type,
            details.invocationParameters())
        .addStatement("return $N", varGoal)
        .build();
  }

  static String methodName(UpdaterGoalDescription description) {
    return description.details().name() + MODULE_NAME;
  }

  @Override
  public ModuleOutput process(UpdaterGoalDescription description) {
    return new ModuleOutput(
        updaterMethod(description),
        List.of(defineUpdater(description)));
  }
}
