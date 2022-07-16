package com.de.mo.annotation;

import com.de.mo.lib.annotations.DemoAnnotation;

@DemoAnnotation("DemoAnnotation1")
public class DemoAnnotation1 {
    void define2(){

    }

    @DemoAnnotation("DemoAnnotation1Inner")
    public static class DemoAnnotation1Inner{
        void define1Inner(){

        }
    }
}
