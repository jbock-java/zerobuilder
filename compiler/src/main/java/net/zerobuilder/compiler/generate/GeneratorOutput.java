package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import net.zerobuilder.compiler.ZeroProcessor;

import javax.annotation.processing.Generated;
import java.util.List;
import java.util.Objects;

import static com.palantir.javapoet.MethodSpec.constructorBuilder;
import static com.palantir.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;

/**
 * @param methods       All methods in the type returned by {@link #typeSpec()}.
 *                      Includes static methods. Excludes constructors.
 * @param nestedTypes
 * @param generatedType Class name of the type returned by {@link #typeSpec()}.
 */
public record GeneratorOutput(
    GoalDetails detail,
    List<MethodSpec> methods,
    List<TypeSpec> nestedTypes,
    ClassName generatedType) {

  /**
   * Create the definition of the generated class.
   *
   * @return type definition
   */
  public TypeSpec typeSpec() {
    return classBuilder(generatedType())
        .addMethod(constructor())
        .addMethods(methods())
        .addAnnotation(generatedAnnotation())
        .addModifiers(detail.getAccess(FINAL))
        .addTypes(nestedTypes()).build();
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
