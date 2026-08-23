package net.zerobuilder.modules.builder;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import io.jbock.simple.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
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
import static net.zerobuilder.modules.builder.RegularBuilder.contractType;
import static net.zerobuilder.modules.builder.RegularBuilder.implType;
import static net.zerobuilder.modules.builder.RegularBuilder.stepInterfaceName;

public class BuilderFactory {
  private final BuilderGoalDescription description;
  private final Builder builder;
  private final Step step;
  private final BuilderMethod builderMethod;

  @Inject
  BuilderFactory(
      Builder builder,
      BuilderGoalDescription description,
      Step step,
      BuilderMethod builderMethod) {
    this.builder = builder;
    this.description = description;
    this.step = step;
    this.builderMethod = builderMethod;
  }

  private List<TypeSpec> stepInterfaces() {
    return IntStream.range(0, description.parameters().size())
        .mapToObj(step::stepInterface)
        .toList();
  }

  private List<MethodSpec> steps() {
    return IntStream.range(0, description.parameters().size())
        .mapToObj(builder::steps)
        .toList();
  }

  private TypeSpec defineBuilderImpl() {
    return classBuilder(implType(description))
        .addSuperinterfaces(stepInterfaceTypes())
        .addFields(builder.fields())
        .addMethod(constructor())
        .addMethods(steps())
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build();
  }

  private TypeSpec defineContract() {
    return classBuilder(contractType(description))
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructorBuilder()
            .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
            .addModifiers(PRIVATE).build())
        .build();
  }

  private List<ClassName> stepInterfaceTypes() {
    return transform(description.parameters(),
        step -> description.context().generatedType().nestedClass(stepInterfaceName(step)));
  }

  ModuleOutput process() {
    List<TypeSpec> steps = stepInterfaces();
    List<TypeSpec> typeSpecs = new ArrayList<>(steps.size() + 2);
    typeSpecs.add(defineBuilderImpl());
    typeSpecs.add(defineContract());
    typeSpecs.addAll(steps);
    return new ModuleOutput(builderMethod.builderMethod(), typeSpecs);
  }
}
