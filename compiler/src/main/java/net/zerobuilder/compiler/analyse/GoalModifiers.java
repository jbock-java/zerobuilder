package net.zerobuilder.compiler.analyse;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import net.zerobuilder.RecordBuilder;
import net.zerobuilder.Visibility;
import net.zerobuilder.compiler.generate.Access;

final class GoalModifiers {

  static Access getAccess(TypeElement tel) {
    RecordBuilder recordBuilder = tel.getAnnotation(RecordBuilder.class);
    Visibility visibility = recordBuilder.visibility();
    if (visibility == Visibility.PACKAGE) {
      return Access.PACKAGE;
    } else if (visibility == Visibility.PUBLIC) {
      return Access.PUBLIC;
    }
    if (tel.getModifiers().contains(Modifier.PUBLIC)) {
      return Access.PUBLIC;
    }
    return Access.PACKAGE;
  }

  private GoalModifiers() {
  }
}
