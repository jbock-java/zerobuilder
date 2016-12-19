package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

public class HardGenericsTest {

  @Test
  public void staticMethod() {
    JavaFileObject apexFactory = forSourceLines("foo.Foo",
        "package foo;",
        "import net.zerobuilder.Builder;",
        "import net.zerobuilder.Updater;",
        "",
        "final class Foo<A, B> {",
        "  final B ab0;",
        "  final B ab1;",
        "  final A aa0;",
        "  final B ab2;",
        "  Foo(B ab0, B ab1, A aa0, B ab2) {",
        "    this.ab0 = ab0;",
        "    this.ab1 = ab1;",
        "    this.aa0 = aa0;",
        "    this.ab2 = ab2;",
        "  }",
        "",
        "  @Builder",
        "  <C extends B> Bar<A, B, C> bar(C bc0, java.util.Map<A, B> map0, java.util.Map<A, B> map1) {",
        "    return new Bar<>(ab0, ab1, aa0, ab2, bc0, map0, map1);",
        "  }",
        "",
        "  static final class Bar<A, B, C extends B> {",
        "    final B ab0;",
        "    final B ab1;",
        "    final A aa0;",
        "    final B ab2;",
        "    final C bc0;",
        "    final java.util.Map<A, B> map0;",
        "    final java.util.Map<A, B> map1;",
        "    Bar(B ab0, B ab1, A aa0, B ab2, C bc0, java.util.Map<A, B> map0, java.util.Map<A, B> map1) {",
        "      this.ab0 = ab0;",
        "      this.ab1 = ab1;",
        "      this.aa0 = aa0;",
        "      this.ab2 = ab2;",
        "      this.bc0 = bc0;",
        "      this.map0 = map0;",
        "      this.map1 = map1;",
        "    }",
        "  }",
        "}");
    assertAbout(javaSources()).that(ImmutableList.of(apexFactory))
        .processedWith(new ZeroProcessor())
        .compilesWithoutError();
  }
}
