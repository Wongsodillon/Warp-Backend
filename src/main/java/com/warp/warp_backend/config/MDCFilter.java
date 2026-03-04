package com.warp.warp_backend.config;

import com.warp.warp_backend.model.constant.FieldNames;
import com.warp.warp_backend.util.RequestContextHelper;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class MDCFilter extends OncePerRequestFilter {

  @Autowired
  private RequestContextHelper requestContextHelper;

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

    String requestId = request.getParameter(FieldNames.REQUEST_ID);
    if (StringUtils.isBlank(requestId)) {
      requestContextHelper.setRequestId(UUID.randomUUID().toString());
    } else {
      requestContextHelper.setRequestId(requestId);
    }
    filterChain.doFilter(request, response);
  }
}
