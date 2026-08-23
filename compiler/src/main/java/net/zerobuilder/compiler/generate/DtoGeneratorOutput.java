package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import java.util.List;

import static com.palantir.javapoet.MethodSpec.constructorBuilder;
import static com.palantir.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;

public final class DtoGeneratorOutput {

  /**
   * Can be either a {@code builder} or {@code updater} method
   *
   * @param name the name of the goal that generates this method
   */
  public record BuilderMethod(
      String name,
      MethodSpec method) {
  }

  /**
   * @param methods       All methods in the type returned by {@link #typeSpec(List)}.
   *                      Includes static methods. Excludes constructors.
   * @param nestedTypes
   * @param fields
   * @param generatedType Class name of the type returned by {@link #typeSpec(List)}.
   */
  public record GeneratorOutput(
      List<BuilderMethod> methods,
      List<TypeSpec> nestedTypes,
      List<FieldSpec> fields,
      ClassName generatedType) {

    /**
     * Create the definition of the generated class.
     *
     * @param generatedAnnotations annotations to add to the generated type, if any
     * @return type definition
     */
    public TypeSpec typeSpec(List<AnnotationSpec> generatedAnnotations) {
      return classBuilder(generatedType()).addFields(fields()).addMethod(constructor()).addMethods(transform(methods(), BuilderMethod::method)).addAnnotations(generatedAnnotations).addModifiers(PUBLIC, FINAL).addTypes(nestedTypes()).build();
    }

    private MethodSpec constructor() {
      return constructorBuilder().addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances").addModifiers(PRIVATE).build();
    }

    /**
     * Create the definition of the generated class.
     *
     * @return type definition
     */
    public TypeSpec typeSpec() {
      return typeSpec(List.of());
    }

  }

  private DtoGeneratorOutput() {
    throw new UnsupportedOperationException("no instances");
  }
}
