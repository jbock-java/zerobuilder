package net.zerobuilder.compiler;

import io.jbock.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static io.jbock.testing.compile.CompilationSubject.assertThat;
import static io.jbock.testing.compile.JavaFileObjects.forSourceLines;
import static net.zerobuilder.compiler.Compilers.simpleCompiler;

public class BeanTest {

  @Test
  public void noRecycling() {
    JavaFileObject businessAnalyst = forSourceLines("beans.BusinessAnalyst",
        "package beans;",
        "import net.zerobuilder.*;",
        "import java.util.List;",
        "import java.util.ArrayList;",
        "",
        "@BeanBuilder",
        "public class BusinessAnalyst {",
        "  private String name;",
        "  BusinessAnalyst() throws ClassNotFoundException {}",
        "  public String getName() { return name; }",
        "  public void setName(String name) { this.name = name; }",
        "}");
    Compilation compilation = simpleCompiler().compile(businessAnalyst);
    assertThat(compilation).succeeded();
    assertThat(compilation).generatedSourceFile("beans.BusinessAnalystBuilders").containsLines(
        "package beans;",
        "import javax.annotation.processing.Generated;",
        "",
        "@Generated(",
        "    value = \"net.zerobuilder.compiler.ZeroProcessor\",",
        "    comments = \"https://github.com/h908714124/zerobuilder\"",
        ")",
        "public final class BusinessAnalystBuilders {",
        "  private BusinessAnalystBuilders() {",
        "    throw new UnsupportedOperationException(\"no instances\");",
        "  }",
        "",
        "  public static BusinessAnalystBuilder.Name businessAnalystBuilder() throws ClassNotFoundException {",
        "    return new BusinessAnalystBuilderImpl();",
        "  }",
        "",
        "  public static BusinessAnalystUpdater businessAnalystUpdater(BusinessAnalyst businessAnalyst)",
        "      throws ClassNotFoundException {",
        "    BusinessAnalystUpdater _updater = new BusinessAnalystUpdater();",
        "    _updater.businessAnalyst.setName(businessAnalyst.getName());",
        "    return _updater;",
        "  }",
        "",
        "  private static final class BusinessAnalystBuilderImpl implements BusinessAnalystBuilder.Name {",
        "    private final BusinessAnalyst businessAnalyst;",
        "    BusinessAnalystBuilderImpl() throws ClassNotFoundException {",
        "      this.businessAnalyst = new BusinessAnalyst();",
        "    }",
        "",
        "    @Override",
        "    public BusinessAnalyst name(String name) {",
        "      this.businessAnalyst.setName(name);",
        "      BusinessAnalyst _businessAnalyst = this.businessAnalyst;",
        "      return _businessAnalyst;",
        "    }",
        "  }",
        "",
        "  public static final class BusinessAnalystBuilder {",
        "    private BusinessAnalystBuilder() {",
        "      throw new UnsupportedOperationException(\"no instances\");",
        "    }",
        "    public interface Name {",
        "      BusinessAnalyst name(String name);",
        "    }",
        "  }",
        "",
        "  public static final class BusinessAnalystUpdater {",
        "    private final BusinessAnalyst businessAnalyst;",
        "    private BusinessAnalystUpdater() throws ClassNotFoundException {",
        "      this.businessAnalyst = new BusinessAnalyst();",
        "    }",
        "",
        "    public BusinessAnalystUpdater name(String name) {",
        "      this.businessAnalyst.setName(name);",
        "      return this;",
        "    }",
        "",
        "    public BusinessAnalyst done() {",
        "      BusinessAnalyst _businessAnalyst = this.businessAnalyst;",
        "      return _businessAnalyst;",
        "    }",
        "  }",
        "}");
  }
}
