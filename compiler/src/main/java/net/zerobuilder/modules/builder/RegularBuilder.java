package net.zerobuilder.modules.builder;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import com.palantir.javapoet.TypeVariableName;
import io.jbock.simple.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import net.zerobuilder.HasBuildMethod;
import net.zerobuilder.compiler.generate.GoalDescription;
import net.zerobuilder.compiler.generate.ModuleOutput;

import static com.palantir.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.generate.ZeroUtil.parameterizedTypeName;

public record RegularBuilder(
    Builder builder,
    GoalDescription description,
    BuilderUtil util,
    Step step,
    BuilderMethod builderMethod) {

  @Inject
  public RegularBuilder {
  }

  private List<TypeSpec> stepInterfaces() {
    return IntStream.range(0, description.parameters().size())
        .mapToObj(step::stepInterface)
        .toList();
  }

  private List<MethodSpec> steps() {
    return IntStream.range(0, description.parameters().size())
        .mapToObj(builder::createStepMethod)
        .toList();
  }

  private TypeSpec defineBuilderImpl() {
    return classBuilder(util.implType())
        .addTypeVariables(description.details().instanceTypeParameters())
        .addSuperinterfaces(stepInterfaceTypes())
        .addSuperinterface(ParameterizedTypeName.get(ClassName.get(HasBuildMethod.class),
            description.details().goalType()))
        .addFields(builder.fields())
        .addMethods(steps())
        .addMethod(MethodSpec.methodBuilder("build")
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .addCode(builder.constructorCall())
            .returns(description.details().goalType())
            .build())
        .addModifiers(PRIVATE, STATIC).build();
  }

  private List<TypeName> stepInterfaceTypes() {
    List<TypeVariableName> typeVars = description.details().instanceTypeParameters();
    return IntStream.range(0, description.parameters().size()).mapToObj(util::stepType)
        .map(type -> parameterizedTypeName(type, typeVars))
        .toList();
  }

  ModuleOutput process() {
    List<TypeSpec> steps = stepInterfaces();
    List<TypeSpec> typeSpecs = new ArrayList<>(steps.size() + 2);
    typeSpecs.add(defineBuilderImpl());
    typeSpecs.addAll(steps);
    return new ModuleOutput(List.of(builderMethod.builderMethod()), typeSpecs);
  }
}
