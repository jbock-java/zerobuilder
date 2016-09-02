package net.zerobuilder.compiler;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import net.zerobuilder.compiler.MatchValidator.ProjectionInfo;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.Set;

import static com.google.common.base.Optional.absent;
import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.anonymousClassBuilder;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.Messages.JavadocMessages.JAVADOC_BUILDER;
import static net.zerobuilder.compiler.Util.downcase;
import static net.zerobuilder.compiler.Util.joinCodeBlocks;
import static net.zerobuilder.compiler.Util.upcase;

final class MyContext implements GenerationContext {

  private static final String STATIC_FIELD_INSTANCE = "INSTANCE";
  private static final String FIELD_UPDATER = "updater";
  private static final String FIELD_STEPS = "steps";

  private static final String UPDATER_SUFFIX = "Updater";
  private static final String CONTRACT = "Contract";
  private static final String STEPS_IMPL = "StepsImpl";

  /**
   * return type of {@link #goal}
   */
  final TypeName goalType;

  final boolean toBuilder;

  /**
   * the element carrying the {@link net.zerobuilder.Build} annotation
   */
  final TypeElement buildElement;

  /**
   * the element carrying the {@link net.zerobuilder.Build.Goal} annotation
   */
  final ExecutableElement goal;

  /**
   * arguments of the {@link #goal}
   */
  final ImmutableList<StepSpec> stepSpecs;

  final boolean nogc;

  private MyContext(TypeName goalType, boolean toBuilder, TypeElement buildElement,
                    ExecutableElement goal,
                    ImmutableList<StepSpec> stepSpecs, boolean nogc) {
    this.goalType = goalType;
    this.toBuilder = toBuilder;
    this.buildElement = buildElement;
    this.goal = goal;
    this.stepSpecs = stepSpecs;
    this.nogc = nogc;
  }

  static MyContext createContext(TypeName goalType, boolean toBuilder, TypeElement buildElement,
                                 ImmutableList<ProjectionInfo> projectionInfos,
                                 ExecutableElement goal,
                                 boolean nogc) {
    ImmutableList<StepSpec> specs = specs(buildElement, goalType, projectionInfos);
    return new MyContext(goalType, toBuilder, buildElement, goal, specs, nogc);
  }

  private static ImmutableList<StepSpec> specs(TypeElement buildElement, TypeName goalType,
                                               ImmutableList<ProjectionInfo> goal) {
    ClassName contractName = generatedClassName(buildElement).nestedClass(CONTRACT);
    ImmutableList.Builder<StepSpec> stepSpecsBuilder = ImmutableList.builder();
    for (int i = goal.size() - 1; i >= 0; i--) {
      ProjectionInfo arg = goal.get(i);
      ClassName stepName = contractName.nestedClass(
          upcase(arg.parameter.getSimpleName().toString()));
      StepSpec stepSpec = StepSpec.stepSpec(stepName, arg, goalType);
      stepSpecsBuilder.add(stepSpec);
      goalType = stepSpec.stepContractType;
    }
    return stepSpecsBuilder.build().reverse();
  }

  private static ClassName generatedClassName(TypeElement typeElement) {
    ClassName sourceType = ClassName.get(typeElement);
    String simpleName = Joiner.on('_').join(sourceType.simpleNames()) + "Builder";
    return sourceType.topLevelClassName().peerClass(simpleName);
  }

  ImmutableList<ClassName> stepInterfaceNames() {
    ImmutableList.Builder<ClassName> specs = ImmutableList.builder();
    for (StepSpec spec : stepSpecs) {
      specs.add(spec.stepContractType);
    }
    return specs.build();
  }

  ImmutableList<TypeName> thrownTypes() {
    return FluentIterable.from(goal.getThrownTypes())
        .transform(new Function<TypeMirror, TypeName>() {
          @Override
          public TypeName apply(TypeMirror thrownType) {
            return TypeName.get(thrownType);
          }
        })
        .toList();
  }

  Optional<ClassName> receiver() {
    return goal.getKind() == METHOD && !goal.getModifiers().contains(STATIC)
        ? Optional.of(ClassName.get(buildElement))
        : Optional.<ClassName>absent();
  }

  Optional<FieldSpec> threadLocalField() {
    if (!nogc) {
      return absent();
    }
    ClassName generatedTypeName = generatedTypeName();
    TypeName threadLocal = ParameterizedTypeName.get(ClassName.get(ThreadLocal.class),
        generatedTypeName);
    MethodSpec initialValue = methodBuilder("initialValue")
        .addAnnotation(Override.class)
        .addModifiers(PROTECTED)
        .returns(generatedTypeName)
        .addStatement("return new $T()", generatedTypeName)
        .build();
    return Optional.of(FieldSpec.builder(threadLocal, STATIC_FIELD_INSTANCE)
        .initializer("$L", anonymousClassBuilder("")
            .addSuperinterface(threadLocal)
            .addMethod(initialValue)
            .build())
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build());
  }

