package net.zerobuilder.compiler.analyse;

import org.junit.Test;

import static net.zerobuilder.compiler.analyse.ProjectionValidatorB.IS_GETTER_NAME;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProjectionValidatorBTest {


  @Test
  public void getterName() {
    assertTrue(IS_GETTER_NAME.test("isFoo"));
    assertTrue(IS_GETTER_NAME.test("getFoo"));
    assertFalse(IS_GETTER_NAME.test("getfoo"));
    assertFalse(IS_GETTER_NAME.test("isfoo"));
    assertFalse(IS_GETTER_NAME.test("is99"));
    assertFalse(IS_GETTER_NAME.test("foobar"));
  }

}