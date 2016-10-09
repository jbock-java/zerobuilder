package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Collections.emptyList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod.getMethod;
import static net.zerobuilder.compiler.generate.Utilities.constructor;
import static net.zerobuilder.compiler.generate.Utilities.transform;

public final class DtoGeneratorOutput {

  /**
   * Can be either a {@code builder} or {@code toBuilder} method
   */
  public static final class BuilderMethod {

    private final String name;
    private final MethodSpec method;

    BuilderMethod(String name, MethodSpec method) {
      this.name = name;
      this.method = method;
    }

    /**
     * Returns the name of the goal that generates this method.
     *
     * @return goal name
     */
    public String name() {
      return name;
    }

    public MethodSpec method() {
      return method;
    }

    static final Function<BuilderMethod, MethodSpec> getMethod
        = builderMethod -> builderMethod.method;
  }

  public static final class GeneratorOutput {

    private final List<BuilderMethod> methods;
    private final List<TypeSpec> nestedTypes;
    private final List<FieldSpec> fields;
    private final ClassName generatedType;

    /**
     * Create the definition of the generated class.
     *
     * @param generatedAnnotations annotations to add to the generated type, if any
     * @return type definition
     */
    public TypeSpec typeSpec(List<AnnotationSpec> generatedAnnotations) {
      return classBuilder(generatedType)
          .addFields(fields)
          .addMethod(constructor(PRIVATE))
          .addMethods(transform(methods, getMethod))
          .addAnnotations(generatedAnnotations)
          .addModifiers(PUBLIC, FINAL)
          .addTypes(nestedTypes)
          .build();
    }

    /**
     * Create the definition of the generated class.
     *
     * @return type definition
     */
    public TypeSpec typeSpec() {
      return typeSpec(emptyList());
    }

    GeneratorOutput(List<BuilderMethod> methods,
                    List<TypeSpec> nestedTypes,
                    List<FieldSpec> fields,
                    ClassName generatedType) {
      this.methods = methods;
      this.nestedTypes = nestedTypes;
      this.fields = fields;
      this.generatedType = generatedType;
    }

    /**
     * All methods in the type returned by {@link #typeSpec(List)}.
     * Includes static methods. Excludes constructors.
     *
     * @return list of methods
     */
    public List<BuilderMethod> methods() {
      return methods;
    }

    /**
     * All types that are nested directly inside the type returned by {@link #typeSpec(List)}.
     * Excludes non-static inner classes, local classes and anonymous classes.
     *
     * @return list of types
     */
    public List<TypeSpec> nestedTypes() {
      return nestedTypes;
    }

    /**
     * All fields of the type returned by {@link #typeSpec(List)}.
     * Includes static fields, if any.
     *
     * @return list of fields
     */
    public List<FieldSpec> fields() {
      return fields;
    }

    /**
     * Class name of the type returned by {@link #typeSpec(List)}.
     *
     * @return class name
     */
    public ClassName generatedType() {
      return generatedType;
    }
  }

  private DtoGeneratorOutput() {
    throw new UnsupportedOperationException("no instances");
  }
}
