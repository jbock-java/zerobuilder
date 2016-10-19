package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoBeanStep.AbstractBeanStep;
import net.zerobuilder.compiler.generate.DtoContext.BuildersContext;
import net.zerobuilder.compiler.generate.DtoGoal.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoParameter.AbstractParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.AbstractRegularParameter;
import net.zerobuilder.compiler.generate.Utilities.ClassNames;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.DtoBeanStep.validBeanParameter;
import static net.zerobuilder.compiler.generate.Utilities.ClassNames.COLLECTION;
import static net.zerobuilder.compiler.generate.Utilities.ClassNames.ITERABLE;
import static net.zerobuilder.compiler.generate.Utilities.ClassNames.SET;
import static net.zerobuilder.compiler.generate.Utilities.fieldSpec;
import static net.zerobuilder.compiler.generate.Utilities.memoize;
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

  public static abstract class AbstractStep {

    public final AbstractGoalDetails goalDetails;
    public final BuildersContext context;
    public final String thisType;
    public final Optional<? extends AbstractStep> nextStep;

    static TypeName nextType(AbstractStep step) {
      if (step.nextStep.isPresent()) {
        return step.context.generatedType
            .nestedClass(upcase(step.goalDetails.name + "Builder"))
            .nestedClass(step.nextStep.get().thisType);
      }
      return step.goalDetails.type();
    }

    AbstractStep(String thisType,
                 Optional<? extends AbstractStep> nextStep,
                 AbstractGoalDetails goalDetails,
                 BuildersContext context) {
      this.thisType = thisType;
      this.nextStep = nextStep;
      this.goalDetails = goalDetails;
      this.context = context;
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
    final AbstractRegularParameter parameter;
    final List<TypeName> declaredExceptions;

    private final Supplier<FieldSpec> field;
    private final Supplier<Optional<CollectionInfo>> collectionInfo;

    private RegularStep(String thisType,
                        Optional<? extends AbstractStep> nextType,
                        AbstractGoalDetails goalDetails,
                        BuildersContext context,
                        AbstractRegularParameter parameter,
                        List<TypeName> declaredExceptions) {
      super(thisType, nextType, goalDetails, context);
      this.declaredExceptions = declaredExceptions;
      this.parameter = parameter;
      this.field = memoizeField(parameter);
      this.collectionInfo = memoizeCollectionInfo(parameter);
    }

    private static Supplier<Optional<CollectionInfo>> memoizeCollectionInfo(
        AbstractRegularParameter parameter) {
      return memoize(() ->
          CollectionInfo.create(parameter.type, parameter.name));
    }

    private static Supplier<FieldSpec> memoizeField(AbstractRegularParameter parameter) {
      return memoize(() ->
          fieldSpec(parameter.type, parameter.name, PRIVATE));
    }

    static RegularStep create(String thisType,
                              Optional<? extends AbstractStep> nextType,
                              AbstractGoalDetails goalDetails,
                              BuildersContext context,
                              AbstractRegularParameter parameter,
                              List<TypeName> declaredExceptions) {
      return new RegularStep(thisType, nextType, goalDetails, context, parameter, declaredExceptions);
    }

    Optional<CollectionInfo> collectionInfo() {
      return collectionInfo.get();
    }

    FieldSpec field() {
      return field.get();
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
      return step.parameter;
    }
    @Override
    public AbstractParameter beanStep(AbstractBeanStep step) {
      return step.acceptBean(validBeanParameter);
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

  private DtoStep() {
    throw new UnsupportedOperationException("no instances");
  }
}
