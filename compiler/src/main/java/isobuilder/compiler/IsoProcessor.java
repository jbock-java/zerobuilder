package isobuilder.compiler;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import dagger.Component;

import javax.annotation.processing.Processor;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.lang.model.SourceVersion;

import static isobuilder.compiler.DaggerIsoProcessor_IsoContext.builder;

@AutoService(Processor.class)
public class IsoProcessor extends BasicAnnotationProcessor {

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Component(modules = IsoModule.class)
  @Singleton
  interface IsoContext {
    Steps getSteps();
  }

  static class Steps {
    private final BuilderStep builderStep;

    @Inject
    Steps(BuilderStep builderStep) {
      this.builderStep = builderStep;
    }

    ImmutableList<? extends ProcessingStep> getSteps() {
      return ImmutableList.of(builderStep);
    }
  }

  @Override
  protected Iterable<? extends ProcessingStep> initSteps() {
    IsoModule module = new IsoModule(processingEnv);
    IsoContext context = builder().isoModule(module).build();
    return context.getSteps().getSteps();
  }

}
