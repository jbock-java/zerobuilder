package net.zerobuilder.compiler.analyse;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.Builder;
import net.zerobuilder.Updater;
import net.zerobuilder.compiler.analyse.DtoGoalElement.AbstractGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.AbstractRegularGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.BeanGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.ModuleChoice;
import net.zerobuilder.compiler.common.LessElements;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.AbstractGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.BeanGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.ProjectedGoalInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.RegularSimpleGoalInput;
import net.zerobuilder.compiler.generate.DtoModule.BeanModule;
import net.zerobuilder.compiler.generate.DtoModule.ProjectedModule;
import net.zerobuilder.compiler.generate.DtoModule.RegularSimpleModule;
import net.zerobuilder.modules.builder.RegularBuilder;
import net.zerobuilder.modules.builder.bean.BeanBuilder;
import net.zerobuilder.modules.generics.GenericsBuilder;
import net.zerobuilder.modules.updater.RegularUpdater;
import net.zerobuilder.modules.updater.bean.BeanUpdater;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.function.Function;

import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.Style.IMMUTABLE;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_SUBGOALS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.REUSE_GENERICS;
import static net.zerobuilder.compiler.Messages.ErrorMessages.REUSE_IMMUTABLE;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.goalElementCases;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.regularGoalElementCases;
import static net.zerobuilder.compiler.analyse.MoreValidations.checkAccessLevel;
import static net.zerobuilder.compiler.analyse.MoreValidations.checkNameConflict;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorB.validateBean;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateBuilder;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateUpdater;
import static net.zerobuilder.compiler.analyse.TypeValidator.validateContextClass;
import static net.zerobuilder.compiler.analyse.Utilities.peer;
import static net.zerobuilder.compiler.common.LessTypes.asTypeElement;
import static net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle.REUSE_INSTANCES;
import static net.zerobuilder.compiler.generate.DtoContext.createContext;
import static net.zerobuilder.compiler.generate.ZeroUtil.flatList;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.rawClassName;
import static net.zerobuilder.compiler.generate.ZeroUtil.transform;

public final class Analyser {

  private static final RegularSimpleModule BUILDER = new RegularBuilder();
  private static final BeanModule BEAN_BUILDER = new BeanBuilder();
  private static final ProjectedModule UPDATER = new RegularUpdater();
  private static final BeanModule BEAN_UPDATER = new BeanUpdater();
  private static final RegularSimpleModule GENERICS = new GenericsBuilder();

  /**
   * Extract all goals from the given type, by inspecting annotations.
   * Perform validations and bundle each goal with the appropriate module.
   *
   * @param tel a type element
   * @return list of goal inputs
   * @throws ValidationException if validation fails
   */
  public static List<AbstractGoalInput> analyse(TypeElement tel) throws ValidationException {
    validateContextClass(tel);
    TypeName type = parameterizedTypeName(ClassName.get(tel),
        transform(tel.getTypeParameters(), TypeVariableName::get));
    ClassName generatedType = peer(rawClassName(type), "Builders");
    GoalContext context = createContext(type, generatedType);
    List<? extends AbstractGoalElement> goals = goals(tel, context);
    checkNameConflict(goals);
    checkAccessLevel(goals);
    return transform(goals, assignModule);
  }

  private static final Function<AbstractGoalElement, AbstractGoalInput> assignModule =
      goalElementCases(
          regularGoalElementCases(
              general -> {
                boolean hasTypevars = hasTypevars(general.executableElement);
                if (hasTypevars && general.goalAnnotation.lifecycle == REUSE_INSTANCES) {
                  throw new ValidationException(REUSE_GENERICS, general.executableElement);
                }
                if (general.style == IMMUTABLE && general.goalAnnotation.lifecycle == REUSE_INSTANCES) {
                  throw new ValidationException(REUSE_IMMUTABLE, general.executableElement);
                }
                return hasTypevars || general.style == IMMUTABLE ?
                    new RegularSimpleGoalInput(GENERICS, validateBuilder.apply(general)) :
                    new RegularSimpleGoalInput(BUILDER, validateBuilder.apply(general));
              },
              projected -> new ProjectedGoalInput(UPDATER, validateUpdater.apply(projected))),
          bean -> bean.moduleChoice == ModuleChoice.BUILDER ?
              new BeanGoalInput(BEAN_BUILDER, validateBean.apply(bean)) :
              new BeanGoalInput(BEAN_UPDATER, validateBean.apply(bean)));

  private static List<? extends AbstractGoalElement> goals(TypeElement tel, GoalContext context) {
    return tel.getAnnotation(net.zerobuilder.BeanBuilder.class) != null ?
        beanGoals(tel, context) :
        regularGoals(tel, context);
  }

  private static boolean hasTypevars(ExecutableElement element) {
    return !element.getTypeParameters().isEmpty()
        || !element.getModifiers().contains(STATIC)
        && !asTypeElement(element.getEnclosingElement().asType()).getTypeParameters().isEmpty();
  }

  private static List<AbstractRegularGoalElement> regularGoals(TypeElement tel, GoalContext context) {
    return tel.getEnclosedElements().stream()
        .filter(el -> el.getAnnotation(Builder.class) != null || el.getAnnotation(Updater.class) != null)
        .filter(el -> el.getKind() == CONSTRUCTOR || el.getKind() == METHOD)
        .map(LessElements::asExecutable)
        .map(DtoGoalElement.createRegular(context))
        .collect(flatList());
  }

  private static List<BeanGoalElement> beanGoals(TypeElement buildElement, GoalContext context) {
    buildElement.getEnclosedElements().stream()
        .filter(el -> el.getAnnotation(Builder.class) != null || el.getAnnotation(Updater.class) != null)
        .findAny()
        .ifPresent(el -> {
          throw new ValidationException(BEAN_SUBGOALS, el);
        });
    return BeanGoalElement.create(buildElement, context);
  }

  private Analyser() {
    throw new UnsupportedOperationException("no instances");
  }
}
