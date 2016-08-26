package isobuilder.compiler;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import dagger.Component;
import dagger.Module;
import dagger.Provides;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.lang.model.SourceVersion;
import javax.lang.model.util.Elements;

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
    private final MyStep myStep;

    @Inject
    Steps(MyStep myStep) {
      this.myStep = myStep;
    }

    ImmutableList<? extends ProcessingStep> getSteps() {
      return ImmutableList.of(myStep);
    }
  }

  @Module
  static final class IsoModule {

    private final ProcessingEnvironment processingEnv;

    IsoModule(ProcessingEnvironment processingEnv) {
      this.processingEnv = processingEnv;
    }

    @Provides
    @Singleton
    Filer provideFiler() {
      return processingEnv.getFiler();
    }

    @Provides
    @Singleton
    Elements provideElements() {
      return processingEnv.getElementUtils();
    }

    @Provides
    @Singleton
    Messager provideMessager() {
      return processingEnv.getMessager();
    }

  }

  @Override
  protected Iterable<? extends ProcessingStep> initSteps() {
    IsoModule module = new IsoModule(processingEnv);
    IsoContext context = builder().isoModule(module).build();
    return context.getSteps().getSteps();
  }

}
