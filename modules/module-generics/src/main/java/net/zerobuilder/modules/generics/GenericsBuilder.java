package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ContractModuleOutput;

import java.util.Collections;
import java.util.List;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.context;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

public final class GenericsBuilder extends DtoModule.RegularContractModule {

  private List<TypeSpec> stepInterfaces(SimpleStaticMethodGoalContext goal) {
    return Collections.emptyList();
  }

  private TypeSpec defineContract(SimpleStaticMethodGoalContext goal) {
    return classBuilder(contractType(goal))
        .addTypes(stepInterfaces(goal))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructorBuilder()
            .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
            .addModifiers(PRIVATE)
            .build())
        .build();
  }

  ClassName contractType(SimpleStaticMethodGoalContext goal) {
    String contractName = upcase(goal.details.name) + upcase(name());
    return context.apply(goal)
        .generatedType.nestedClass(contractName);
  }

  private String name() {
    return "builder";
  }

  @Override
  protected ContractModuleOutput process(SimpleStaticMethodGoalContext goal) {
    return new ContractModuleOutput(
        null, null,
        defineContract(goal));
  }
}
