package ru.hse.lab2.observability;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Запасная аннотация: можно было бы читать value() из аспекта; сейчас HTTP именуют через интерсептор.
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ObservedOperation {
    String value(); // логическое имя операции, если понадобится кастом вместо controller.Class.method
}
