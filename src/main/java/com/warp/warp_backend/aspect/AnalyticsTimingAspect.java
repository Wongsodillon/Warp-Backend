package com.warp.warp_backend.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AnalyticsTimingAspect {

  private static final Logger log = LoggerFactory.getLogger(AnalyticsTimingAspect.class);

  @Around("execution(public * com.warp.warp_backend.service.AnalyticsService.*(..))")
  public Object time(ProceedingJoinPoint joinPoint) throws Throwable {
    long start = System.currentTimeMillis();
    try {
      return joinPoint.proceed();
    } finally {
      long elapsed = System.currentTimeMillis() - start;
      log.debug("[analytics] {} completed in {}ms", joinPoint.getSignature().getName(), elapsed);
    }
  }
}