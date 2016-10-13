package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static net.zerobuilder.compiler.GeneratedLines.GENERATED_ANNOTATION;

public class BeanTest {

  @Test
  public void noRecycling() {
    JavaFileObject businessAnalyst = forSourceLines("beans.BusinessAnalyst",
        "package beans;",
        "import net.zerobuilder.*;",
        "import java.util.List;",
        "import java.util.ArrayList;",
        "",
        "@Builders",
        "@Goal(updater = true, builderAccess = AccessLevel.PACKAGE)",
        "public class BusinessAnalyst {",
        "  private String name;",
        "  BusinessAnalyst() throws ClassNotFoundException {}",
        "  public String getName() { return name; }",
        "  public void setName(String name) { this.name = name; }",
        "}");
    JavaFileObject expected =
        forSourceLines("beans.BusinessAnalystBuilders",
            "package beans;",
            "import javax.annotation.Generated;",
            "",
            GENERATED_ANNOTATION,
            "public final class BusinessAnalystBuilders {",
            "",
            "  private BusinessAnalystBuilders() {",
            "    throw new UnsupportedOperationException(\"no instances\");",
            "  }",
            "",
            "  static BusinessAnalystBuilder.Name businessAnalystBuilder() throws ClassNotFoundException {",
            "    BusinessAnalystBuilderImpl businessAnalystBuilderImpl = new BusinessAnalystBuilderImpl();",
            "    return businessAnalystBuilderImpl;",
            "  }",
            "",
            "  public static BusinessAnalystUpdater businessAnalystUpdater(BusinessAnalyst businessAnalyst) throws ClassNotFoundException {",
            "    BusinessAnalystUpdater updater = new BusinessAnalystUpdater();",
            "    updater.businessAnalyst.setName(businessAnalyst.getName());",
            "    return updater;",
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
            "    public BusinessAnalyst done() { return this.businessAnalyst; }",
            "  }",
            "",
            "  static final class BusinessAnalystBuilderImpl",
            "        implements BusinessAnalystBuilder.Name {",
            "    private final BusinessAnalyst businessAnalyst;",
            "    private BusinessAnalystBuilderImpl() throws ClassNotFoundException {",
            "      this.businessAnalyst = new BusinessAnalyst();",
            "    }",
            "",
            "    @Override public BusinessAnalyst name(String name) {",
            "      this.businessAnalyst.setName(name);",
            "      return this.businessAnalyst;",
            "    }",
            "  }",
            "",
            "  public static final class BusinessAnalystBuilder {",
            "    private BusinessAnalystBuilder() {",
            "      throw new UnsupportedOperationException(\"no instances\");",
            "    }",
            "    public interface Name { BusinessAnalyst name(String name); }",
            "  }",
            "}");
    assertAbout(javaSources()).that(ImmutableList.of(businessAnalyst))
        .processedWith(new ZeroProcessor())
        .compilesWithoutError()
        .and().generatesSources(expected);
  }


}
