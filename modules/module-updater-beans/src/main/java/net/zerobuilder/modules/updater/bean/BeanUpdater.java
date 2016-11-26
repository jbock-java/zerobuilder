package net.zerobuilder.modules.updater.bean;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
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
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.rawClassName;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.updater.bean.Updater.fields;
import static net.zerobuilder.modules.updater.bean.Updater.stepMethods;

public final class BeanUpdater implements BeanModule {

  private static final String moduleName = "updater";

  private MethodSpec buildMethod(BeanGoalContext goal) {
    return methodBuilder("done")
        .addModifiers(PUBLIC)
        .returns(goal.details.type())
        .addCode(invoke.apply(goal))
        .build();
  }

  private TypeSpec defineUpdater(BeanGoalContext projectedGoal) {
    return classBuilder(rawClassName(implType(projectedGoal)).get())
        .addFields(fields.apply(projectedGoal))
        .addMethods(stepMethods.apply(projectedGoal))
        .addMethod(buildMethod(projectedGoal))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(updaterConstructor.apply(projectedGoal))
        .build();
  }

  static TypeName implType(BeanGoalContext goal) {
    String implName = upcase(goal.details.name) + upcase(moduleName);
    return goal.context.generatedType.nestedClass(implName);
  }

  private static final Function<BeanGoalContext, MethodSpec> updaterConstructor =
      bean -> constructorBuilder()
          .addModifiers(PRIVATE)
          .addExceptions(bean.mayReuse()
              ? emptyList()
              : bean.description().thrownTypes)
          .addCode(bean.mayReuse()
              ? emptyCodeBlock
              : statement("this.$N = new $T()", bean.bean(), bean.type()))
          .build();

  private CodeBlock returnBean(BeanGoalContext goal) {
    ClassName type = goal.details.goalType;
    ParameterSpec varGoal = parameterSpec(type,
        '_' + downcase(type.simpleName()));
    CodeBlock.Builder builder = CodeBlock.builder();
    if (goal.mayReuse()) {
      builder.addStatement("this._currently_in_use = false");
    }
    builder.addStatement("$T $N = this.$N", varGoal.type, varGoal, goal.bean());
    if (goal.mayReuse()) {
      builder.addStatement("this.$N = null", goal.bean());
    }
    return builder.addStatement("return $N", varGoal).build();
  }

  private final Function<BeanGoalContext, CodeBlock> invoke =
      this::returnBean;

  static FieldSpec cacheField(BeanGoalContext projectedGoal) {
    TypeName type = implType(projectedGoal);
    return FieldSpec.builder(type, downcase(rawClassName(type).get().simpleName()), PRIVATE)
        .initializer("new $T()", type)
        .build();
  }

  @Override
  public ModuleOutput process(BeanGoalContext goal) {
    return new ModuleOutput(
        Generator.updaterMethod(goal),
        singletonList(defineUpdater(goal)),
        singletonList(cacheField(goal)));
  }
}
