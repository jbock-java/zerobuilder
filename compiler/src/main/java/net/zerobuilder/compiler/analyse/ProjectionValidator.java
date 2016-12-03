package net.zerobuilder.compiler.analyse;

import com.squareup.javapoet.TypeName;
import net.zerobuilder.BeanRejectNull;
import net.zerobuilder.BeanStep;
import net.zerobuilder.RejectNull;
import net.zerobuilder.Step;
import net.zerobuilder.compiler.generate.DtoBeanParameter;
import net.zerobuilder.compiler.generate.DtoBeanParameter.AbstractBeanParameter;
import net.zerobuilder.compiler.generate.DtoProjectionInfo.ProjectionInfo;
import net.zerobuilder.compiler.generate.DtoRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;
import net.zerobuilder.compiler.generate.NullPolicy;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
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

    static NullPolicy nullPolicy(TypeMirror type, RejectNull rejectNullAnnotation, NullPolicy defaultPolicy) {
      if (TypeName.get(type).isPrimitive()) {
        return NullPolicy.ALLOW;
      }
      return rejectNullAnnotation != null ? NullPolicy.REJECT : defaultPolicy;
    }

    static NullPolicy nullPolicy(TypeMirror type, BeanRejectNull rejectNullAnnotation, NullPolicy defaultPolicy) {
      if (TypeName.get(type).isPrimitive()) {
        return NullPolicy.ALLOW;
      }
      return rejectNullAnnotation != null ? NullPolicy.REJECT : defaultPolicy;
    }

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
    static TmpSimpleParameter create(VariableElement parameter,
                                     GoalModifiers goalAnnotation) {
      int value = parameter.getAnnotation(BeanStep.class) == null ?
          -1 : parameter.getAnnotation(BeanStep.class).value();
      RejectNull stepPolicy = parameter.getAnnotation(RejectNull.class);
      NullPolicy nullPolicy = TmpValidParameter.nullPolicy(parameter.asType(), stepPolicy, goalAnnotation.nullPolicy);
      String name = parameter.getSimpleName().toString();
      TypeName type = TypeName.get(parameter.asType());
      DtoRegularParameter.SimpleParameter regularParameter =
          DtoRegularParameter.create(name, type, nullPolicy);
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

    static TmpProjectedParameter create(VariableElement parameter, ProjectionInfo projectionInfo,
                                        GoalModifiers goalAnnotation) {
      int value = parameter.getAnnotation(Step.class) == null ?
          -1 : parameter.getAnnotation(Step.class).value();
      RejectNull stepPolicy = parameter.getAnnotation(RejectNull.class);
      NullPolicy nullPolicy = TmpValidParameter.nullPolicy(parameter.asType(), stepPolicy, goalAnnotation.nullPolicy);
      String name = parameter.getSimpleName().toString();
      TypeName type = TypeName.get(parameter.asType());
      ProjectedParameter regularParameter =
          DtoRegularParameter.create(name, type, nullPolicy, projectionInfo);
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
      int value = getter.getAnnotation(BeanStep.class) == null ?
          -1 : getter.getAnnotation(BeanStep.class).value();
      BeanRejectNull beanRejectNull = getter.getAnnotation(BeanRejectNull.class);
      TypeName type = TypeName.get(getter.getReturnType());
      NullPolicy nullPolicy = TmpValidParameter.nullPolicy(getter.getReturnType(), beanRejectNull, NullPolicy.ALLOW);
      AbstractBeanParameter accessorPair = DtoBeanParameter.accessorPair(type, getter.getSimpleName().toString(), nullPolicy,
          thrownTypes(getter), thrownTypes(setter));
      return new TmpAccessorPair(getter, value, accessorPair);
    }

    static TmpAccessorPair createLoneGetter(ExecutableElement getter, AbstractBeanParameter loneGetter) {
      int value = getter.getAnnotation(BeanStep.class) == null ?
          -1 : getter.getAnnotation(BeanStep.class).value();
      return new TmpAccessorPair(getter, value, loneGetter);
    }
  }

  private ProjectionValidator() {
    throw new UnsupportedOperationException("no instances");
  }
}
