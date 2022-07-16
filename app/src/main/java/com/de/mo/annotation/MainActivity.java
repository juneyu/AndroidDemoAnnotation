package com.de.mo.annotation;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.de.mo.lib.annotations.Constant;
import com.de.mo.lib.annotations.DemoAnnotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

@DemoAnnotation("MainActivity")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            Class testClass = Class.forName(Constant.PACKAGE_NAME + "." + Constant.CLASS_NAME);
            Method method = testClass.getMethod(Constant.METHOD_NAME, new Class[]{});
            method.invoke(null, new  Object[]{});
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            Log.e("MainActivity", Arrays.toString(e.getStackTrace()));
        }
    }
}