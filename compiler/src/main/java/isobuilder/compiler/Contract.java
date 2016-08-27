package isobuilder.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.EnumSet;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.interfaceBuilder;
import static javax.lang.model.element.ElementKind.CONSTRUCTOR;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;

final class Contract {

  private final Target target;

  Contract(Target target) {
    this.target = target;
  }

  ImmutableList<ClassName> stepInterfaceNames() {
    ImmutableList.Builder<ClassName> specs = ImmutableList.builder();
    for (StepSpec spec : target.stepSpecs) {
      specs.add(spec.stepName);
    }
    return specs.build();
  }

  ImmutableList<TypeSpec> stepInterfaces() {
    ImmutableList.Builder<TypeSpec> specs = ImmutableList.builder();
    for (StepSpec spec : target.stepSpecs) {
      specs.add(spec.asInterface(target.typeModifiers(EnumSet.noneOf(Modifier.class))));
    }
    return specs.build();
  }

  TypeSpec updaterInterface() {
    MethodSpec buildMethod = methodBuilder("build")
        .returns(target.executableElement.getKind() == CONSTRUCTOR
            ? ClassName.get(target.typeElement)
            : TypeName.get(target.executableElement.getReturnType()))
        .addModifiers(PUBLIC, ABSTRACT)
        .build();
    return interfaceBuilder(target.contractUpdaterName())
        .addMethod(buildMethod)
        .addMethods(updateMethods())
        .addModifiers(target.typeModifiers(EnumSet.noneOf(Modifier.class)))
        .build();
  }

  private ImmutableList<MethodSpec> updateMethods() {
    ClassName updaterName = target.contractUpdaterName();
    ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
    for (StepSpec spec : target.stepSpecs) {
      builder.add(spec.asUpdaterInterfaceMethod(updaterName));
    }
    return builder.build();
  }


}
