package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.TypeName;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;

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

  static Predicate<ProjectionInfo> asPredicate(ProjectionInfoCases<Boolean, Void> cases) {
    return projectionInfo -> projectionInfo.accept(cases, null);
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

  static final Predicate<ProjectionInfo> isPresent
      = asPredicate(new ProjectionInfoCases<Boolean, Void>() {
    @Override
    public Boolean projectionMethod(ProjectionMethod projection, Void p) {
      return true;
    }
    @Override
    public Boolean fieldAccess(FieldAccess projection, Void p) {
      return true;
    }
    @Override
    public Boolean none() {
      return false;
    }
  });

  static final Function<ProjectionInfo, List<TypeName>> thrownTypes
      = asFunction(new ProjectionInfoCases<List<TypeName>, Void>() {
    @Override
    public List<TypeName> projectionMethod(ProjectionMethod projection, Void p) {
      return projection.thrownTypes;
    }
    @Override
    public List<TypeName> fieldAccess(FieldAccess projection, Void p) {
      return emptyList();
    }
    @Override
    public List<TypeName> none() {
      return emptyList();
    }
  });

  private DtoProjectionInfo() {
    throw new UnsupportedOperationException("no instances");
  }
}
