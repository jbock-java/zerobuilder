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
  public void businessTest() {
    JavaFileObject cube = forSourceLines("beans.BusinessAnalyst",
        "package beans;",
        "import net.zerobuilder.*;",
        "import java.util.List;",
        "import java.util.ArrayList;",
        "",
        "@Builders(recycle = true)",
        "@Goal(toBuilder = true)",
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
            "  private static final ThreadLocal<BusinessAnalystBuilders> INSTANCE = new ThreadLocal<BusinessAnalystBuilders>() {",
            "    @Override",
            "    protected BusinessAnalystBuilders initialValue() {",
            "      return new BusinessAnalystBuilders();",
            "    }",
            "  }",
            "",
            "  private final BusinessAnalystUpdater businessAnalystUpdater = new BusinessAnalystUpdater();",
            "  private final BusinessAnalystBuilderImpl businessAnalystBuilderImpl = new BusinessAnalystBuilderImpl();",
            "  private BusinessAnalystBuilders() {}",
            "",
            "  public static BusinessAnalystBuilder.Name businessAnalystBuilder() {",
            "    BusinessAnalystBuilderImpl businessAnalystBuilderImpl = INSTANCE.get().businessAnalystBuilderImpl;",
            "    businessAnalystBuilderImpl.businessAnalyst = new BusinessAnalyst();",
            "    return businessAnalystBuilderImpl;",
            "  }",
            "",
            "  public static BusinessAnalystUpdater businessAnalystToBuilder(BusinessAnalyst businessAnalyst) {",
            "    BusinessAnalystUpdater updater = INSTANCE.get().businessAnalystUpdater;",
            "    updater.businessAnalyst = new BusinessAnalyst();",
            "    updater.businessAnalyst.setName(businessAnalyst.getName());",
            "    for (String v : businessAnalyst.getNotes()) {",
            "      updater.businessAnalyst.getNotes().add(v);",
            "    }",
            "    return updater;",
            "  }",
            "",
            "  public static final class BusinessAnalystUpdater {",
            "    BusinessAnalyst businessAnalyst;",
            "    private BusinessAnalystUpdater() {}",
            "",
            "    public BusinessAnalystUpdater name(String name) {",
            "      this.businessAnalyst.setName(name);",
            "      return this;",
            "    }",
            "",
            "    public BusinessAnalystUpdater notes(Iterable<? extends String> notes) {",
            "      this.businessAnalyst.getNotes().clear();",
            "      for (String v : notes) {",
            "        this.businessAnalyst.getNotes().add(v);",
            "      }",
            "      return this;",
            "    }",
            "",
            "    public BusinessAnalystUpdater notes(String notes) {",
            "      this.businessAnalyst.getNotes().clear();",
            "      this.businessAnalyst.getNotes().add(notes);",
            "      return this;",
            "    }",
            "",
            "    public BusinessAnalyst build() { return businessAnalyst; }",
            "  }",
            "",
            "  static final class BusinessAnalystBuilderImpl",
            "        implements BusinessAnalystBuilder.Name, BusinessAnalystBuilder.Notes {",
            "    BusinessAnalyst businessAnalyst;",
            "    private BusinessAnalystBuilderImpl() {}",
            "",
            "    @Override public BusinessAnalystBuilder.Notes name(String name) {",
            "      this.businessAnalyst.setName(name);",
            "      return this;",
            "    }",
            "",
            "    @Override public BusinessAnalyst notes(Iterable<? extends String> notes) {",
            "      for (String v : notes) {",
            "        this.businessAnalyst.getNotes().add(v);",
            "      }",
            "      return businessAnalyst;",
            "    }",
            "",
            "    @Override public BusinessAnalyst notes(String notes) {",
            "      this.businessAnalyst.getNotes().add(notes);",
            "      return businessAnalyst;",
            "    }",
            "  }",
            "",
            "  public static final class BusinessAnalystBuilder {",
            "    private BusinessAnalystBuilder() {}",
            "    public interface Name { Notes name(String name); }",
            "    public interface Notes {",
            "      BusinessAnalyst notes(Iterable<? extends String> notes);",
            "      BusinessAnalyst notes(String notes);",
            "    }",
            "  }",
            "}");
    assertAbout(javaSources()).that(ImmutableList.of(cube))
        .processedWith(new ZeroProcessor())
        .compilesWithoutError()
        .and().generatesSources(expected);
  }

}
