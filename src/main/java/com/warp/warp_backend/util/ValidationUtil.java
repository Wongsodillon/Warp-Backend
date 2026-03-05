package com.warp.warp_backend.util;

import com.warp.warp_backend.model.annotation.validator.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ValidationUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(ValidationUtil.class);

  private static final String SERVICE_CLASS_METHOD_NAME = "validationServiceClass";

  private static final List<String> EXCLUDE_PACKAGE_PREFIXES = Arrays.asList(
      "java", "org.apache"
  );

  @Autowired
  private BeanFactory beanFactory;

  public void validateArgument(Object argument, Parameter parameter) throws Exception {
    Annotation[] annotations = parameter.getDeclaredAnnotations();

    for (Annotation annotation : annotations) {
      callValidator(argument, annotation);
    }

    validateNestedFields(argument);
  }

  public void validateArgumentFields(List<Object> arguments) throws Exception {
    for (Object argument : arguments) {
      validateFields(getAllFields(argument.getClass()), argument);
    }
  }

  private Field[] getAllFields(Class<?> clazz) {
    Field[] declaredFields = clazz.getDeclaredFields();
    Class<?> superclass = clazz.getSuperclass();

    if (Objects.nonNull(superclass)) {
      return concat(getAllFields(superclass), declaredFields);
    }

    return declaredFields;
  }

  private Field[] concat(Field[] a, Field[] b) {
    Field[] result = new Field[a.length + b.length];

    System.arraycopy(a, 0, result, 0, a.length);
    System.arraycopy(b, 0, result, a.length, b.length);

    return result;
  }

  private void validateFields(Field[] fields, Object argument) throws Exception {
    for (Field field : fields) {
      validateField(field, argument);
    }
  }

  private void validateField(Field field, Object argument) throws Exception {
    if (!field.trySetAccessible()) {
      return;
    }
    Object needToValidate = field.get(argument);

    for (Annotation annotation : field.getDeclaredAnnotations()) {
      callValidator(needToValidate, annotation);
    }

    validateNestedFields(needToValidate);
  }

  private void validateNestedFields(Object fieldObj) throws Exception {
    if (isNeedToValidateFields(fieldObj)) {
      validateFields(fieldObj.getClass().getDeclaredFields(), fieldObj);
    }

    if (fieldObj instanceof Collection) {
      validateCollection((Collection<?>) fieldObj);
    }

    if (fieldObj instanceof Map) {
      validateCollection(((Map<?, ?>) fieldObj).values());
    }
  }

  private boolean isNeedToValidateFields(Object fieldObj) {
    return Objects.nonNull(fieldObj) &&
        EXCLUDE_PACKAGE_PREFIXES.stream()
            .noneMatch(prefix -> fieldObj.getClass().getName().startsWith(prefix));
  }

  private void validateCollection(Collection<?> listObj) throws Exception {
    for (Object obj : listObj) {
      if (Objects.nonNull(obj)) {
        validateFields(obj.getClass().getDeclaredFields(), obj);
      }
    }
  }

  private void callValidator(Object needToValidate, Annotation annotation) {
    Class<?> serviceClass = getValidatorServiceClass(annotation);

    if (Objects.nonNull(serviceClass)) {
      runValidation(needToValidate, annotation, serviceClass);
    }
  }

  private Class<?> getValidatorServiceClass(Annotation annotation) {
    Class<?> serviceClass = null;

    try {
      Method serviceClassMethod = annotation.getClass()
          .getDeclaredMethod(SERVICE_CLASS_METHOD_NAME);
      serviceClass = (Class<?>) serviceClassMethod.invoke(annotation);
    } catch (Exception e) {
      LOGGER.debug("failed to get validator service class on annotation={}",
          annotation.annotationType());
    }

    return serviceClass;
  }

  private void runValidation(Object objToValidate, Annotation annotation, Class<?> serviceClass) {
    Validator validator = (Validator) beanFactory.getBean(serviceClass);
    validator.validate(objToValidate, annotation);
  }
}
