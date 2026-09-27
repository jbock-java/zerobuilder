package net.zerobuilder.compiler.generate;

public final class DtoProjectionInfo {

  public sealed interface ProjectionInfo permits FieldAccess, GetterMethod {
  }

  public static GetterMethod createGetterMethod(String methodName) {
    return new GetterMethod(methodName);
  }

  public static FieldAccess createFieldAccess(String fieldName) {
    return new FieldAccess(fieldName);
  }

  public record GetterMethod(String methodName) implements ProjectionInfo {
  }

  public record FieldAccess(
      String fieldName) implements ProjectionInfo {
  }

  private DtoProjectionInfo() {
  }
}
