package isobuilder.compiler;

import com.squareup.javapoet.ClassName;

import javax.lang.model.element.VariableElement;

final class StepSpec {

  private final ClassName stepName;
  private final VariableElement argument;

  StepSpec(ClassName stepName, VariableElement argument) {
    this.stepName = stepName;
    this.argument = argument;
  }

  ClassName getStepName() {
    return stepName;
  }

  VariableElement getArgument() {
    return argument;
  }

}
