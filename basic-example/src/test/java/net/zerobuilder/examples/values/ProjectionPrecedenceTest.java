package net.zerobuilder.examples.values;

import net.zerobuilder.examples.values.ProjectionPrecedence.AutoGetter;
import net.zerobuilder.examples.values.ProjectionPrecedence.BoolGetter;
import net.zerobuilder.examples.values.ProjectionPrecedence.Getter;
import net.zerobuilder.examples.values.ProjectionPrecedence.InheritedField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectionPrecedenceTest {

  @Test
  void testField() {
    InheritedField foo = new InheritedField("foo");
    InheritedField bar = ProjectionPrecedence_InheritedFieldBuilders.builder(foo).foo("bar").build();
    assertEquals("bar", bar.foo);
  }

  @Test
  void testGetter() {
    Getter foo = new Getter("foo");
    Getter bar = ProjectionPrecedence_GetterBuilders.builder(foo).foo("bar").build();
    assertEquals("bar", bar.foo());
  }

  @Test
  void testAutoGetter() {
    AutoGetter foo = new AutoGetter("foo");
    AutoGetter bar = ProjectionPrecedence_AutoGetterBuilders.builder(foo).foo("bar").build();
    assertEquals("bar", bar.getFoo());
  }

  @Test
  void testBooleanGetter() {
    BoolGetter foo = new BoolGetter(false);
    BoolGetter bar = ProjectionPrecedence_BoolGetterBuilders.builder(foo).foo(true).build();
    assertTrue(bar.isFoo());
  }
}
