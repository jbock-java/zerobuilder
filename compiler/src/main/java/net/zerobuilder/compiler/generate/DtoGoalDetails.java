package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeVariableName;
import java.util.List;
import javax.lang.model.element.Modifier;

import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;

public final class DtoGoalDetails {

  public static final class AbstractRegularDetails extends LessAbstractRegularDetails {

    public final TypeName goalType;
    public final List<TypeVariableName> instanceTypeParameters;

    private AbstractRegularDetails(
        ClassName goalType,
        String name,
        List<String> parameterNames,
        Access access,
        List<TypeVariableName> instanceTypeParameters) {
      super(name, parameterNames, access);
      this.goalType = parameterizedTypeName(goalType, instanceTypeParameters);
      this.instanceTypeParameters = instanceTypeParameters;
    }

    public static AbstractRegularDetails create(
        ClassName goalType,
        String name,
        List<String> parameterNames,
        Access access,
        List<TypeVariableName> instanceTypeParameters) {
      return new AbstractRegularDetails(goalType, name, parameterNames, access, instanceTypeParameters);
    }

    public TypeName type() {
      return goalType;
    }
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

  private DtoGoalDetails() {
    throw new UnsupportedOperationException("no instances");
  }
}
