package net.zerobuilder.compiler.generate;

import io.jbock.javapoet.ClassName;
import io.jbock.javapoet.CodeBlock;
import io.jbock.javapoet.TypeName;
import io.jbock.javapoet.TypeVariableName;
import net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;

public final class DtoGoalDetails {

  interface RegularGoalDetailsCases<R, P> {
    R method(InstanceMethodGoalDetails details, P p);
    R staticMethod(StaticMethodGoalDetails details, P p);
    R constructor(ConstructorGoalDetails details, P p);
  }

  private static <R, P> BiFunction<AbstractRegularDetails, P, R> asFunction(RegularGoalDetailsCases<R, P> cases) {
    return (details, p) -> details.accept(cases, p);
  }

  public static <R, P> BiFunction<AbstractRegularDetails, P, R> regularDetailsCases(
      BiFunction<? super ConstructorGoalDetails, ? super P, ? extends R> constructorFunction,
      BiFunction<? super StaticMethodGoalDetails, ? super P, ? extends R> staticFunction,
      BiFunction<? super InstanceMethodGoalDetails, ? super P, ? extends R> instanceFunction) {
    return asFunction(new RegularGoalDetailsCases<R, P>() {
      @Override
      public R method(InstanceMethodGoalDetails details, P p) {
        return instanceFunction.apply(details, p);
      }
      @Override
      public R staticMethod(StaticMethodGoalDetails details, P p) {
        return staticFunction.apply(details, p);
      }
      @Override
      public R constructor(ConstructorGoalDetails details, P p) {
        return constructorFunction.apply(details, p);
      }
    });
  }

  public static <R> Function<AbstractRegularDetails, R> regularDetailsCases(
      Function<ConstructorGoalDetails, R> constructorFunction,
      Function<StaticMethodGoalDetails, R> staticFunction,
      Function<InstanceMethodGoalDetails, R> instanceFunction) {
    BiFunction<AbstractRegularDetails, Void, R> biFunction =
        regularDetailsCases(
            (x, _null) -> constructorFunction.apply(x),
            (x, _null) -> staticFunction.apply(x),
            (x, _null) -> instanceFunction.apply(x));
    return details -> biFunction.apply(details, null);
  }

  public static abstract class AbstractRegularDetails {

    public final String name;
    public final Access access;
    public final ContextLifecycle lifecycle;

    /**
     * parameter names in original order
     */
    final List<String> parameterNames;

    public final String name() {
      return name;
    }

    public final Modifier[] access(Modifier modifiers) {
      return ZeroUtil.modifiers(access, modifiers);
    }

    public final CodeBlock invocationParameters() {
      return CodeBlock.of(String.join(", ", parameterNames));
    }

    public abstract TypeName type();

    /**
     * @param name           goal name
     * @param parameterNames parameter names in original order
     * @param access         goal options
     * @param lifecycle      lifecycle
     */
    AbstractRegularDetails(String name, List<String> parameterNames,
                           Access access, ContextLifecycle lifecycle) {
      this.name = name;
      this.access = access;
      this.parameterNames = parameterNames;
      this.lifecycle = lifecycle;
    }

    abstract <R, P> R accept(RegularGoalDetailsCases<R, P> cases, P p);
  }

  public static final class ConstructorGoalDetails extends AbstractRegularDetails {

    public final TypeName goalType;
    public final List<TypeVariableName> instanceTypeParameters;

    private ConstructorGoalDetails(ClassName goalType, String name, List<String> parameterNames,
                                   Access access, List<TypeVariableName> instanceTypeParameters,
                                   ContextLifecycle lifecycle) {
      super(name, parameterNames, access, lifecycle);
      this.goalType = parameterizedTypeName(goalType, instanceTypeParameters);
      this.instanceTypeParameters = instanceTypeParameters;
    }

    public static ConstructorGoalDetails create(ClassName goalType, String name, List<String> parameterNames,
                                                Access access, List<TypeVariableName> instanceTypeParameters,
                                                ContextLifecycle lifecycle) {
      return new ConstructorGoalDetails(goalType, name, parameterNames, access, instanceTypeParameters, lifecycle);
    }

    @Override
    public TypeName type() {
      return goalType;
    }

    @Override
    <R, P> R accept(RegularGoalDetailsCases<R, P> cases, P p) {
      return cases.constructor(this, p);
    }
  }

  public static final class InstanceMethodGoalDetails extends AbstractRegularDetails {
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
                                      List<TypeVariableName> returnTypeParameters,
                                      ContextLifecycle lifecycle) {
      super(name, parameterNames, access, lifecycle);
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
                                                   List<TypeVariableName> returnTypeParameters,
                                                   ContextLifecycle lifecycle) {
      return new InstanceMethodGoalDetails(goalType, name, parameterNames, methodName,
          access, typeParameters, instanceTypeParameters, returnTypeParameters, lifecycle);
    }

    @Override
    public TypeName type() {
      return goalType;
    }

    @Override
    <R, P> R accept(RegularGoalDetailsCases<R, P> cases, P p) {
      return cases.method(this, p);
    }
  }


  /**
   * Describes static method goal.
   */
  public static final class StaticMethodGoalDetails extends AbstractRegularDetails {

    public final List<TypeVariableName> typeParameters;
    public final String methodName;
    public final TypeName goalType;

    private StaticMethodGoalDetails(TypeName goalType, String name,
                                    List<String> parameterNames,
                                    String methodName,
                                    Access access,
                                    List<TypeVariableName> typeParameters,
                                    ContextLifecycle lifecycle) {
      super(name, parameterNames, access, lifecycle);
      this.goalType = goalType;
      this.methodName = methodName;
      this.typeParameters = typeParameters;
    }

    public static StaticMethodGoalDetails create(TypeName goalType,
                                                 String name,
                                                 List<String> parameterNames,
                                                 String methodName,
                                                 Access access,
                                                 List<TypeVariableName> typeParameters,
                                                 ContextLifecycle lifecycle) {
      return new StaticMethodGoalDetails(goalType, name, parameterNames, methodName, access, typeParameters, lifecycle);
    }

    @Override
    public TypeName type() {
      return goalType;
    }

    @Override
    <R, P> R accept(RegularGoalDetailsCases<R, P> cases, P p) {
      return cases.staticMethod(this, p);
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

  public static final Function<AbstractRegularDetails, Boolean> isInstance =
      regularDetailsCases(
          constructor -> false,
          staticMethod -> false,
          instanceMethod -> true);

  private DtoGoalDetails() {
    throw new UnsupportedOperationException("no instances");
  }
}
