package net.zerobuilder.compiler.generate;

import net.zerobuilder.compiler.generate.DtoGeneratorOutput.GeneratorFailure;

class GenerateException extends RuntimeException {

  private static final long serialVersionUID = 12;

  GenerateException(String message) {
    super(message);
  }

  GeneratorFailure asFailure() {
    return new GeneratorFailure(getMessage());
  }
}
