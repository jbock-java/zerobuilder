package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import java.util.Objects;
import javax.annotation.processing.Generated;
import net.zerobuilder.compiler.ZeroProcessor;

import static com.palantir.javapoet.MethodSpec.constructorBuilder;
import static com.palantir.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;

public record GeneratorOutput(
    GoalDescription description,
    ModuleOutput moduleOutput) {

  /**
   * Create the definition of the generated class.
   *
   * @return type definition
   */
  public TypeSpec typeSpec() {
    return classBuilder(description.generatedType())
        .addMethod(constructor())
        .addMethods(moduleOutput.method())
        .addAnnotation(generatedAnnotation())
        .addModifiers(description.details().getAccess(FINAL))
        .addTypes(moduleOutput.typeSpecs()).build();
  }

  private MethodSpec constructor() {
    return constructorBuilder()
        .addModifiers(PRIVATE)
        .build();
  }

  private AnnotationSpec generatedAnnotation() {
    String version = Objects.toString(getClass().getPackage().getImplementationVersion(), "");
    String value = ZeroProcessor.class.getName() + (version.isEmpty() ? "" : " " + version);
    return AnnotationSpec.builder(Generated.class)
        .addMember("value", "$S", value)
        .addMember("comments", "$S", "https://github.com/jbock-java/zerobuilder")
        .build();
  }
}
