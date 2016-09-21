package net.zerobuilder.compiler;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.zerobuilder.compiler.GoalContext.GoalCases;
import net.zerobuilder.compiler.GoalContext.GoalFunction;
import net.zerobuilder.compiler.GoalContextFactory.GoalKind;
import net.zerobuilder.compiler.ParameterContext.BeansParameterContext;
import net.zerobuilder.compiler.ParameterContext.ExecutableParameterContext;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static com.squareup.javapoet.WildcardTypeName.subtypeOf;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static net.zerobuilder.compiler.GoalContext.always;
import static net.zerobuilder.compiler.GoalContextFactory.GoalKind.INSTANCE_METHOD;
import static net.zerobuilder.compiler.ParameterContext.maybeIterationNullCheck;
import static net.zerobuilder.compiler.ParameterContext.maybeNullCheck;
import static net.zerobuilder.compiler.Utilities.downcase;
import static net.zerobuilder.compiler.Utilities.parameterSpec;
import static net.zerobuilder.compiler.Utilities.upcase;

final class UpdaterContext {

  static final GoalCases<ClassName> typeName = always(new GoalFunction<ClassName>() {
    @Override
    public ClassName apply(GoalContext goal, TypeName goalType, ImmutableList<? extends ParameterContext> parameters) {
      return goal.config.generatedType.nestedClass(upcase(goal.goalName + "Updater"));
    }
  });

  private static final GoalCases<ImmutableList<FieldSpec>> fields = new GoalCases<ImmutableList<FieldSpec>>() {
    @Override
    ImmutableList<FieldSpec> executableGoal(GoalContext goal, TypeName goalType, GoalKind kind,
                                            ImmutableList<ExecutableParameterContext> parameters,
                                            ImmutableList<TypeName> thrownTypes) {
      ImmutableList.Builder<FieldSpec> builder = ImmutableList.builder();
      if (kind == INSTANCE_METHOD) {
        ClassName receiverType = goal.config.annotatedType;
        builder.add(FieldSpec.builder(receiverType, '_' + downcase(receiverType.simpleName()), PRIVATE).build());
      }
      for (ExecutableParameterContext parameter : parameters) {
        String name = parameter.parameter.name;
        TypeName type = parameter.parameter.type;
        builder.add(FieldSpec.builder(type, name, PRIVATE).build());
      }
      return builder.build();
    }
    @Override
    ImmutableList<FieldSpec> beanGoal(GoalContext goal, ClassName goalType, ImmutableList<BeansParameterContext> parameters) {
      FieldSpec field = FieldSpec.builder(goalType, downcase(goalType.simpleName()))
          .build();
      return ImmutableList.of(field);
    }
  };


  private static final GoalCases<ImmutableList<MethodSpec>> updateMethods = new GoalCases<ImmutableList<MethodSpec>>() {
    @Override
    ImmutableList<MethodSpec> executableGoal(GoalContext goal, TypeName goalType, GoalKind kind,
                                             ImmutableList<ExecutableParameterContext> parameters,
                                             ImmutableList<TypeName> thrownTypes) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (ExecutableParameterContext parameter : parameters) {
        String name = parameter.parameter.name;
        TypeName type = parameter.parameter.type;
        builder.add(methodBuilder(name)
            .returns(goal.accept(typeName))
            .addParameter(parameterSpec(type, name))
            .addCode(parameter.accept(maybeNullCheck))
            .addStatement("this.$N = $N", name, name)
            .addStatement("return this")
            .addModifiers(PUBLIC)
            .build());
      }
      return builder.build();
    }
    @Override
    ImmutableList<MethodSpec> beanGoal(GoalContext goal, ClassName goalType, ImmutableList<BeansParameterContext> parameters) {
      ImmutableList.Builder<MethodSpec> builder = ImmutableList.builder();
      for (BeansParameterContext parameter : parameters) {
        String name = parameter.accessorPair.name;
        TypeName type = parameter.accessorPair.type;
        if (parameter.accessorPair.collectionType.type.isPresent()) {
          TypeName collectionType = parameter.accessorPair.collectionType.type.get();
          ParameterizedTypeName iterable = ParameterizedTypeName.get(ClassName.get(Iterable.class),
              subtypeOf(collectionType));
          String iterationVarName = "v";
          builder.add(methodBuilder(name)
              .returns(goal.accept(typeName))
              .addParameter(parameterSpec(iterable, name))
              .addStatement("this.$N.$N().clear()", downcase(goalType.simpleName()),
                  parameter.accessorPair.projectionMethodName)
              .beginControlFlow("for ($T $N : $N)", collectionType, iterationVarName, name)
              .addCode(parameter.accept(maybeIterationNullCheck))
              .addStatement("this.$N.$N().add($N)", downcase(goalType.simpleName()),
                  parameter.accessorPair.projectionMethodName, iterationVarName)
              .endControlFlow()
              .addStatement("return this")
              .addModifiers(PUBLIC)
              .build());
          if (parameter.accessorPair.collectionType.allowShortcut) {
            builder.add(methodBuilder(name)
                .returns(goal.accept(typeName))
                .addParameter(parameterSpec(collectionType, name))
                .addCode(parameter.accept(maybeNullCheck))
                .addStatement("this.$N.$N().clear()", downcase(goalType.simpleName()),
                parameter.accessorPair.projectionMethodName)
                .addStatement("this.$N.$N().add($N)", downcase(goalType.simpleName()),
                    parameter.accessorPair.projectionMethodName, name)
                .addStatement("return this")
                .addModifiers(PUBLIC)
                .build());
          }
        } else {
          builder.add(methodBuilder(name)
              .returns(goal.accept(typeName))
              .addParameter(parameterSpec(type, name))
              .addStatement("this.$N.set$L($N)", downcase(goalType.simpleName()), upcase(name), name)
              .addStatement("return this")
              .addModifiers(PUBLIC)
              .build());
        }
      }
      return builder.build();
    }
  };

  private static final GoalCases<MethodSpec> buildMethod = new GoalCases<MethodSpec>() {
    @Override
    MethodSpec executableGoal(GoalContext goal, TypeName goalType, GoalKind kind,
                              ImmutableList<ExecutableParameterContext> parameters,
                              ImmutableList<TypeName> thrownTypes) {
      return methodBuilder("build")
          .addModifiers(PUBLIC)
          .returns(goalType)
          .addCode(goal.goalCall)
          .addExceptions(thrownTypes)
          .build();
    }
    @Override
    MethodSpec beanGoal(GoalContext goal, ClassName goalType, ImmutableList<BeansParameterContext> parameters) {
      return methodBuilder("build")
          .addModifiers(PUBLIC)
          .returns(goalType)
          .addCode(goal.goalCall)
          .build();
    }
  };

  static TypeSpec defineUpdater(GoalContext goal) {
    return classBuilder(goal.accept(typeName))
        .addFields(goal.accept(fields))
        .addMethods(goal.accept(updateMethods))
        .addMethod(goal.accept(buildMethod))
        .addModifiers(PUBLIC, FINAL, STATIC)
        .addMethod(constructorBuilder().addModifiers(PRIVATE).build())
        .build();
  }

  private UpdaterContext() {
    throw new UnsupportedOperationException("no instances");
  }
}
