package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoGoalDetails.AbstractGoalDetails;
import net.zerobuilder.compiler.generate.DtoRegularParameter.AbstractRegularParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.ProjectedParameter;
import net.zerobuilder.compiler.generate.DtoRegularParameter.SimpleParameter;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.ZeroUtil.fieldSpec;
import static net.zerobuilder.compiler.generate.ZeroUtil.memoize;

public final class DtoRegularStep {

  public static abstract class AbstractRegularStep extends DtoStep.AbstractStep {

    protected AbstractRegularStep(String thisType,
                                  Optional<AbstractRegularStep> nextType,
                                  AbstractGoalDetails goalDetails,
                                  GoalContext context) {
      super(thisType, nextType, goalDetails, context);
    }

    public abstract Optional<DtoStep.CollectionInfo> collectionInfo();
    public abstract FieldSpec field();
    public abstract AbstractRegularParameter regularParameter();
    public abstract List<TypeName> declaredExceptions();

    @Override
    final <R> R accept(DtoStep.StepCases<R> cases) {
      return cases.regularStep(this);
    }
  }

  public static final class ProjectedRegularStep extends AbstractRegularStep {
    final ProjectedParameter parameter;
    final List<TypeName> declaredExceptions;

    private final Supplier<FieldSpec> field;
    private final Supplier<Optional<DtoStep.CollectionInfo>> collectionInfo;

    private ProjectedRegularStep(String thisType,
                                 Optional<AbstractRegularStep> nextType,
                                 AbstractGoalDetails goalDetails,
                                 GoalContext context,
                                 ProjectedParameter parameter,
                                 List<TypeName> declaredExceptions,
                                 Supplier<FieldSpec> field,
                                 Supplier<Optional<DtoStep.CollectionInfo>> collectionInfo) {
      super(thisType, nextType, goalDetails, context);
      this.declaredExceptions = declaredExceptions;
      this.parameter = parameter;
      this.field = field;
      this.collectionInfo = collectionInfo;
    }

    static ProjectedRegularStep create(String thisType,
                                       Optional<? extends AbstractRegularStep> nextType,
                                       AbstractGoalDetails goalDetails,
                                       GoalContext context,
                                       ProjectedParameter parameter,
                                       List<TypeName> declaredExceptions) {
      return new ProjectedRegularStep(thisType, Optional.ofNullable(nextType.orElse(null)), goalDetails, context, parameter, declaredExceptions,
          memoizeField(parameter), memoizeCollectionInfo(parameter));
    }

    @Override
    public Optional<DtoStep.CollectionInfo> collectionInfo() {
      return collectionInfo.get();
    }

    @Override
    public FieldSpec field() {
      return field.get();
    }

    @Override
    public AbstractRegularParameter regularParameter() {
      return parameter;
    }

    @Override
    public List<TypeName> declaredExceptions() {
      return declaredExceptions;
    }
  }

  public static final class SimpleRegularStep extends AbstractRegularStep {

    public final SimpleParameter parameter;
    private final Supplier<FieldSpec> field;
    private final Supplier<Optional<DtoStep.CollectionInfo>> collectionInfo;

    private SimpleRegularStep(String thisType,
                              Optional<AbstractRegularStep> nextType,
                              AbstractGoalDetails goalDetails,
                              DtoContext.GoalContext context,
                              SimpleParameter parameter,
                              Supplier<FieldSpec> field,
                              Supplier<Optional<DtoStep.CollectionInfo>> collectionInfo) {
      super(thisType, nextType, goalDetails, context);
      this.parameter = parameter;
      this.field = field;
      this.collectionInfo = collectionInfo;
    }

    static SimpleRegularStep create(String thisType,
                                    Optional<? extends AbstractRegularStep> nextType,
                                    AbstractGoalDetails goalDetails,
                                    GoalContext context,
                                    SimpleParameter parameter) {
      return new SimpleRegularStep(thisType, Optional.ofNullable(nextType.orElse(null)), goalDetails, context, parameter,
          memoizeField(parameter), memoizeCollectionInfo(parameter));
    }

    @Override
    public Optional<DtoStep.CollectionInfo> collectionInfo() {
      return collectionInfo.get();
    }

    @Override
    public FieldSpec field() {
      return field.get();
    }

    @Override
    public AbstractRegularParameter regularParameter() {
      return parameter;
    }

    @Override
    public List<TypeName> declaredExceptions() {
      return emptyList();
    }
  }

  private static Supplier<Optional<DtoStep.CollectionInfo>> memoizeCollectionInfo(
      AbstractRegularParameter parameter) {
    return memoize(() ->
        DtoStep.CollectionInfo.create(parameter.type, parameter.name));
  }

  private static Supplier<FieldSpec> memoizeField(AbstractRegularParameter parameter) {
    return memoize(() ->
        fieldSpec(parameter.type, parameter.name, PRIVATE));
  }

  private DtoRegularStep() {
    throw new UnsupportedOperationException("no instances");
  }
}
