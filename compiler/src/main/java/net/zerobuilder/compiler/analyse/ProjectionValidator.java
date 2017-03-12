package net.zerobuilder.compiler.analyse;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.Getter;
import net.zerobuilder.Step;
import net.zerobuilder.compiler.generate.DtoBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfo;
import net.zerobuilder.compiler.generate.DtoRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.nCopies;
import static javax.tools.Diagnostic.Kind.ERROR;
import static net.zerobuilder.compiler.Messages.ErrorMessages.STEP_DUPLICATE;
import static net.zerobuilder.compiler.Messages.ErrorMessages.STEP_OUT_OF_BOUNDS;
import static net.zerobuilder.compiler.analyse.Utilities.thrownTypes;

final class ProjectionValidator {

  /**
   * Modifies the parameter order, depending on {@link Step} annotations.
   * If none of the parameters has a {@link Step} annotation, the
   * order of the input parameters is not changed.
   *
   * @param parameters parameters in original order
   * @param <E>        parameter type
   * @return parameters in a potentially different order
   * @throws ValidationException if the input is inconsistent
   */
  static <E extends TmpValidParameter> List<E> shuffledParameters(List<E> parameters)
      throws ValidationException {
    List<E> builder = new ArrayList<>(nCopies(parameters.size(), (E) null));
    List<E> noAnnotation = new ArrayList<>();
    for (E parameter : parameters) {
      int value = parameter.annotation;
      if (value >= 0) {
        parameter
            .checkState(value < parameters.size(), STEP_OUT_OF_BOUNDS)
            .checkState(builder.get(value) == null, STEP_DUPLICATE);
        builder.set(value, parameter);
      } else {
        noAnnotation.add(parameter);
      }
    }
    int pos = 0;
    for (E parameter : noAnnotation) {
      while (builder.get(pos) != null) {
        pos++;
      }
      builder.set(pos++, parameter);
    }
    return builder;
  }

  static abstract class TmpValidParameter {

    final Element element;
    // step position
    final int annotation;

    private TmpValidParameter(Element element, int annotation) {
      this.element = element;
      this.annotation = annotation;
    }

    TmpValidParameter checkState(boolean condition, String message) {
      if (!condition) {
        throw new ValidationException(ERROR,
            message, element);
      }
      return this;
    }
  }

  static final class TmpSimpleParameter extends TmpValidParameter {
    final SimpleParameter parameter;

    private TmpSimpleParameter(Element element, int annotation, SimpleParameter parameter) {
      super(element, annotation);
      this.parameter = parameter;
    }

    static TmpSimpleParameter create(VariableElement parameter) {
      int value = parameter.getAnnotation(Step.class) == null ?
          -1 : parameter.getAnnotation(Step.class).value();
      String name = parameter.getSimpleName().toString();
      TypeName type = TypeName.get(parameter.asType());
      DtoRegularParameter.SimpleParameter regularParameter =
          DtoRegularParameter.create(name, type);
      return new TmpSimpleParameter(parameter, value, regularParameter);
    }
  }

  static final class TmpProjectedParameter extends TmpValidParameter {
    private final ProjectedParameter parameter;

    private TmpProjectedParameter(Element element, int annotation, ProjectedParameter parameter) {
      super(element, annotation);
      this.parameter = parameter;
    }

    static final Function<TmpProjectedParameter, ProjectedParameter> toValidParameter =
        parameter -> parameter.parameter;

    static TmpProjectedParameter create(VariableElement parameter, ProjectionInfo projectionInfo) {
      int value = parameter.getAnnotation(Step.class) == null ?
          -1 : parameter.getAnnotation(Step.class).value();
      String name = parameter.getSimpleName().toString();
      TypeName type = TypeName.get(parameter.asType());
      ProjectedParameter regularParameter =
          DtoRegularParameter.create(name, type, projectionInfo);
      return new TmpProjectedParameter(parameter, value, regularParameter);
    }
  }

  static final class TmpAccessorPair extends TmpValidParameter {
    final AbstractBeanParameter parameter;

    private TmpAccessorPair(Element element, int annotation, AbstractBeanParameter parameter) {
      super(element, annotation);
      this.parameter = parameter;
    }

    static final Function<TmpAccessorPair, AbstractBeanParameter> toValidParameter = parameter -> parameter.parameter;

    static TmpAccessorPair accessorPair(ExecutableElement getter, ExecutableElement setter) {
      int value = getter.getAnnotation(Getter.class) == null ?
          -1 : getter.getAnnotation(Getter.class).value();
      TypeName type = TypeName.get(getter.getReturnType());
      AbstractBeanParameter accessorPair = DtoBeanParameter.accessorPair(type, getter.getSimpleName().toString(),
          thrownTypes(getter), thrownTypes(setter));
      return new TmpAccessorPair(getter, value, accessorPair);
    }

    static TmpAccessorPair createLoneGetter(ExecutableElement getter, AbstractBeanParameter loneGetter) {
      int value = getter.getAnnotation(Getter.class) == null ?
          -1 : getter.getAnnotation(Getter.class).value();
      return new TmpAccessorPair(getter, value, loneGetter);
    }
  }

  private ProjectionValidator() {
    throw new UnsupportedOperationException("no instances");
  }
}
