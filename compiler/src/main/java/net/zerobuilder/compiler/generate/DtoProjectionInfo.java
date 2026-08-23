package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.TypeName;
import java.util.List;

public final class DtoProjectionInfo {

  public sealed interface ProjectionInfo permits FieldAccess, GetterMethod {
  }

  public static GetterMethod createGetterMethod(String methodName, List<TypeName> thrownTypes) {
    return new GetterMethod(methodName, thrownTypes);
  }

  public static GetterMethod createGetterMethod(String methodName) {
    return new GetterMethod(methodName, List.of());
  }

  public static FieldAccess createFieldAccess(String fieldName) {
    return new FieldAccess(fieldName);
  }

  public static final class GetterMethod implements ProjectionInfo {
    public final String methodName;
    final List<TypeName> thrownTypes;

    private GetterMethod(String methodName, List<TypeName> thrownTypes) {
      this.methodName = methodName;
      this.thrownTypes = thrownTypes;
    }
  }

  public static final class FieldAccess implements ProjectionInfo {
    public final String fieldName;

    private FieldAccess(String fieldName) {
      this.fieldName = fieldName;
    }

  }

  public static List<TypeName> thrownTypes(ProjectionInfo projectionInfo) {
    return switch (projectionInfo) {
      case GetterMethod getter -> getter.thrownTypes;
      case FieldAccess _ -> List.of();
    };
  }

  private DtoProjectionInfo() {
    throw new UnsupportedOperationException("no instances");
  }
}
