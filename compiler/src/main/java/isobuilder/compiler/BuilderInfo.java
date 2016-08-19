package isobuilder.compiler;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import static com.google.common.collect.ImmutableList.copyOf;

@AutoValue
abstract class BuilderInfo {

  abstract TypeElement sourceType();
  abstract ImmutableList<VariableElement> parameters();

  static BuilderInfo factoryInfo(TypeElement sourceType, Iterable<? extends VariableElement> parameters) {
    return new AutoValue_BuilderInfo(sourceType, copyOf(parameters));
  }

}
