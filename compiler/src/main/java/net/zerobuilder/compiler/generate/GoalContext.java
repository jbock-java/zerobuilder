package net.zerobuilder.compiler.generate;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;

/**
 * @param type          The class that contains the goal method(s) or constructor(s).
 *                      This is either a {@link ClassName} or a {@link ParameterizedTypeName}.
 * @param generatedType The type that should be generated.
 */
public record GoalContext(
    TypeName type,
    ClassName generatedType) {
}
