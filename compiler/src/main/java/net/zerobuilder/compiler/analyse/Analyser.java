package net.zerobuilder.compiler.analyse;

import com.squareup.javapoet.ClassName;
import net.zerobuilder.AccessLevel;
import net.zerobuilder.Builders;
import net.zerobuilder.Goal;
import net.zerobuilder.compiler.analyse.DtoGoalElement.AbstractGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.AbstractRegularGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.BeanGoalElement;
import net.zerobuilder.compiler.analyse.DtoGoalElement.ModuleChoice;
import net.zerobuilder.compiler.generate.DtoContext.ContextLifecycle;
import net.zerobuilder.compiler.generate.DtoContext.GoalContext;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.DescriptionInput;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.ProjectedDescriptionInput;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.SimpleDescriptionInput;
import net.zerobuilder.compiler.generate.DtoDescriptionInput.SimpleRegularDescriptionInput;
import net.zerobuilder.compiler.generate.DtoGeneratorInput.GeneratorInput;
import net.zerobuilder.compiler.generate.DtoModule.Module;
import net.zerobuilder.compiler.generate.DtoModule.ProjectedModule;
import net.zerobuilder.compiler.generate.DtoModule.RegularContractModule;
import net.zerobuilder.modules.builder.Builder;
import net.zerobuilder.modules.generics.GenericsBuilder;
import net.zerobuilder.modules.updater.Updater;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.ElementKind.METHOD;
import static net.zerobuilder.compiler.Messages.ErrorMessages.BEAN_SUBGOALS;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.goalElementCases;
import static net.zerobuilder.compiler.analyse.DtoGoalElement.regularGoalElementCases;
import static net.zerobuilder.compiler.analyse.MoreValidations.checkAccessLevel;
import static net.zerobuilder.compiler.analyse.MoreValidations.checkMinParameters;
import static net.zerobuilder.compiler.analyse.MoreValidations.checkNameConflict;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorB.validateBean;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateBuilder;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateGenerics;
import static net.zerobuilder.compiler.analyse.ProjectionValidatorV.validateUpdater;
import static net.zerobuilder.compiler.analyse.TypeValidator.validateContextClass;
import static net.zerobuilder.compiler.analyse.Utilities.flatList;
import static net.zerobuilder.compiler.analyse.Utilities.peer;
import static net.zerobuilder.compiler.analyse.Utilities.transform;
import static net.zerobuilder.compiler.common.LessElements.asExecutable;
import static net.zerobuilder.compiler.generate.DtoContext.createContext;

public final class Analyser {

  private static final Module BUILDER = new Builder();
  private static final ProjectedModule UPDATER = new Updater();
  private static final RegularContractModule GENERICS = new GenericsBuilder();

  public static GeneratorInput analyse(TypeElement tel) throws ValidationException {
    validateContextClass(tel);
    ContextLifecycle lifecycle = tel.getAnnotation(Builders.class).recycle()
        ? ContextLifecycle.REUSE_INSTANCES
        : ContextLifecycle.NEW_INSTANCE;
    ClassName type = ClassName.get(tel);
    ClassName generatedType = peer(type, "Builders");
    GoalContext context = createContext(type, generatedType, lifecycle);
    List<? extends AbstractGoalElement> goals = goals(tel);
    checkNameConflict(goals);
    checkAccessLevel(goals);
    checkMinParameters(goals);
    List<DescriptionInput> descriptions = transform(goals, description);
    return GeneratorInput.create(context, descriptions);
  }

  private static final Function<AbstractGoalElement, DescriptionInput> description =
      goalElementCases(
          regularGoalElementCases(
              general -> new SimpleDescriptionInput(BUILDER, validateBuilder.apply(general)),
              generalStatic -> new SimpleRegularDescriptionInput(GENERICS, validateGenerics(generalStatic)),
              projected -> new ProjectedDescriptionInput(UPDATER, validateUpdater.apply(projected))),
          bean -> bean.moduleChoice == ModuleChoice.BUILDER ?
              new SimpleDescriptionInput(BUILDER, validateBean.apply(bean)) :
              new ProjectedDescriptionInput(UPDATER, validateBean.apply(bean)));

  /**
   * @param tel a class that carries the {@link net.zerobuilder.Builders} annotation
   * @return the goals that this class defines: one per {@link Goal} annotation
   * @throws ValidationException if validation fails
   */
  private static List<? extends AbstractGoalElement> goals(TypeElement tel) throws ValidationException {
    Builders buildersAnnotation = tel.getAnnotation(Builders.class);
    AccessLevel defaultAccess = buildersAnnotation.access();
    return tel.getAnnotation(Goal.class) != null ?
        beanGoals(tel, defaultAccess) :
        regularGoals(tel, defaultAccess);
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
