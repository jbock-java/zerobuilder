package net.zerobuilder.compiler.analyse;

import com.palantir.javapoet.ClassName;
import javax.lang.model.element.ExecutableElement;
import net.zerobuilder.compiler.generate.GoalDetails;

record GoalElement(
    GoalDetails details,
    ExecutableElement executableElement,
    ClassName generatedType
) {
}
