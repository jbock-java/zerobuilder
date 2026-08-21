package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import java.util.List;
import javax.lang.model.element.Modifier;

import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;

public final class DtoGoalDetails {

  public sealed interface AbstractRegularDetails permits InstanceMethodGoalDetails, ConstructorGoalDetails, StaticMethodGoalDetails {

    TypeName type();

    Modifier[] access(Modifier modifiers);

    String name();

    List<String> parameterNames();
  }

  public static abstract class LessAbstractRegularDetails {
    /**
     * @param name           goal name
     * @param parameterNames parameter names in original order
     * @param access         goal options
     */
    LessAbstractRegularDetails(String name, List<String> parameterNames,
                               Access access) {
      this.name = name;
      this.access = access;
      this.parameterNames = parameterNames;
    }

    public final String name;
    public final Access access;

    /**
     * parameter names in original order
     */
    final List<String> parameterNames;

    public List<String> parameterNames() {
      return parameterNames;
    }

    public final String name() {
      return name;
    }

    public final Modifier[] access(Modifier modifiers) {
      return ZeroUtil.modifiers(access, modifiers);
    }

    public final CodeBlock invocationParameters() {
      return CodeBlock.of(String.join(", ", parameterNames));
    }
  }

  public static final class ConstructorGoalDetails extends LessAbstractRegularDetails implements AbstractRegularDetails {

    public final TypeName goalType;
    public final List<TypeVariableName> instanceTypeParameters;

    private ConstructorGoalDetails(
        ClassName goalType,
        String name,
        List<String> parameterNames,
        Access access,
        List<TypeVariableName> instanceTypeParameters) {
      super(name, parameterNames, access);
      this.goalType = parameterizedTypeName(goalType, instanceTypeParameters);
      this.instanceTypeParameters = instanceTypeParameters;
    }

    public static ConstructorGoalDetails create(
        ClassName goalType,
        String name,
        List<String> parameterNames,
        Access access,
        List<TypeVariableName> instanceTypeParameters) {
      return new ConstructorGoalDetails(goalType, name, parameterNames, access, instanceTypeParameters);
    }

    @Override
    public TypeName type() {
      return goalType;
    }
  }

  public static final class InstanceMethodGoalDetails extends LessAbstractRegularDetails implements AbstractRegularDetails {
    public final String methodName;
    public final TypeName goalType;

    // typevars of the method
    public final List<TypeVariableName> typeParameters;

    // typevars of the enclosing class
    public final List<TypeVariableName> instanceTypeParameters;

    // typevars of the returned type
    public final List<TypeVariableName> returnTypeParameters;


    private InstanceMethodGoalDetails(TypeName goalType, String name, List<String> parameterNames, String methodName,
                                      Access access,
                                      List<TypeVariableName> typeParameters,
                                      List<TypeVariableName> instanceTypeParameters,
                                      List<TypeVariableName> returnTypeParameters) {
      super(name, parameterNames, access);
      this.goalType = goalType;
      this.methodName = methodName;
      this.typeParameters = typeParameters;
      this.instanceTypeParameters = instanceTypeParameters;
      this.returnTypeParameters = returnTypeParameters;
    }

    public static InstanceMethodGoalDetails create(TypeName goalType,
                                                   String name,
                                                   List<String> parameterNames,
                                                   String methodName,
                                                   Access access,
                                                   List<TypeVariableName> typeParameters,
                                                   List<TypeVariableName> instanceTypeParameters,
                                                   List<TypeVariableName> returnTypeParameters) {
      return new InstanceMethodGoalDetails(goalType, name, parameterNames, methodName,
          access, typeParameters, instanceTypeParameters, returnTypeParameters);
    }

    @Override
    public TypeName type() {
      return goalType;
    }
  }


  /**
   * Describes static method goal.
   */
  public static final class StaticMethodGoalDetails extends LessAbstractRegularDetails implements AbstractRegularDetails {

    public final List<TypeVariableName> typeParameters;
    public final String methodName;
    public final TypeName goalType;

    private StaticMethodGoalDetails(TypeName goalType, String name,
                                    List<String> parameterNames,
                                    String methodName,
                                    Access access,
                                    List<TypeVariableName> typeParameters) {
      super(name, parameterNames, access);
      this.goalType = goalType;
      this.methodName = methodName;
      this.typeParameters = typeParameters;
    }

    public static StaticMethodGoalDetails create(TypeName goalType,
                                                 String name,
                                                 List<String> parameterNames,
                                                 String methodName,
                                                 Access access,
                                                 List<TypeVariableName> typeParameters) {
      return new StaticMethodGoalDetails(goalType, name, parameterNames, methodName, access, typeParameters);
    }

    @Override
    public TypeName type() {
      return goalType;
    }
  }

  public static final class BeanGoalDetails {
    public final ClassName goalType;
    public final String name;
    public final Access access;
    public final DtoContext.GoalContext context;

    public BeanGoalDetails(ClassName goalType, String name, Access access, DtoContext.GoalContext context) {
      this.name = name;
      this.access = access;
      this.goalType = goalType;
      this.context = context;
    }

    public Modifier[] access(Modifier modifiers) {
      return ZeroUtil.modifiers(access, modifiers);
    }
  }

  public static boolean isInstance(AbstractRegularDetails details) {
    return switch (details) {
      case ConstructorGoalDetails constructor -> false;
      case StaticMethodGoalDetails staticMethod -> false;
      case InstanceMethodGoalDetails instanceMethod -> true;
    };
  }

  private DtoGoalDetails() {
    throw new UnsupportedOperationException("no instances");
  }
}