  Optional<MethodSpec> toBuilderMethod() {
    if (!toBuilder) {
      return absent();
    }
    String parameterName = downcase(ClassName.get(buildElement).simpleName());
    MethodSpec.Builder builder = methodBuilder("toBuilder")
        .addParameter(goalType, parameterName);
    String varUpdater = "updater";
    ClassName updaterType = updaterContext().typeName();
    if (nogc) {
      builder.addStatement("$T $L = $L.get().$N", updaterType, varUpdater,
          STATIC_FIELD_INSTANCE, FIELD_UPDATER);
    } else {
      builder.addStatement("$T $L = new $T()", updaterType, varUpdater,
          updaterType);
    }
    for (StepSpec stepSpec : stepSpecs) {
      if (stepSpec.projectionMethodName.isPresent()) {
        builder.addStatement("$N.$N = $N.$N()", varUpdater, stepSpec.parameter.getSimpleName(),
            parameterName, stepSpec.projectionMethodName.get());
      } else {
        builder.addStatement("$N.$N = $N.$N", varUpdater, stepSpec.parameter.getSimpleName(),
            parameterName, stepSpec.parameter.getSimpleName());
      }
    }
    builder.addStatement("return $L", varUpdater);
    return Optional.of(builder
        .returns(contractUpdaterName())
        .addModifiers(maybeAddPublic(STATIC)).build());
  }

  MethodSpec builderMethod() {
    StepSpec firstStep = stepSpecs.get(0);
    Optional<ClassName> maybeReceiver = receiver();
    MethodSpec.Builder builder = methodBuilder("builder");
    if (maybeReceiver.isPresent()) {
      ClassName receiver = maybeReceiver.get();
      builder.addParameter(ParameterSpec.builder(receiver,
          downcase(receiver.simpleName())).build());
      if (nogc) {
        builder.addStatement("$T $N = $N.get().$N", stepsImplTypeName(),
            downcase(stepsImplTypeName().simpleName()), STATIC_FIELD_INSTANCE, FIELD_STEPS);
      } else {
        builder.addStatement("$T $N = new $T()", stepsImplTypeName(),
            downcase(stepsImplTypeName().simpleName()), stepsImplTypeName());
      }
      builder.addStatement("$N.$N = $N",
          downcase(stepsImplTypeName().simpleName()),
          "_" + downcase(receiver.simpleName()),
          downcase(receiver.simpleName()));
      builder.addStatement("return $N",
          downcase(stepsImplTypeName().simpleName()));
    } else {
      if (nogc) {
        builder.addStatement("return $N.get().$N", STATIC_FIELD_INSTANCE, FIELD_STEPS);
      } else {
        builder.addStatement("return new $T()", stepsImplTypeName());
      }
    }
    return builder.returns(firstStep.stepContractType)
        .addJavadoc(JAVADOC_BUILDER, ClassName.get(buildElement))
        .addModifiers(maybeAddPublic(STATIC))
        .build();
  }

  ImmutableList<FieldSpec> instanceFields() {
    if (!nogc) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
    if (toBuilder) {
      builder.add(FieldSpec.builder(updaterContext().typeName(),
          "updater", PRIVATE, FINAL).build());
    }
    builder.add(FieldSpec.builder(stepsImplTypeName(),
        "steps", PRIVATE, FINAL).build());
    return builder.build();
  }

  MethodSpec constructor() {
    MethodSpec.Builder builder = constructorBuilder();
    if (nogc && toBuilder) {
      builder.addStatement("this.$L = new $T()", FIELD_UPDATER, updaterContext().typeName());
    }
    if (nogc) {
      builder.addStatement("this.$L = new $T()", FIELD_STEPS, stepsImplTypeName());
    }
    return builder.addModifiers(PRIVATE).build();
  }


  @Override
  public ClassName generatedTypeName() {
    return generatedClassName(buildElement);
  }

  ClassName contractName() {
    return generatedTypeName().nestedClass(CONTRACT);
  }

  ClassName stepsImplTypeName() {
    return generatedTypeName().nestedClass(STEPS_IMPL);
  }

  ClassName contractUpdaterName() {
    return contractName()
        .nestedClass(ClassName.get(buildElement).simpleName() + UPDATER_SUFFIX);
  }

  StepsContext stepsContext() {
    return new StepsContext(this);
  }

  UpdaterContext updaterContext() {
    return new UpdaterContext(this);
  }

  ContractContext contractContext() {
    return new ContractContext(this);
  }

  CodeBlock factoryCallArgs() {
    ImmutableList.Builder<CodeBlock> builder = ImmutableList.builder();
    for (VariableElement arg : goal.getParameters()) {
      builder.add(CodeBlock.of("$L", arg.getSimpleName()));
    }
    return joinCodeBlocks(builder.build(), ", ");
  }

  Set<Modifier> maybeAddPublic(Modifier... modifiers) {
    ImmutableSet<Modifier> modifierSet = ImmutableSet.copyOf(modifiers);
    if (goal.getModifiers().contains(PUBLIC)
        && !modifierSet.contains(PUBLIC)) {
      return new ImmutableSet.Builder<Modifier>().addAll(modifierSet).add(PUBLIC).build();
    }
    return modifierSet;
  }

}
