package com.warp.warp_backend.util;

import com.warp.warp_backend.model.common.ErrorCode;
import com.warp.warp_backend.model.constant.FieldNames;
import com.warp.warp_backend.model.exception.ValidationException;
import io.micrometer.common.util.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class RequestContextHelper {

  public void setRequestId(String requestId) {
    if (StringUtils.isBlank(requestId)) {
      throw new ValidationException(ErrorCode.REQUEST_ID_IS_BLANK);
    }
    MDC.put(FieldNames.REQUEST_ID, requestId);
  }

  public String getRequestId() {
    return MDC.get(FieldNames.REQUEST_ID);
  }
}
