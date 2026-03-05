package com.warp.warp_backend.model.annotation.validator;

import com.warp.warp_backend.model.annotation.constraint.NotBlank;
import com.warp.warp_backend.model.exception.ValidationException;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class NotBlankValidator implements Validator {

  public void validate(Object object, Object annotation) {
    NotBlank notBlank = (NotBlank) annotation;
    if (isBlankString(object)) {
      throw new ValidationException(notBlank.errorCode());
    }
  }

  private boolean isBlankString(Object object) {
    return Objects.isNull(object) ||
        (object instanceof String && StringUtils.isBlank((String) object));
  }
}
