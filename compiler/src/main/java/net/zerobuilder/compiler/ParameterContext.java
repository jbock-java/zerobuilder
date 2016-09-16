package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.ToBuilderValidator.ValidParameter;

import javax.lang.model.element.Modifier;
import java.util.Set;

import static com.google.common.collect.Iterables.toArray;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * <p>method, constructor goal: parameter</p>
 * <p>field goal: setter</p>
 */
abstract class ParameterContext {

  /**
   * Type of this step
   */
  final ClassName typeName;

  /**
   * Type of the next step
   */
  final TypeName returnType;

  abstract static class ParameterCases<R> {
    abstract R regularParameter(ClassName typeName, TypeName returnType, ValidParameter.Parameter parameter);
    abstract R beansParameter(ClassName typeName, TypeName returnType, ValidParameter.AccessorPair parameter);
  }

  abstract <R> R accept(ParameterCases<R> cases);

  ParameterContext(ClassName typeName, TypeName returnType) {
    this.typeName = typeName;
    this.returnType = returnType;
  }

  final static class RegularParameterContext extends ParameterContext {
    final ValidParameter.Parameter parameter;
    RegularParameterContext(ClassName typeName, TypeName returnType, ValidParameter.Parameter parameter) {
      super(typeName, returnType);
      this.parameter = parameter;
    }
    @Override
    <R> R accept(ParameterCases<R> cases) {
      return cases.regularParameter(typeName, returnType, parameter);
    }
  }

  final static class BeansParameterContext extends ParameterContext {
    final ValidParameter.AccessorPair parameter;
    BeansParameterContext(ClassName typeName, TypeName returnType, ValidParameter.AccessorPair parameter) {
      super(typeName, returnType);
      this.parameter = parameter;
    }
    @Override
    <R> R accept(ParameterCases<R> cases) {
      return cases.beansParameter(typeName, returnType, parameter);
    }
  }

  abstract static class ParameterFunction<R> {
    abstract R apply(ClassName typeName, TypeName returnType, ValidParameter parameter);
  }

  static <R> ParameterCases<R> parameterCasesFunction(final ParameterFunction<R> parameterFunction) {
    return new ParameterCases<R>() {
      @Override
      R regularParameter(ClassName typeName, TypeName returnType, ValidParameter.Parameter parameter) {
        return parameterFunction.apply(typeName, returnType, parameter);
      }
      @Override
      R beansParameter(ClassName typeName, TypeName returnType, ValidParameter.AccessorPair parameter) {
        return parameterFunction.apply(typeName, returnType, parameter);
      }
    };
  }

  static ParameterCases<TypeSpec> asStepInterface(final Set<Modifier> modifiers, final ImmutableList<TypeName> declaredExceptions) {
    return parameterCasesFunction(new ParameterFunction<TypeSpec>() {
      @Override
      TypeSpec apply(ClassName typeName, TypeName returnType, ValidParameter parameter) {
        String name = parameter.name;
        TypeName type = parameter.type;
        MethodSpec methodSpec = methodBuilder(name)
            .returns(returnType)
            .addParameter(ParameterSpec.builder(type, name).build())
            .addExceptions(declaredExceptions)
            .addModifiers(PUBLIC, ABSTRACT)
            .build();
        return interfaceBuilder(typeName)
            .addMethod(methodSpec)
            .addModifiers(toArray(modifiers, Modifier.class))
            .build();
      }
    });
  }

  static ParameterCases<TypeSpec> asStepInterface(Set<Modifier> modifiers) {
    return asStepInterface(modifiers, ImmutableList.<TypeName>of());
  }

}
