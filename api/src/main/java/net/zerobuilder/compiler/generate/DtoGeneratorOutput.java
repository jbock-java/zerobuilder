package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.util.List;
import java.util.function.Function;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Collections.emptyList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static net.zerobuilder.compiler.generate.DtoGeneratorOutput.BuilderMethod.getMethod;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;

public final class DtoGeneratorOutput {

  /**
   * Can be either a {@code builder} or {@code updater} method
   */
  public static final class BuilderMethod {

    private final String name;
    private final MethodSpec method;

    public BuilderMethod(String name, MethodSpec method) {
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

    final List<BuilderMethod> methods;
    final List<TypeSpec> nestedTypes;
    final List<FieldSpec> fields;
    public final ClassName generatedType;

    private GeneratorOutput(List<BuilderMethod> methods, List<TypeSpec> nestedTypes, List<FieldSpec> fields,
                            ClassName generatedType) {
      this.methods = methods;
      this.nestedTypes = nestedTypes;
      this.fields = fields;
      this.generatedType = generatedType;
    }

    static GeneratorOutput create(List<BuilderMethod> methods, List<TypeSpec> nestedTypes, List<FieldSpec> fields,
                                  DtoContext.GoalContext context) {
      return new GeneratorOutput(methods, nestedTypes, fields, context.generatedType);
    }


    /**
     * Create the definition of the generated class.
     *
     * @param generatedAnnotations annotations to add to the generated type, if any
     * @return type definition
     */
    public TypeSpec typeSpec(List<AnnotationSpec> generatedAnnotations) {
      return classBuilder(generatedType)
          .addFields(fields)
          .addMethod(constructor())
          .addMethods(transform(methods(), getMethod))
          .addAnnotations(generatedAnnotations)
          .addModifiers(PUBLIC, FINAL)
          .addTypes(nestedTypes)
          .build();
    }

    private MethodSpec constructor() {
      return constructorBuilder()
          .addStatement("throw new $T($S)", UnsupportedOperationException.class, "no instances")
          .addModifiers(PRIVATE)
          .build();
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
     * Create the definition of the generated class.
     *
     * @return type definition
     */
    public TypeSpec typeSpec() {
      return typeSpec(emptyList());
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
