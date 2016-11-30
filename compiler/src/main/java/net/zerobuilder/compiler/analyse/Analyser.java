package net.zerobuilder.compiler.analyse;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import net.zerobuilder.AccessLevel;
import net.zerobuilder.Builders;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.analyse.DtoGoalElement.AbstractGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.AbstractRegularGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.BeanGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.ModuleChoice;
import net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.BeanDescriptionInput;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.DescriptionInput;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.ProjectedDescriptionInput;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.RegularSimpleDescriptionInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.GeneratorInput;
import net.zerobuilder.compiler.generate.DtoModule.BeanModule;
import net.zerobuilder.compiler.generate.DtoModule.ProjectedModule;
import net.zerobuilder.compiler.generate.DtoModule.RegularSimpleModule;
import net.zerobuilder.modules.builder.RegularBuilder;
import net.zerobuilder.modules.builder.bean.BeanBuilder;
import net.zerobuilder.modules.generics.GenericsBuilder;
import net.zerobuilder.modules.updater.RegularUpdater;
import net.zerobuilder.modules.updater.bean.BeanUpdater;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_SUBGOALS;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.goalElementCases;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.regularGoalElementCases;
import static net.zerobuilder.compiler.analyse.MoreValidations.checkAccessLevel;
import static net.zerobuilder.compiler.analyse.MoreValidations.checkNameConflict;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorB.validateBean;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateBuilder;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateUpdater;
import static net.zerobuilder.compiler.analyse.TypeValidator.validateContextClass;
import static net.zerobuilder.compiler.analyse.Utilities.flatList;
import static net.zerobuilder.compiler.analyse.Utilities.peer;
import static net.zerobuilder.compiler.analyse.Utilities.transform;
import static net.zerobuilder.compiler.common.LessElements.asExecutable;
import static net.zerobuilder.compiler.common.LessTypes.asTypeElement;
import static net.zerobuilder.compiler.generate.DtoContext.createContext;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;
import static net.zerobuilder.compiler.generate.ZeroUtil.rawClassName;

public final class Analyser {

  private static final RegularSimpleModule BUILDER = new RegularBuilder();
  private static final BeanModule BEAN_BUILDER = new BeanBuilder();
  private static final ProjectedModule UPDATER = new RegularUpdater();
  private static final BeanModule BEAN_UPDATER = new BeanUpdater();
  private static final RegularSimpleModule GENERICS = new GenericsBuilder();

  public static GeneratorInput analyse(TypeElement tel) throws ValidationException {
    validateContextClass(tel);
    Builders builders = tel.getAnnotation(Builders.class);
    ContextLifecycle lifecycle = builders != null && builders.recycle()
        ? ContextLifecycle.REUSE_INSTANCES
        : ContextLifecycle.NEW_INSTANCE;
    TypeName type = parameterizedTypeName(ClassName.get(tel),
        transform(tel.getTypeParameters(), TypeVariableName::get));
    ClassName generatedType = peer(rawClassName(type), "Builders");
    GoalContext context = createContext(type, generatedType, lifecycle);
    List<? extends AbstractGoalElement> goals = goals(tel);
    checkNameConflict(goals);
    checkAccessLevel(goals);
    List<DescriptionInput> descriptions = transform(goals, description);
    return GeneratorInput.create(context, descriptions);
  }

  private static final Function<AbstractGoalElement, DescriptionInput> description =
      goalElementCases(
          regularGoalElementCases(
              general -> hasTypevars(general.executableElement) ?
                  new RegularSimpleDescriptionInput(GENERICS, validateBuilder.apply(general)) :
                  new RegularSimpleDescriptionInput(BUILDER, validateBuilder.apply(general)),
              projected -> new ProjectedDescriptionInput(UPDATER, validateUpdater.apply(projected))),
          bean -> bean.moduleChoice == ModuleChoice.BUILDER ?
              new BeanDescriptionInput(BEAN_BUILDER, validateBean.apply(bean)) :
              new BeanDescriptionInput(BEAN_UPDATER, validateBean.apply(bean)));

  /**
   * @param tel a class that carries the {@link net.zerobuilder.Builders} annotation
   * @return the goals that this class defines: one per {@link Goal} annotation
   * @throws ValidationException if validation fails
   */
  private static List<? extends AbstractGoalElement> goals(TypeElement tel) throws ValidationException {
    Builders builders = tel.getAnnotation(Builders.class);
    AccessLevel defaultAccess = builders == null ? AccessLevel.PUBLIC : builders.access();
    return tel.getAnnotation(Goal.class) != null ?
        beanGoals(tel, defaultAccess) :
        regularGoals(tel, defaultAccess);
  }

  private static boolean hasTypevars(ExecutableElement element) {
    if (!element.getTypeParameters().isEmpty()) {
      return true;
    }
    if (element.getModifiers().contains(STATIC)) {
      return false;
    }
    return !asTypeElement(element.getEnclosingElement().asType())
        .getTypeParameters().isEmpty();
  }

  private static List<AbstractRegularGoalElement> regularGoals(TypeElement tel, AccessLevel defaultAccess) {
    return tel.getEnclosedElements().stream()
        .filter(el -> el.getAnnotation(Goal.class) != null)
        .filter(el -> el.getKind() == CONSTRUCTOR || el.getKind() == METHOD)
        .map(el -> asExecutable(el))
        .map(el -> DtoGoalElement.createRegular(el, defaultAccess))
        .collect(flatList());
  }

  private static List<BeanGoalElement> beanGoals(TypeElement buildElement, AccessLevel defaultAccess) {
    Optional<? extends Element> annotated = buildElement.getEnclosedElements().stream()
        .filter(el -> el.getAnnotation(Goal.class) != null)
        .filter(el -> el.getKind() == METHOD || el.getKind() == CONSTRUCTOR)
        .findAny();
    if (annotated.isPresent()) {
      throw new ValidationException(BEAN_SUBGOALS, annotated.get());
    }
    return BeanGoalElement.create(buildElement, defaultAccess);
  }

  private Analyser() {
    throw new UnsupportedOperationException("no instances");
  }
}
