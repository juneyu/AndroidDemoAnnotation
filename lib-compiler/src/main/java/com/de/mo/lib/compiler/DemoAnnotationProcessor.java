package com.de.mo.lib.compiler;

import com.de.mo.lib.annotations.Constant;
import com.de.mo.lib.annotations.DemoAnnotation;
import com.google.auto.common.SuperficialValidation;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import net.ltgt.gradle.incap.IncrementalAnnotationProcessor;
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.DYNAMIC)
public class DemoAnnotationProcessor extends AbstractProcessor {

    private Filer filer;
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        for (Class<? extends Annotation> annotation : getSupportedAnnotations()) {
            if (annotation == null) {
                continue;
            }
            String canonicalName = annotation.getCanonicalName();
            if (canonicalName == null) {
                continue;
            }
            types.add(canonicalName);
        }
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (!set.isEmpty()) {
            MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder(Constant.METHOD_NAME)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(void.class);
            Map<TypeElement, DemoAnnotationSet> demoAnnoationMap = findAndParseTargets(roundEnvironment);
            for (Map.Entry<TypeElement, DemoAnnotationSet> entry : demoAnnoationMap.entrySet()) {
                TypeElement typeElement = entry.getKey();
                DemoAnnotationSet binding = entry.getValue();
                ClassName bindingClassName = binding.getBindingClassName();
                note("DemoAnnotationProcessor process :" + bindingClassName);
                methodSpecBuilder.beginControlFlow("try")
                        .addStatement("$T.forName($S)", Class.class, bindingClassName)
                        .nextControlFlow("catch ($T e)", Exception.class)
                        .addStatement("e.printStackTrace()")
                        .addStatement("$T.out.println($S)", System.class, bindingClassName + " loaded failed!")
                        .endControlFlow();
            }
            MethodSpec methodSpec = methodSpecBuilder.build();
            TypeSpec typeSpec = TypeSpec.classBuilder(Constant.CLASS_NAME)
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addMethod(methodSpec)
                    .addJavadoc("load class")
                    .build();
            JavaFile javaFile = JavaFile.builder(Constant.PACKAGE_NAME, typeSpec)
                    .build();
            try {
                javaFile.writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
                error("Unable to write DemoAnnotation :", e.getMessage());
            }
        }
        return false;
    }

    private Set<Class<? extends Annotation>> getSupportedAnnotations() {
        Set<Class<? extends Annotation>> annotations = new LinkedHashSet<>();
        annotations.add(DemoAnnotation.class);
        return annotations;
    }

    private Map<TypeElement, DemoAnnotationSet> findAndParseTargets(RoundEnvironment roundEnvironment) {
        Map<TypeElement, DemoAnnotationSet.Builder> builderMap = new LinkedHashMap<>();
        Set<TypeElement> erasedTargetNames = new LinkedHashSet<>();
        // Process each @DemoAnnotation element.
        for (Element element : roundEnvironment.getElementsAnnotatedWith(DemoAnnotation.class)) {
            if (element == null) {
                continue;
            }
            if (!SuperficialValidation.validateElement(element)) {
                continue;
            }
            try {
                parseKindClass(element, builderMap, erasedTargetNames);
            } catch (Exception e) {
                logParsingError(element, DemoAnnotation.class, e);
            }
        }
        Deque<Map.Entry<TypeElement, DemoAnnotationSet.Builder>> entries =
                new ArrayDeque<>(builderMap.entrySet());
        Map<TypeElement, DemoAnnotationSet> bindingMap = new LinkedHashMap<>();
        while (!entries.isEmpty()) {
            Map.Entry<TypeElement, DemoAnnotationSet.Builder> entry = entries.removeFirst();
            TypeElement type = entry.getKey();
            DemoAnnotationSet.Builder builder = entry.getValue();
            bindingMap.put(type, builder.build());
        }
        return bindingMap;
    }

    private void parseKindClass(Element element,
                                Map<TypeElement, DemoAnnotationSet.Builder> builderMap, Set<TypeElement> erasedTargetNames) {
        if (element.getKind() == ElementKind.CLASS) {
            boolean hasError = false;
            TypeElement enclosingElement = (TypeElement) element;
            hasError |= isBindingInWrongPackage(DemoAnnotation.class, element);
            if (hasError) {
                return;
            }
            getOrCreateBindingBuilder(builderMap, enclosingElement);
            erasedTargetNames.add(enclosingElement);
        }
    }

    private DemoAnnotationSet.Builder getOrCreateBindingBuilder(
            Map<TypeElement, DemoAnnotationSet.Builder> builderMap, TypeElement enclosingElement) {
        DemoAnnotationSet.Builder builder = builderMap.get(enclosingElement);
        if (builder == null) {
            builder = DemoAnnotationSet.newBuilder(enclosingElement);
            builderMap.put(enclosingElement, builder);
        }
        return builder;
    }

    private boolean isBindingInWrongPackage(Class<? extends Annotation> annotationClass,
                                            Element element) {
        TypeElement enclosingElement = (TypeElement) element;
        String qualifiedName = enclosingElement.getQualifiedName().toString();

        if (qualifiedName.startsWith("android.") || qualifiedName.startsWith("androidx.")) {
            error(element, "@%s-annotated class incorrectly in Android framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }
        if (qualifiedName.startsWith("java.")) {
            error(element, "@%s-annotated class incorrectly in Java framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }

        return false;
    }

    private void logParsingError(Element element, Class<? extends Annotation> annotation,
                                 Exception e) {
        StringWriter stackTrace = new StringWriter();
        e.printStackTrace(new PrintWriter(stackTrace));
        error(element, "Unable to parse @%s annotation.\n\n%s", annotation.getSimpleName(), stackTrace);
    }

    private void error(Element element, String message, Object... args) {
        printMessage(Diagnostic.Kind.ERROR, element, message, args);
    }

    private void error(String message, Object... args) {
        printMessage(Diagnostic.Kind.ERROR, message, args);
    }

    private void note(String message, Object... args) {
        printMessage(Diagnostic.Kind.NOTE, message, args);
    }

    private void printMessage(Diagnostic.Kind kind, Element element, String message, Object[] args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(kind, message, element);
    }


    private void printMessage(Diagnostic.Kind kind, String message, Object[] args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(kind, message);
    }
}