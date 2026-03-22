package com.warp.warp_backend.model.annotation.validator;

import com.warp.warp_backend.model.annotation.constraint.MaxLength;
import com.warp.warp_backend.model.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class MaxLengthValidator implements Validator {

  public void validate(Object object, Object annotation) {
    MaxLength maxLength = (MaxLength) annotation;
    if (Objects.isNull(object)) {
      return;
    }
    if (object instanceof String str && str.length() > maxLength.value()) {
      throw new ValidationException(maxLength.errorCode());
    }
  }
}
