package isobuilder.compiler;

import dagger.Module;
import dagger.Provides;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Singleton;
import javax.lang.model.util.Elements;

@Module
class IsoModule {

  private final ProcessingEnvironment processingEnv;

  IsoModule(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
  }

  @Provides
  @Singleton
  public Filer provideFiler() {
    return processingEnv.getFiler();
  }

  @Provides
  @Singleton
  public Elements provideElements() {
    return processingEnv.getElementUtils();
  }

  @Provides
  @Singleton
  public Messager provideMessager() {
    return processingEnv.getMessager();
  }

}
