package net.zerobuilder.modules.builder.bean;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoBeanGoal.BeanGoalContext;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod;
import net.zerobuilder.compiler.generate.DtoModule.BeanModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.ZeroUtil.emptyCodeBlock;
import static net.zerobuilder.compiler.generate.ZeroUtil.statement;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.builder.bean.BeanStep.beanStepInterface;

public final class BeanBuilder implements BeanModule {

  private static final String moduleName = "builder";

  private List<TypeSpec> stepInterfaces(BeanGoalContext goal) {
    return IntStream.range(0, goal.description().parameters().size())
        .mapToObj(beanStepInterface(goal))
        .collect(toList());
  }

  private final Function<BeanGoalContext, List<MethodSpec>> steps =
      Builder.steps;

  private final Function<BeanGoalContext, List<FieldSpec>> fields =
      Builder.fields;

  private final Function<BeanGoalContext, BuilderMethod> goalToBuilder =
      Generator::builderMethodB;

  static ClassName implType(BeanGoalContext goal) {
    ClassName contract = contractType(goal);
    return contract.peerClass(contract.simpleName() + "Impl");
  }

  static String methodName(BeanGoalContext goal) {
    return goal.details.name + upcase(moduleName);
  }

  private TypeSpec defineBuilderImpl(BeanGoalContext goal) {
    return classBuilder(implType(goal))
        .addSuperinterfaces(stepInterfaceTypes(goal))
        .addFields(fields.apply(goal))
        .addMethod(builderConstructor.apply(goal))
        .addMethods(steps.apply(goal))
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build();
  }

  private TypeSpec defineContract(BeanGoalContext goal) {
    return classBuilder(contractType(goal))
        .addTypes(stepInterfaces(goal))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructorBuilder()
            .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
            .addModifiers(PRIVATE)
            .build())
        .build();
  }

  private final Function<BeanGoalContext, MethodSpec> builderConstructor =
      bean -> constructorBuilder()
          .addExceptions(bean.context.lifecycle == REUSE_INSTANCES
              ? Collections.emptyList()
              : bean.description().thrownTypes)
          .addCode(bean.context.lifecycle == REUSE_INSTANCES
              ? emptyCodeBlock
              : statement("this.$N = new $T()", bean.bean(), bean.type()))
          .build();

  private List<ClassName> stepInterfaceTypes(BeanGoalContext goal) {
    return transform(goal.description().parameters(),
        step -> contractType(goal).nestedClass(upcase(step.name())));
  }

  static ClassName contractType(BeanGoalContext goal) {
    String contractName = upcase(goal.details.name) + upcase(moduleName);
    return goal.context.generatedType.nestedClass(contractName);
  }

  @Override
  public ModuleOutput process(BeanGoalContext goal) {
    return new ModuleOutput(
        goalToBuilder.apply(goal),
        asList(
            defineBuilderImpl(goal),
            defineContract(goal)),
        singletonList(goal.context.cache(implType(goal))));
  }
}
