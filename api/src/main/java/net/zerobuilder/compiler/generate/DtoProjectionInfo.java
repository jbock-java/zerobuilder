package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Collections.emptyList;

public final class DtoProjectionInfo {

  public interface ProjectionInfo {
    <R, P> R accept(ProjectionInfoCases<R, P> cases, P p);
  }

  public interface ProjectionInfoCases<R, P> {
    R projectionMethod(ProjectionMethod projection, P p);
    R fieldAccess(FieldAccess projection, P p);
  }

  static <R> Function<ProjectionInfo, R> asFunction(ProjectionInfoCases<R, Void> cases) {
    return projectionInfo -> projectionInfo.accept(cases, null);
  }

  static <R, P> BiFunction<ProjectionInfo, P, R> asBiFunction(ProjectionInfoCases<R, P> cases) {
    return (projectionInfo, p) -> projectionInfo.accept(cases, p);
  }

  static <R> Function<ProjectionInfo, R> projectionInfoCases(
      Function<ProjectionMethod, R> projectionMethod,
      Function<FieldAccess, R> fieldAccess) {
    return asFunction(new ProjectionInfoCases<R, Void>() {
      @Override
      public R projectionMethod(ProjectionMethod projection, Void aVoid) {
        return projectionMethod.apply(projection);
      }
      @Override
      public R fieldAccess(FieldAccess projection, Void aVoid) {
        return fieldAccess.apply(projection);
      }
    });
  }

  static <R, P> BiFunction<ProjectionInfo, P, R> projectionInfoCases(
      BiFunction<ProjectionMethod, P, R> projectionMethod,
      BiFunction<FieldAccess, P, R> fieldAccess) {
    return asBiFunction(new ProjectionInfoCases<R, P>() {
      @Override
      public R projectionMethod(ProjectionMethod projection, P p) {
        return projectionMethod.apply(projection, p);
      }
      @Override
      public R fieldAccess(FieldAccess projection, P p) {
        return fieldAccess.apply(projection, p);
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

  public static ProjectionInfo method(String methodName, List<TypeName> thrownTypes) {
    return new ProjectionMethod(methodName, thrownTypes);
  }

  public static ProjectionInfo method(String methodName) {
    return new ProjectionMethod(methodName, emptyList());
  }

  static final Function<ProjectionInfo, List<TypeName>> thrownTypes =
      projectionInfoCases(
          projection -> projection.thrownTypes,
          projection -> emptyList());

  private DtoProjectionInfo() {
    throw new UnsupportedOperationException("no instances");
  }
}
