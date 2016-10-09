package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoParameter.AbstractParameter;
import net.zerobuilder.compiler.generate.DtoParameter.RegularParameter;
import net.zerobuilder.compiler.generate.Utilities.ClassNames;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoBeanStep.validBeanParameter;
import static net.zerobuilder.compiler.generate.Utilities.ClassNames.COLLECTION;
import static net.zerobuilder.compiler.generate.Utilities.ClassNames.ITERABLE;
import static net.zerobuilder.compiler.generate.Utilities.ClassNames.SET;
import static net.zerobuilder.compiler.generate.Utilities.fieldSpec;
import static net.zerobuilder.compiler.generate.Utilities.rawClassName;
import static net.zerobuilder.compiler.generate.Utilities.upcase;

final class DtoStep {

  private static final Set<ClassName> LIST_HIERARCHY
      = new HashSet<>(Arrays.asList(ClassNames.LIST, COLLECTION, ITERABLE));

  static final class CollectionInfo {

    /**
     * Initializer for a variable of type {@link AbstractParameter#type}.
     * It evaluates to an empty List or Set.
     */
    final CodeBlock initializer;

    /**
     * Name of the convenience method to be generated, e.g. {@code "emptyFoo"}
     */
    final String name;

    private CollectionInfo(CodeBlock initializer, String name) {
      this.initializer = initializer;
      this.name = name;
    }

    static Optional<CollectionInfo> create(TypeName type, String name) {
      Optional<ClassName> maybeClassName = rawClassName(type);
      if (!maybeClassName.isPresent()) {
        return Optional.empty();
      }
      ClassName className = maybeClassName.get();
      if (LIST_HIERARCHY.contains(className)) {
        return Optional.of(new CollectionInfo(
            CodeBlock.of("$T.emptyList()", Collections.class),
            emptyOptionName(name)));
      }
      if (SET.equals(className)) {
        return Optional.of(new CollectionInfo(
            CodeBlock.of("$T.emptySet()", Collections.class),
            emptyOptionName(name)));
      }
      return Optional.empty();
    }

    private static String emptyOptionName(String name) {
      return "empty" + upcase(name);
    }
  }

  static abstract class AbstractStep {
    final ClassName thisType;
    final TypeName nextType;
    AbstractStep(ClassName thisType, TypeName nextType) {
      this.thisType = thisType;
      this.nextType = nextType;
    }
    abstract <R> R accept(StepCases<R> cases);
  }

  interface StepCases<R> {
    R regularStep(RegularStep step);
    R beanStep(AbstractBeanStep step);
  }

  static <R> Function<AbstractStep, R> asFunction(final StepCases<R> cases) {
    return abstractStep -> abstractStep.accept(cases);
  }

  static <R> StepCases<R> stepCases(final Function<? super RegularStep, R> regularFunction,
                                    final Function<? super AbstractBeanStep, R> beanFunction) {
    return new StepCases<R>() {
      @Override
      public R regularStep(RegularStep step) {
        return regularFunction.apply(step);
      }
      @Override
      public R beanStep(AbstractBeanStep step) {
        return beanFunction.apply(step);
      }
    };
  }

  static final class RegularStep extends AbstractStep {
    final RegularParameter validParameter;
    final List<TypeName> declaredExceptions;

    private RegularStep(ClassName thisType, TypeName nextType, RegularParameter validParameter,
                        List<TypeName> declaredExceptions) {
      super(thisType, nextType);
      this.declaredExceptions = declaredExceptions;
      this.validParameter = validParameter;
    }

    static RegularStep create(ClassName thisType, TypeName nextType, RegularParameter parameter,
                              List<TypeName> declaredExceptions) {
      return new RegularStep(thisType, nextType, parameter, declaredExceptions);
    }

    Optional<CollectionInfo> emptyOption() {
      return CollectionInfo.create(validParameter.type, validParameter.name);
    }

    FieldSpec field() {
      return fieldSpec(validParameter.type, validParameter.name, PRIVATE);
    }

    @Override
    <R> R accept(StepCases<R> cases) {
      return cases.regularStep(this);
    }
  }

  static final Function<AbstractStep, AbstractParameter> abstractParameter
      = asFunction(new StepCases<AbstractParameter>() {
    @Override
    public AbstractParameter regularStep(RegularStep step) {
      return step.validParameter;
    }
    @Override
    public AbstractParameter beanStep(AbstractBeanStep step) {
      return step.acceptBean(validBeanParameter);
    }
  });

  static final Function<AbstractStep, List<TypeName>> declaredExceptions
      = asFunction(new StepCases<List<TypeName>>() {
    @Override
    public List<TypeName> regularStep(RegularStep step) {
      return step.declaredExceptions;
    }
    @Override
    public List<TypeName> beanStep(AbstractBeanStep step) {
      return Collections.emptyList();
    }
  });

  static <R> Function<AbstractStep, R> always(final Function<AbstractStep, R> parameterFunction) {
    return asFunction(new StepCases<R>() {
      @Override
      public R regularStep(RegularStep step) {
        return parameterFunction.apply(step);
      }
      @Override
      public R beanStep(AbstractBeanStep step) {
        return parameterFunction.apply(step);
      }
    });
  }

  static final Function<AbstractStep, Optional<CollectionInfo>> emptyOption
      = asFunction(new StepCases<Optional<CollectionInfo>>() {
    @Override
    public Optional<CollectionInfo> regularStep(RegularStep step) {
      return step.emptyOption();
    }
    @Override
    public Optional<CollectionInfo> beanStep(AbstractBeanStep step) {
      return step.acceptBean(DtoBeanStep.emptyOption);
    }
  });

  private DtoStep() {
    throw new UnsupportedOperationException("no instances");
  }
}
