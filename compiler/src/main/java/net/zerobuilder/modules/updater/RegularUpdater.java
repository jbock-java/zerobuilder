package net.zerobuilder.modules.updater;

import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.jbock.simple.Inject;
import java.util.ArrayList;
import java.util.List;
import net.zerobuilder.compiler.generate.GoalDescription;
import net.zerobuilder.compiler.generate.GoalDetails;
import net.zerobuilder.compiler.generate.ModuleOutput;

import static com.palantir.javapoet.MethodSpec.methodBuilder;
import static com.palantir.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;

public record RegularUpdater(
    GoalDescription description,
    UpdaterMethod updaterMethod,
    Updater updater) {

  @Inject
  public RegularUpdater {
  }

  private MethodSpec buildMethod() {
    return methodBuilder("build")
        .addModifiers(description.details().getAccess())
        .returns(description.details().goalType())
        .addCode(constructorCall(description.details()))
        .build();
  }

  private TypeSpec defineUpdater() {
    return classBuilder(simpleName(updater.implType()))
        .addFields(updater.fields())
        .addMethods(updater.stepMethods())
        .addTypeVariables(description.details().instanceTypeParameters())
        .addMethod(buildMethod())
        .addModifiers(description.details().getAccess(STATIC, FINAL))
        .build();
  }

  private CodeBlock constructorCall(
      GoalDetails details) {
    TypeName type = details.goalType();
    CodeBlock.Builder builder = CodeBlock.builder();
    return builder.addStatement("return new $T($L)", type,
            description.invocationParameters())
        .build();
  }

  public ModuleOutput process() {
    List<MethodSpec> methods = new ArrayList<>(2);
    if (description.classicMode()) {
      methods.add(updaterMethod.nullaryMethod());
    }
    methods.add(updaterMethod.unaryMethod());
    return new ModuleOutput(
        methods,
        List.of(defineUpdater()));
  }
}
