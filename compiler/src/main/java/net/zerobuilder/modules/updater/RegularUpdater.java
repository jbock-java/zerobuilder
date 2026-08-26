package net.zerobuilder.modules.updater;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.jbock.simple.Inject;
import java.util.List;
import net.zerobuilder.compiler.generate.GoalDescription;
import net.zerobuilder.compiler.generate.GoalDetails;
import net.zerobuilder.compiler.generate.ModuleOutput;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static com.palantir.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

public final class RegularUpdater {
  private static final String MODULE_NAME = "Updater";

  private final GoalDescription description;
  private final UpdaterMethod updaterMethod;

  @Inject
  RegularUpdater(GoalDescription description, UpdaterMethod updaterMethod) {
    this.description = description;
    this.updaterMethod = updaterMethod;
  }

  private MethodSpec doneMethod() {
    return methodBuilder("build")
        .addModifiers(description.details().getAccess())
        .addExceptions(description.thrownTypes())
        .returns(description.details().goalType())
        .addCode(constructorCall(description.details()))
        .build();
  }

  private TypeSpec defineUpdater() {
    return classBuilder(simpleName(implType(description)))
        .addFields(Updater.fields(description))
        .addMethods(Updater.stepMethods(description))
        .addTypeVariables(description.details().instanceTypeParameters())
        .addMethod(doneMethod())
        .addModifiers(description.details().getAccess(STATIC, FINAL))
        .build();
  }

  static TypeName implType(GoalDescription description) {
    return parameterizedTypeName(
        description.context().generatedType().nestedClass(implTypeName(description)),
        description.details().instanceTypeParameters());
  }

  private static String implTypeName(GoalDescription description) {
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

  public ModuleOutput process() {
    return new ModuleOutput(
        List.of(updaterMethod.updaterMethod()),
        List.of(defineUpdater()));
  }
}
