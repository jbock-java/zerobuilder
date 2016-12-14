package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Collections.emptyList;

public final class DtoProjectionInfo {

  public interface ProjectionInfo {
    <R, P1, P2> R accept(ProjectionInfoCases<R, P1, P2> cases, P1 p1, P2 p2);
  }

  public interface ProjectionInfoCases<R, P1, P2> {
    R projectionMethod(ProjectionMethod projection, P1 p1, P2 p2);
    R fieldAccess(FieldAccess projection, P1 p1, P2 p2);
  }

  private static <R> Function<ProjectionInfo, R> projectionInfoCases(
      Function<ProjectionMethod, R> projectionMethod,
      Function<FieldAccess, R> fieldAccess) {
    ProjectionInfoCases<R, Void, Void> cases = new ProjectionInfoCases<R, Void, Void>() {
      @Override
      public R projectionMethod(ProjectionMethod projection, Void _null, Void _null2) {
        return projectionMethod.apply(projection);
      }
      @Override
      public R fieldAccess(FieldAccess projection, Void _null, Void _null2) {
        return fieldAccess.apply(projection);
      }
    };
    return projectionInfo -> projectionInfo.accept(cases, null, null);
  }

  public static <R, P> BiFunction<ProjectionInfo, P, R> projectionInfoCases(
      BiFunction<ProjectionMethod, P, R> projectionMethod,
      BiFunction<FieldAccess, P, R> fieldAccess) {
    ProjectionInfoCases<R, P, Void> cases = new ProjectionInfoCases<R, P, Void>() {
      @Override
      public R projectionMethod(ProjectionMethod projection, P p, Void _null2) {
        return projectionMethod.apply(projection, p);
      }
      @Override
      public R fieldAccess(FieldAccess projection, P p, Void _null2) {
        return fieldAccess.apply(projection, p);
      }
    };
    return (projectionInfo, p) -> projectionInfo.accept(cases, p, null);
  }

  public static final class ProjectionMethod implements ProjectionInfo {
    public final String methodName;
    final List<TypeName> thrownTypes;

    private ProjectionMethod(String methodName, List<TypeName> thrownTypes) {
      this.methodName = methodName;
      this.thrownTypes = thrownTypes;
    }

    public static ProjectionMethod create(String methodName, List<TypeName> thrownTypes) {
      return new ProjectionMethod(methodName, thrownTypes);
    }

    public static ProjectionMethod create(String methodName) {
      return new ProjectionMethod(methodName, emptyList());
    }

    @Override
    public <R, P1, P2> R accept(ProjectionInfoCases<R, P1, P2> cases, P1 p1, P2 p2) {
      return cases.projectionMethod(this, p1, p2);
    }
  }

  public static final class FieldAccess implements ProjectionInfo {
    public final String fieldName;

    private FieldAccess(String fieldName) {
      this.fieldName = fieldName;
    }

    public static FieldAccess create(String fieldName) {
      return new FieldAccess(fieldName);
    }

    @Override
    public <R, P1, P2> R accept(ProjectionInfoCases<R, P1, P2> cases, P1 p1, P2 p2) {
      return cases.fieldAccess(this, p1, p2);
    }
  }

  public static final Function<ProjectionInfo, List<TypeName>> thrownTypes =
      projectionInfoCases(
          projection -> projection.thrownTypes,
          projection -> emptyList());

  private DtoProjectionInfo() {
    throw new UnsupportedOperationException("no instances");
  }
}
