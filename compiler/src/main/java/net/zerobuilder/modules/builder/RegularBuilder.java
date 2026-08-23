package net.zerobuilder.modules.builder;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import net.zerobuilder.compiler.generate.DtoModule.BuilderModule;
import net.zerobuilder.compiler.generate.DtoRegularGoalDescription.BuilderGoalDescription;
import net.zerobuilder.compiler.generate.ModuleOutput;

import static com.palantir.javapoet.MethodSpec.constructorBuilder;
import static com.palantir.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.constructor;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;
import static net.zerobuilder.compiler.generate.ZeroUtil.upcase;
import static net.zerobuilder.modules.builder.Builder.fields;
import static net.zerobuilder.modules.builder.BuilderMethod.builderMethod;
import static net.zerobuilder.modules.builder.BuilderMethod.stepInterfaceName;
import static net.zerobuilder.modules.builder.Step.stepInterface;

public final class RegularBuilder implements BuilderModule {

  private static final String MODULE_NAME = "Builder";

  private List<TypeSpec> stepInterfaces(BuilderGoalDescription description) {
    return IntStream.range(0, description.parameters().size())
        .mapToObj(i -> stepInterface(description, i))
        .toList();
  }

  private List<MethodSpec> steps(BuilderGoalDescription description) {
    return IntStream.range(0, description.parameters().size())
        .mapToObj(i -> Builder.steps(description, i))
        .toList();
  }

  static ClassName implType(BuilderGoalDescription description) {
    ClassName contract = contractType(description);
    return description.context().generatedType().nestedClass(contract.simpleName() + "Impl");
  }

  static String methodName(BuilderGoalDescription description) {
    return description.details().name() + MODULE_NAME;
  }

  private TypeSpec defineBuilderImpl(BuilderGoalDescription description) {
    return classBuilder(implType(description))
        .addSuperinterfaces(stepInterfaceTypes(description))
        .addFields(fields(description))
        .addMethod(constructor())
        .addMethods(steps(description))
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build();
  }

  private TypeSpec defineContract(BuilderGoalDescription description) {
    return classBuilder(contractType(description))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructorBuilder()
            .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
            .addModifiers(PRIVATE).build())
        .build();
  }

  private List<ClassName> stepInterfaceTypes(BuilderGoalDescription description) {
    return transform(description.parameters(),
        step -> description.context().generatedType().nestedClass(stepInterfaceName(step)));
  }

  static ClassName contractType(BuilderGoalDescription description) {
    String contractName = upcase(description.details().name()) + MODULE_NAME;
    return description.context().generatedType().nestedClass(contractName);
  }

  @Override
  public ModuleOutput process(BuilderGoalDescription description) {
    List<TypeSpec> steps = stepInterfaces(description);
    List<TypeSpec> typeSpecs = new ArrayList<>(steps.size() + 2);
    typeSpecs.add(defineBuilderImpl(description));
    typeSpecs.add(defineContract(description));
    typeSpecs.addAll(steps);
    return new ModuleOutput(builderMethod(description), typeSpecs);
  }
}
