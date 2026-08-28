package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.TypeName;

import java.util.List;

public final class DtoProjectionInfo {

  public sealed interface ProjectionInfo permits FieldAccess, GetterMethod {
  }

  public static GetterMethod createGetterMethod(
      String methodName,
      List<TypeName> thrownTypes) {
    return new GetterMethod(methodName, thrownTypes);
  }

  public static FieldAccess createFieldAccess(String fieldName) {
    return new FieldAccess(fieldName);
  }

  public record GetterMethod(
      String methodName,
      List<TypeName> thrownTypes) implements ProjectionInfo {
  }

  public record FieldAccess(
      String fieldName) implements ProjectionInfo {
  }

  public static List<TypeName> thrownTypes(ProjectionInfo projectionInfo) {
    return switch (projectionInfo) {
      case GetterMethod getter -> getter.thrownTypes;
      case FieldAccess _ -> List.of();
    };
  }

  private DtoProjectionInfo() {
  }
}
