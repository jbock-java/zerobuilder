package net.zerobuilder.compiler.generate;

import net.zerobuilder.RecordBuilder;
import net.zerobuilder.Visibility;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public enum Access {
  PUBLIC,
  PACKAGE,
  ;

  public static Access getAccess(TypeElement tel) {
    RecordBuilder recordBuilder = tel.getAnnotation(RecordBuilder.class);
    if (recordBuilder != null) {
      Visibility visibility = recordBuilder.visibility();
      if (visibility == Visibility.PACKAGE) {
        return Access.PACKAGE;
      } else if (visibility == Visibility.PUBLIC) {
        return Access.PUBLIC;
      }
    }
    if (tel.getModifiers().contains(Modifier.PUBLIC)) {
      return Access.PUBLIC;
    }
    return Access.PACKAGE;
  }
}
