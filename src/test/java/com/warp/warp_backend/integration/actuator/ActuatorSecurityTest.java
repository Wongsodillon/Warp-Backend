package com.warp.warp_backend.integration.actuator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.warp.warp_backend.integration.BaseIntegrationContextTest;
import com.warp.warp_backend.model.TestConstant;
import com.warp.warp_backend.model.constant.ApiPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

public class ActuatorSecurityTest extends BaseIntegrationContextTest {

  @Autowired
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockAuthForUser(TestConstant.TEST_CLERK_USER_ID);
  }

  @Test
  void getHealth_unauthenticated_returns200() throws Exception {
    mockMvc.perform(get(ApiPath.ACTUATOR_HEALTH))
        .andExpect(status().isOk());
  }

  @Test
  void getHealth_authenticated_returns200() throws Exception {
    mockMvc.perform(withAuth(get(ApiPath.ACTUATOR_HEALTH)))
        .andExpect(status().isOk());
  }

  @Test
  void getMetrics_unauthenticated_returns403() throws Exception {
    mockMvc.perform(get(ApiPath.ACTUATOR_METRICS))
        .andExpect(status().isForbidden());
  }

  @Test
  void getMetrics_authenticated_returns403() throws Exception {
    mockMvc.perform(withAuth(get(ApiPath.ACTUATOR_METRICS)))
        .andExpect(status().isForbidden());
  }
}
