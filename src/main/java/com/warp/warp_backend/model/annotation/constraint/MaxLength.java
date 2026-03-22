package com.warp.warp_backend.model.annotation.constraint;

import com.warp.warp_backend.model.annotation.validator.MaxLengthValidator;
import com.warp.warp_backend.model.common.ErrorCode;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({PARAMETER, FIELD})
public @interface MaxLength {

  int value();

  Class<?> validationServiceClass() default MaxLengthValidator.class;

  ErrorCode errorCode();
}
