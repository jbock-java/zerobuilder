package net.zerobuilder.modules.updater;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.UpdaterGoalDescription;
import net.zerobuilder.compiler.generate.GoalDetails;
import net.zerobuilder.compiler.generate.ModuleOutput;

import java.util.List;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static com.palantir.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.*;
import static net.zerobuilder.compiler.generate.ZeroUtil.*;
import static net.zerobuilder.modules.updater.UpdaterMethod.updaterMethod;

public final class RegularUpdater {

  private static final String MODULE_NAME = "Updater";

  private MethodSpec doneMethod(UpdaterGoalDescription description) {
    return methodBuilder("build")
        .addModifiers(description.details().getAccess())
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
        .addModifiers(description.details().getAccess(STATIC, FINAL))
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
    CodeBlock.Builder builder = CodeBlock.builder();
    return builder.addStatement("return new $T($L)", type,
            details.invocationParameters())
        .build();
  }

  public ModuleOutput process(UpdaterGoalDescription description) {
    return new ModuleOutput(
        updaterMethod(description),
        List.of(defineUpdater(description)));
  }
}
