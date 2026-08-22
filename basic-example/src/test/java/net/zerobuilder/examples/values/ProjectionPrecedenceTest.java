package net.zerobuilder.examples.values;

import net.zerobuilder.examples.values.ProjectionPrecedence.AutoGetter;
import net.zerobuilder.examples.values.ProjectionPrecedence.BoolGetter;
import net.zerobuilder.examples.values.ProjectionPrecedence.Getter;
import net.zerobuilder.examples.values.ProjectionPrecedence.InheritedField;
import org.junit.jupiter.api.Test;

import static net.zerobuilder.examples.values.ProjectionPrecedence_AutoGetterBuilders.autoGetterUpdater;
import static net.zerobuilder.examples.values.ProjectionPrecedence_BoolGetterBuilders.boolGetterUpdater;
import static net.zerobuilder.examples.values.ProjectionPrecedence_GetterBuilders.getterUpdater;
import static net.zerobuilder.examples.values.ProjectionPrecedence_InheritedFieldBuilders.inheritedFieldUpdater;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectionPrecedenceTest {

  @Test
  void testField() {
    InheritedField foo = new InheritedField("foo");
    InheritedField bar = inheritedFieldUpdater(foo).foo("bar").build();
    assertEquals("bar", bar.foo);
  }

  @Test
  void testGetter() {
    Getter foo = new Getter("foo");
    Getter bar = getterUpdater(foo).foo("bar").build();
    assertEquals("bar", bar.foo());
  }

  @Test
  void testAutoGetter() {
    AutoGetter foo = new AutoGetter("foo");
    AutoGetter bar = autoGetterUpdater(foo).foo("bar").build();
    assertEquals("bar", bar.getFoo());
  }

  @Test
  void testBooleanGetter() {
    BoolGetter foo = new BoolGetter(false);
    BoolGetter bar = boolGetterUpdater(foo).foo(true).build();
    assertTrue(bar.isFoo());
  }
}
