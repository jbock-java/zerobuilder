package isobuilder.compiler;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import static com.google.common.collect.ImmutableList.copyOf;

@AutoValue
abstract class FactoryInfo {

  abstract TypeElement sourceType();
  abstract ImmutableList<VariableElement> parameters();

  static FactoryInfo factoryInfo(TypeElement sourceType, Iterable<? extends VariableElement> parameters) {
    return new AutoValue_FactoryInfo(sourceType, copyOf(parameters));
  }

}
