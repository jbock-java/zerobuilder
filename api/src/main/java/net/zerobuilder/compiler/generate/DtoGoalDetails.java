package net.zerobuilder.compiler.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.Access;
import net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;

public final class DtoGoalDetails {

  public interface AbstractGoalDetails {

    /**
     * Returns the goal name.
     *
     * @return goal name
     */
    String name();
    Modifier[] access(Modifier... modifiers);
    TypeName type();

    <R> R acceptAbstract(AbstractGoalDetailsCases<R> cases);
  }

  interface AbstractGoalDetailsCases<R> {
    R regular(AbstractRegularDetails details);
    R bean(BeanGoalDetails details);
  }

  interface RegularGoalDetailsCases<R, P> {
    R method(InstanceMethodGoalDetails details, P p);
    R staticMethod(StaticMethodGoalDetails details, P p);
    R constructor(ConstructorGoalDetails details, P p);
  }

  public static <R> Function<AbstractGoalDetails, R> asFunction(AbstractGoalDetailsCases<R> cases) {
    return details -> details.acceptAbstract(cases);
  }

  public static <R, P> BiFunction<AbstractRegularDetails, P, R> asFunction(RegularGoalDetailsCases<R, P> cases) {
    return (details, p) -> details.accept(cases, p);
  }

  public static <R, P> BiFunction<AbstractRegularDetails, P, R> regularDetailsCases(
      BiFunction<ConstructorGoalDetails, P, R> constructorFunction,
      BiFunction<StaticMethodGoalDetails, P, R> staticFunction,
      BiFunction<InstanceMethodGoalDetails, P, R> instanceFunction) {
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

  public static abstract class AbstractRegularDetails implements AbstractGoalDetails {

    public final String name;
    final Access access;
    public final ContextLifecycle lifecycle;

    /**
     * parameter names in original order
     */
    final List<String> parameterNames;

    public final String name() {
      return name;
    }

    public final Modifier[] access(Modifier... modifiers) {
      return access.modifiers(modifiers);
    }

    public abstract TypeName type();

    /**
     * @param name           goal name
     * @param parameterNames parameter names in original order
     * @param access         goal options
     * @param lifecycle
     */
    AbstractRegularDetails(String name, List<String> parameterNames,
                           Access access, ContextLifecycle lifecycle) {
      this.name = name;
      this.access = access;
      this.parameterNames = parameterNames;
      this.lifecycle = lifecycle;
    }

    @Override
    public final <R> R acceptAbstract(AbstractGoalDetailsCases<R> cases) {
      return cases.regular(this);
    }

    abstract <R, P> R accept(RegularGoalDetailsCases<R, P> cases, P p);
  }

  public static final class ConstructorGoalDetails extends AbstractRegularDetails
      implements AbstractGoalDetails {

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
  public static final class StaticMethodGoalDetails extends AbstractRegularDetails
      implements AbstractGoalDetails {

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

  public static final class BeanGoalDetails implements AbstractGoalDetails {
    public final ClassName goalType;
    public final String name;
    private final Access access;
    public final ContextLifecycle lifecycle;
    public BeanGoalDetails(ClassName goalType, String name, Access access, ContextLifecycle lifecycle) {
      this.name = name;
      this.access = access;
      this.goalType = goalType;
      this.lifecycle = lifecycle;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public Modifier[] access(Modifier... modifiers) {
      return access.modifiers(modifiers);
    }

    @Override
    public TypeName type() {
      return goalType;
    }

    @Override
    public <R> R acceptAbstract(AbstractGoalDetailsCases<R> cases) {
      return cases.bean(this);
    }
  }

  private DtoGoalDetails() {
    throw new UnsupportedOperationException("no instances");
  }
}
