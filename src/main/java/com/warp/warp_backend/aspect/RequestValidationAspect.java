package com.warp.warp_backend.aspect;

import com.warp.warp_backend.model.annotation.Validate;
import com.warp.warp_backend.util.ValidationUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Aspect
@Component
public class RequestValidationAspect {

  @Autowired
  private ValidationUtil validationUtil;

  @Around("within(com.warp.warp_backend.controller.* && "
      + "@org.springframework.web.bind.annotation.RestController * && "
      + "!@org.springframework.web.bind.annotation.ControllerAdvice * && "
      + "!@org.springframework.web.bind.annotation.ExceptionHandler *)")
  public Object validate(final ProceedingJoinPoint joinPoint)
      throws Throwable {
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    Parameter[] parameters = methodSignature.getMethod().getParameters();
    Object[] arguments = joinPoint.getArgs();
    validateArguments(arguments, parameters);
    return joinPoint.proceed();
  }

  private void validateArguments(Object[] arguments, Parameter[] parameters) throws Exception {
    List<Integer> needValidateParameterIndices = searchNeedValidateParameterIndices(parameters);
    validateArgumentsDirectly(arguments, parameters, needValidateParameterIndices);
    validateArgumentsField(arguments, needValidateParameterIndices);
  }

  private void validateArgumentsDirectly(Object[] arguments, Parameter[] parameters,
      List<Integer> needValidateParameterIndices) throws Exception {
    List<Integer> parameterIndices = IntStream.range(0, parameters.length)
        .filter(index -> !needValidateParameterIndices.contains(index))
        .boxed()
        .collect(Collectors.toList());

    for (Integer index : parameterIndices) {
      validationUtil.validateArgument(arguments[index], parameters[index]);
    }
  }

  private void validateArgumentsField(Object[] arguments, List<Integer> parameterIndices)
      throws Exception {
    List<Object> filteredArguments = filterArguments(arguments, parameterIndices);
    validationUtil.validateArgumentFields(filteredArguments);
  }

  private List<Object> filterArguments(Object[] arguments, List<Integer> parameterIndices) {
    return parameterIndices.stream()
        .map(index -> arguments[index])
        .collect(Collectors.toList());
  }

  private List<Integer> searchNeedValidateParameterIndices(Parameter[] parameters) {
    return IntStream.range(0, parameters.length)
        .filter(index -> isContainsValidateAnnotation(parameters[index]))
        .boxed()
        .collect(Collectors.toList());
  }

  private boolean isContainsValidateAnnotation(Parameter parameter) {
    Validate[] annotationsByType = parameter.getDeclaredAnnotationsByType(Validate.class);
    return annotationsByType.length != 0;
  }
}
