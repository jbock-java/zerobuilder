package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static net.zerobuilder.compiler.test_util.GeneratedLines.GENERATED_ANNOTATION;

public class RecyclingBeanTest {

  @Test
  public void recycling() {
    JavaFileObject businessAnalyst = forSourceLines("beans.BusinessAnalyst",
        "package beans;",
        "import net.zerobuilder.*;",
        "import java.util.List;",
        "import java.util.ArrayList;",
        "",
        "@BeanBuilder",
        "public class BusinessAnalyst {",
        "  private String name;",
        "  private List<String> notes;",
        "  public String getName() { return name; }",
        "  public void setName(String name) { this.name = name; }",
        "  public List<String> getNotes() {",
        "    if (notes == null) notes = new ArrayList<>();",
        "    return notes;",
        "  }",
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
            "  public static BusinessAnalystBuilder.Name businessAnalystBuilder() {",
            "    return new BusinessAnalystBuilderImpl()",
            "  }",
            "",
            "  public static BusinessAnalystUpdater businessAnalystUpdater(BusinessAnalyst businessAnalyst) {",
            "    BusinessAnalystUpdater _updater = new BusinessAnalystUpdater();",
            "    _updater.businessAnalyst.setName(businessAnalyst.getName());",
            "    for (String string : businessAnalyst.getNotes()) {",
            "      _updater.businessAnalyst.getNotes().add(string);",
            "    }",
            "    return _updater;",
            "  }",
            "",
            "  private static final class BusinessAnalystBuilderImpl",
            "        implements BusinessAnalystBuilder.Name, BusinessAnalystBuilder.Notes {",
            "    private final BusinessAnalyst businessAnalyst;",
            "    BusinessAnalystBuilderImpl() {",
            "      this.businessAnalyst = new BusinessAnalyst();",
            "    }",
            "",
            "    @Override public BusinessAnalystBuilder.Notes name(String name) {",
            "      this.businessAnalyst.setName(name);",
            "      return this;",
            "    }",
            "",
            "    @Override public BusinessAnalyst notes(Iterable<? extends String> notes) {",
            "      for (String string : notes) {",
            "        this.businessAnalyst.getNotes().add(string);",
            "      }",
            "      BusinessAnalyst _businessAnalyst = this.businessAnalyst;",
            "      return _businessAnalyst;",
            "    }",
            "  }",
            "",
            "  public static final class BusinessAnalystBuilder {",
            "    private BusinessAnalystBuilder() {",
            "      throw new UnsupportedOperationException(\"no instances\");",
            "    }",
            "    public interface Name { Notes name(String name); }",
            "    public interface Notes {",
            "      BusinessAnalyst notes(Iterable<? extends String> notes);",
            "    }",
            "  }",
            "",
            "  public static final class BusinessAnalystUpdater {",
            "    private final BusinessAnalyst businessAnalyst;",
            "    private BusinessAnalystUpdater() {",
            "      this.businessAnalyst = new BusinessAnalyst();",
            "    }",
            "",
            "    public BusinessAnalystUpdater name(String name) {",
            "      this.businessAnalyst.setName(name);",
            "      return this;",
            "    }",
            "",
            "    public BusinessAnalystUpdater notes(Iterable<? extends String> notes) {",
            "      this.businessAnalyst.getNotes().clear();",
            "      for (String string : notes) {",
            "        this.businessAnalyst.getNotes().add(string);",
            "      }",
            "      return this;",
            "    }",
            "",
            "    public BusinessAnalyst done() {",
            "      BusinessAnalyst _businessAnalyst = this.businessAnalyst;",
            "      return _businessAnalyst;",
            "    }",
            "  }",
            "}");
    assertAbout(javaSources()).that(ImmutableList.of(businessAnalyst))
        .processedWith(new ZeroProcessor())
        .compilesWithoutError()
        .and().generatesSources(expected);
  }
}
