package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static javax.lang.model.element.Modifier.PRIVATE;
import static net.zerobuilder.compiler.generate.Utilities.fieldSpec;
import static net.zerobuilder.compiler.generate.Utilities.memoize;

public final class DtoRegularStep {

  static final class RegularStep extends DtoStep.AbstractStep {
    final DtoRegularParameter.AbstractRegularParameter parameter;
    final List<TypeName> declaredExceptions;

    private final Supplier<FieldSpec> field;
    private final Supplier<Optional<DtoStep.CollectionInfo>> collectionInfo;

    private RegularStep(String thisType,
                        Optional<? extends DtoStep.AbstractStep> nextType,
                        DtoGoal.AbstractGoalDetails goalDetails,
                        DtoContext.BuildersContext context,
                        DtoRegularParameter.AbstractRegularParameter parameter,
                        List<TypeName> declaredExceptions) {
      super(thisType, nextType, goalDetails, context);
      this.declaredExceptions = declaredExceptions;
      this.parameter = parameter;
      this.field = memoizeField(parameter);
      this.collectionInfo = memoizeCollectionInfo(parameter);
    }

    private static Supplier<Optional<DtoStep.CollectionInfo>> memoizeCollectionInfo(
        DtoRegularParameter.AbstractRegularParameter parameter) {
      return memoize(() ->
          DtoStep.CollectionInfo.create(parameter.type, parameter.name));
    }

    private static Supplier<FieldSpec> memoizeField(DtoRegularParameter.AbstractRegularParameter parameter) {
      return memoize(() ->
          fieldSpec(parameter.type, parameter.name, PRIVATE));
    }

    static RegularStep create(String thisType,
                              Optional<? extends DtoStep.AbstractStep> nextType,
                              DtoGoal.AbstractGoalDetails goalDetails,
                              DtoContext.BuildersContext context,
                              DtoRegularParameter.AbstractRegularParameter parameter,
                              List<TypeName> declaredExceptions) {
      return new RegularStep(thisType, nextType, goalDetails, context, parameter, declaredExceptions);
    }

    Optional<DtoStep.CollectionInfo> collectionInfo() {
      return collectionInfo.get();
    }

    FieldSpec field() {
      return field.get();
    }

    @Override
    <R> R accept(DtoStep.StepCases<R> cases) {
      return cases.regularStep(this);
    }
  }


  private DtoRegularStep() {
    throw new UnsupportedOperationException("no instances");
  }
}
