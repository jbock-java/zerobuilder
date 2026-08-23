module net.zerobuilder.compiler {

  provides javax.annotation.processing.Processor with net.zerobuilder.compiler.ZeroProcessor;

  requires java.compiler;
  requires com.palantir.javapoet;
  requires io.jbock.simple;
  requires net.zerobuilder;
}
