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

  private static <R, P> BiFunction<ProjectionInfo, P, R> asFunction(ProjectionInfoCases<R, P> cases) {
    return (projectionInfo, p) -> projectionInfo.accept(cases, p);
  }

  private static <R> Function<ProjectionInfo, R> projectionInfoCases(
      Function<ProjectionMethod, R> projectionMethod,
      Function<FieldAccess, R> fieldAccess) {
    BiFunction<ProjectionInfo, Void, R> function = asFunction(new ProjectionInfoCases<R, Void>() {
      @Override
      public R projectionMethod(ProjectionMethod projection, Void _null) {
        return projectionMethod.apply(projection);
      }
      @Override
      public R fieldAccess(FieldAccess projection, Void _null) {
        return fieldAccess.apply(projection);
      }
    });
    return projectionInfo -> function.apply(projectionInfo, null);
  }

  public static <R, P> BiFunction<ProjectionInfo, P, R> projectionInfoCases(
      BiFunction<ProjectionMethod, P, R> projectionMethod,
      BiFunction<FieldAccess, P, R> fieldAccess) {
    return asFunction(new ProjectionInfoCases<R, P>() {
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
    public <R, P> R accept(ProjectionInfoCases<R, P> cases, P p) {
      return cases.projectionMethod(this, p);
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
    public <R, P> R accept(ProjectionInfoCases<R, P> cases, P p) {
      return cases.fieldAccess(this, p);
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
