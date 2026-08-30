package net.zerobuilder.compiler;

import io.jbock.testing.compile.Compilation;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;

import static io.jbock.testing.compile.CompilationSubject.assertThat;
import static io.jbock.testing.compile.JavaFileObjects.forSourceLines;
import static net.zerobuilder.compiler.Compilers.simpleCompiler;

class StepOrderTest {

  @Test
  void instance() {
    JavaFileObject cube = forSourceLines("cube.Spaghetti",
        "package cube;",
        "import net.zerobuilder.RecordBuilder;",
        "import net.zerobuilder.StepOrder;",
        "import net.zerobuilder.StepName;",
        "",
        "@RecordBuilder",
        "record Spaghetti(",
        "  String cheese,",
        "  @StepOrder(0)",
        "  @StepName(\"sauce\")",
        "  String agar,",
        "  @StepOrder(1)",
        "  String pasta) {",
        "}");
    Compilation compilation = simpleCompiler().compile(cube);
    assertThat(compilation).succeeded();
    assertThat(compilation).generatedSourceFile("cube.SpaghettiBuilders")
        .containsLines(
            "    public PastaStep sauce(String sauce) {",
            "    public CheeseStep pasta(String pasta) {",
            "    public Spaghetti cheese(String cheese) {",
            "}");
  }
}
