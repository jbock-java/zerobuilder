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

public class BuilderFactory {
  private final BuilderGoalDescription description;
  private final Builder builder;
  private final BuilderUtil util;
  private final Step step;
  private final BuilderMethod builderMethod;

  @Inject
  BuilderFactory(
      Builder builder,
      BuilderGoalDescription description,
      BuilderUtil util,
      Step step,
      BuilderMethod builderMethod) {
    this.builder = builder;
    this.description = description;
    this.util = util;
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
    return classBuilder(util.implType())
        .addSuperinterfaces(stepInterfaceTypes())
        .addFields(builder.fields())
        .addMethod(constructor())
        .addMethods(steps())
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build();
  }

  private TypeSpec defineContract() {
    return classBuilder(util.contractType())
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addMethod(constructorBuilder()
            .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
            .addModifiers(PRIVATE).build())
        .build();
  }

  private List<ClassName> stepInterfaceTypes() {
    List<ClassName> result = new ArrayList<>();
    for (int i = 0; i < description.parameters().size(); i++) {
      result.add(description.context()
          .generatedType()
          .nestedClass(util.stepInterfaceName(i)));
    }
    return result;
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
