package net.zerobuilder.modules.builder;

import io.jbock.simple.Component;
import net.zerobuilder.compiler.generate.GoalDescription;

@Component
public interface BuilderComponent {
  BuilderFactory createFactory();

  @Component.Factory
  interface Factory {
    BuilderComponent create(GoalDescription description);
  }
}
