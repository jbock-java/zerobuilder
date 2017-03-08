package net.zerobuilder.examples.values;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ProjectionPrecedenceTest {

  @Test
  public void testField() {
    ProjectionPrecedence.InheritedField foo = new ProjectionPrecedence.InheritedField("foo");
    ProjectionPrecedence.InheritedField bar = ProjectionPrecedence_InheritedFieldBuilders.inheritedFieldUpdater(foo).foo("bar").done();
    assertThat(bar.foo, is("bar"));
  }

  @Test
  public void testGetter() {
    ProjectionPrecedence.Getter foo = new ProjectionPrecedence.Getter("foo");
    ProjectionPrecedence.Getter bar = ProjectionPrecedence_GetterBuilders.getterUpdater(foo).foo("bar").done();
    assertThat(bar.foo(), is("bar"));
  }

  @Test
  public void testAutoGetter() {
    ProjectionPrecedence.AutoGetter foo = new ProjectionPrecedence.AutoGetter("foo");
    ProjectionPrecedence.AutoGetter bar = ProjectionPrecedence_AutoGetterBuilders.autoGetterUpdater(foo).foo("bar").done();
    assertThat(bar.getFoo(), is("bar"));
  }

  @Test
  public void testBooleanGetter() {
    ProjectionPrecedence.BoolGetter foo = new ProjectionPrecedence.BoolGetter(false);
    ProjectionPrecedence.BoolGetter bar = ProjectionPrecedence_BoolGetterBuilders.boolGetterUpdater(foo).foo(true).done();
    assertThat(bar.isFoo(), is(true));
  }
}