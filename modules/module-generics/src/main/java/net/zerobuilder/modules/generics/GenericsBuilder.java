package net.zerobuilder.modules.generics;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.generate.DtoGeneratorOutput;
import net.zerobuilder.compiler.generate.DtoMethodGoal.SimpleStaticMethodGoalContext;
import net.zerobuilder.compiler.generate.DtoModule;
import net.zerobuilder.compiler.generate.DtoModuleOutput.ModuleOutput;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.DtoSimpleGoal.context;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;

public final class GenericsBuilder extends DtoModule.RegularContractModule {

  private static TypeSpec defineImpl(SimpleStaticMethodGoalContext goal) {
    return TypeSpec.classBuilder(implType(goal)).build();
  }

  private static ClassName implType(SimpleStaticMethodGoalContext goal) {
    String contractName = upcase(goal.details.name) + "BuilderImpl";
    return context.apply(goal)
        .generatedType.nestedClass(contractName);
  }

  private static DtoGeneratorOutput.BuilderMethod builderMethod(SimpleStaticMethodGoalContext goal) {
    return new DtoGeneratorOutput.BuilderMethod(
        goal.details.name,
        MethodSpec.methodBuilder(goal.details.name + "Builder")
            .addModifiers(goal.details.access(STATIC))
            .build());
  }

  @Override
  protected ModuleOutput process(SimpleStaticMethodGoalContext goal) {
    GenericsGenerator generator = GenericsGenerator.create(goal);
    return new ModuleOutput(
        builderMethod(goal),
        asList(defineImpl(goal), generator.defineContract()),
        emptyList());
  }
}
