package net.zerobuilder.compiler.analyse;

import javax.lang.model.element.ExecutableElement;
import net.zerobuilder.compiler.generate.GoalContext;
import net.zerobuilder.compiler.generate.GoalDetails;

record GoalElement(
    GoalDetails details,
    ExecutableElement executableElement,
    GoalModifiers goalAnnotation,
    GoalContext context
) {
}
