package com.de.mo.lib.compiler;

import static com.google.auto.common.MoreElements.getPackage;

import com.squareup.javapoet.ClassName;

import javax.lang.model.element.TypeElement;

public final class DemoAnnotationSet{

    private final ClassName bindingClassName;
    private final TypeElement enclosingElement;

    private DemoAnnotationSet(ClassName bindingClassName, TypeElement enclosingElement) {
        this.bindingClassName = bindingClassName;
        this.enclosingElement = enclosingElement;
    }

    static Builder newBuilder(TypeElement enclosingElement) {
        ClassName bindingClassName = getAnnotationClassName(enclosingElement);
        return new Builder(bindingClassName, enclosingElement);
    }

    static ClassName getAnnotationClassName(TypeElement typeElement) {
        String packageName = getPackage(typeElement).getQualifiedName().toString();
        String className = typeElement.getQualifiedName().toString().substring(
                packageName.length() + 1).replace('.', '$');
        return ClassName.get(packageName, className);
    }

    public ClassName getBindingClassName() {
        return bindingClassName;
    }

    static final class Builder {
        private final ClassName bindingClassName;
        private final TypeElement enclosingElement;

        private Builder(ClassName bindingClassName, TypeElement enclosingElement) {
            this.bindingClassName = bindingClassName;
            this.enclosingElement = enclosingElement;
        }

        DemoAnnotationSet build() {
            return new DemoAnnotationSet(bindingClassName, enclosingElement);
        }
    }

}
