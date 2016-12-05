package net.zerobuilder.modules.updater.bean;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoalDescription.BeanGoalDescription;
import net.zerobuilder.compiler.generate.DtoModule.BeanModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;

import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.downcase;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.simpleName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.updater.bean.Updater.fields;
import static net.zerobuilder.modules.updater.bean.Updater.stepMethods;

public final class BeanUpdater implements BeanModule {

  private static final String moduleName = "updater";

  private MethodSpec buildMethod(BeanGoalDescription description) {
    return methodBuilder("done")
        .addModifiers(PUBLIC)
        .returns(description.details.type())
        .addCode(returnBean(description))
        .build();
  }

  private TypeSpec defineUpdater(BeanGoalDescription description) {
    return classBuilder(simpleName(implType(description)))
        .addFields(fields.apply(description))
        .addMethods(stepMethods.apply(description))
        .addMethod(buildMethod(description))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(updaterConstructor.apply(description))
        .build();
  }

  static ClassName implType(BeanGoalDescription description) {
    String implName = upcase(description.details.name) + upcase(moduleName);
    return description.details.context.generatedType.nestedClass(implName);
  }

  private static final Function<BeanGoalDescription, MethodSpec> updaterConstructor =
      description -> constructorBuilder()
          .addModifiers(PRIVATE)
          .addExceptions(description.thrownTypes)
          .addCode(statement("this.$N = new $T()",
              description.beanField, description.details.goalType))
          .build();

  private CodeBlock returnBean(BeanGoalDescription description) {
    ClassName type = description.details.goalType;
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(type.simpleName()));
    CodeBlock.Builder builder = CodeBlock.builder();
    builder.addStatement("$T $N = this.$N",
        varGoal.type, varGoal, description.beanField);
    return builder.addStatement("return $N", varGoal).build();
  }

  @Override
  public ModuleOutput process(BeanGoalDescription description) {
    return new ModuleOutput(
        Generator.updaterMethod(description),
        singletonList(defineUpdater(description)),
        emptyList());
  }
}
