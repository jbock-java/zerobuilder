package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;

public final class DtoProjectionInfo {

  public interface ProjectionInfo {
    <R> R accept(ProjectionInfoCases<R> cases);
  }

  public interface ProjectionInfoCases<R> {
    R projectionMethod(ProjectionMethod projection);
    R fieldAccess(FieldAccess projection);
    R none();
  }

  public interface ProjectionInfoRequiredCases<R> {
    R projectionMethod(ProjectionMethod projection);
    R fieldAccess(FieldAccess projection);
  }

  static <R> Function<ProjectionInfo, R> asFunction(ProjectionInfoCases<R> cases) {
    return projectionInfo -> projectionInfo.accept(cases);
  }

  static Predicate<ProjectionInfo> asPredicate(ProjectionInfoCases<Boolean> cases) {
    return projectionInfo -> projectionInfo.accept(cases);
  }

  static <R> Function<ProjectionInfo, R> asFunction(ProjectionInfoRequiredCases<R> cases) {
    return asFunction(new ProjectionInfoCases<R>() {
      @Override
      public R projectionMethod(ProjectionMethod projectionMethod) {
        return cases.projectionMethod(projectionMethod);
      }
      @Override
      public R fieldAccess(FieldAccess fieldAccess) {
        return cases.fieldAccess(fieldAccess);
      }
      @Override
      public R none() {
        // should never happen
        throw new IllegalStateException("ProjectionInfo required");
      }
    });
  }

  public static final class ProjectionMethod implements ProjectionInfo {
    final String methodName;
    final List<TypeName> thrownExceptions;

    private ProjectionMethod(String methodName, List<TypeName> thrownExceptions) {
      this.methodName = methodName;
      this.thrownExceptions = thrownExceptions;
    }

    @Override
    public <R> R accept(ProjectionInfoCases<R> cases) {
      return cases.projectionMethod(this);
    }
  }

  public static final class FieldAccess implements ProjectionInfo {
    final String fieldName;

    private FieldAccess(String fieldName) {
      this.fieldName = fieldName;
    }

    @Override
    public <R> R accept(ProjectionInfoCases<R> cases) {
      return cases.fieldAccess(this);
    }
  }

  public static final class None implements ProjectionInfo {
    private None() {
    }

    @Override
    public <R> R accept(ProjectionInfoCases<R> cases) {
      return cases.none();
    }
  }

  public static ProjectionInfo method(String methodName, List<TypeName> thrownExceptions) {
    return new ProjectionMethod(methodName, thrownExceptions);
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

  static final Predicate<ProjectionInfo> isPresent
      = asPredicate(new ProjectionInfoCases<Boolean>() {
    @Override
    public Boolean projectionMethod(ProjectionMethod projection) {
      return true;
    }
    @Override
    public Boolean fieldAccess(FieldAccess projection) {
      return true;
    }
    @Override
    public Boolean none() {
      return false;
    }
  });

  private DtoProjectionInfo() {
    throw new UnsupportedOperationException("no instances");
  }
}
