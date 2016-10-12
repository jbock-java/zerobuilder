package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static net.zerobuilder.compiler.generate.Utilities.asPredicate;

public final class DtoProjectionInfo {

  public interface ProjectionInfo {
    <R, P> R accept(ProjectionInfoCases<R, P> cases, P p);
  }

  public interface ProjectionInfoCases<R, P> {
    R projectionMethod(ProjectionMethod projection, P p);
    R fieldAccess(FieldAccess projection, P p);
    R none();
  }

  static <R> Function<ProjectionInfo, R> asFunction(ProjectionInfoCases<R, Void> cases) {
    return projectionInfo -> projectionInfo.accept(cases, null);
  }

  static <R> Function<ProjectionInfo, R> projectionInfoCases(
      Function<ProjectionMethod, R> projectionMethod,
      Function<FieldAccess, R> fieldAccess,
      Supplier<R> none) {
    return asFunction(new ProjectionInfoCases<R, Void>() {
      @Override
      public R projectionMethod(ProjectionMethod projection, Void aVoid) {
        return projectionMethod.apply(projection);
      }
      @Override
      public R fieldAccess(FieldAccess projection, Void aVoid) {
        return fieldAccess.apply(projection);
      }
      @Override
      public R none() {
        return none.get();
      }
    });
  }

  static final class ProjectionMethod implements ProjectionInfo {
    final String methodName;
    final List<TypeName> thrownTypes;

    private ProjectionMethod(String methodName, List<TypeName> thrownTypes) {
      this.methodName = methodName;
      this.thrownTypes = thrownTypes;
    }

    @Override
    public <R, P> R accept(ProjectionInfoCases<R, P> cases, P p) {
      return cases.projectionMethod(this, p);
    }
  }

  static final class FieldAccess implements ProjectionInfo {
    final String fieldName;

    private FieldAccess(String fieldName) {
      this.fieldName = fieldName;
    }

    @Override
    public <R, P> R accept(ProjectionInfoCases<R, P> cases, P p) {
      return cases.fieldAccess(this, p);
    }
  }

  static final class None implements ProjectionInfo {
    private None() {
    }

    @Override
    public <R, P> R accept(ProjectionInfoCases<R, P> cases, P p) {
      return cases.none();
    }
  }

  public static ProjectionInfo method(String methodName, List<TypeName> thrownTypes) {
    return new ProjectionMethod(methodName, thrownTypes);
  }

  public static ProjectionInfo method(String methodName) {
    return new ProjectionMethod(methodName, emptyList());
  }

  public static ProjectionInfo fieldAccess(String fieldName) {
    return new FieldAccess(fieldName);
  }

  public static ProjectionInfo none() {
    return new None();
  }

  static final Predicate<ProjectionInfo> isPresent =
      asPredicate(projectionInfoCases(
          projection -> true,
          projection -> true,
          () -> false));

  static final Function<ProjectionInfo, List<TypeName>> thrownTypes =
      projectionInfoCases(
          projection -> projection.thrownTypes,
          projection -> emptyList(),
          () -> emptyList());

  private DtoProjectionInfo() {
    throw new UnsupportedOperationException("no instances");
  }
}
