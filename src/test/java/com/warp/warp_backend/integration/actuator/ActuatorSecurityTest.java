package com.warp.warp_backend.integration.actuator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.warp.warp_backend.integration.BaseIntegrationContextTest;
import com.warp.warp_backend.model.TestConstant;
import com.warp.warp_backend.model.constant.ApiPath;
import com.warp.warp_backend.model.constant.ConstantValue;
import com.warp.warp_backend.model.entity.User;
import com.warp.warp_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

public class ActuatorSecurityTest extends BaseIntegrationContextTest {

  private static final String ADMIN_CLERK_USER_ID = "test_admin_clerk_user_id";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    mockAuthForUser(TestConstant.TEST_CLERK_USER_ID);
  }

  @Test
  void getHealth_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get(ApiPath.ACTUATOR_HEALTH))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getHealth_authenticatedNonAdminUser_returns403() throws Exception {
    mockMvc.perform(withAuth(get(ApiPath.ACTUATOR_HEALTH)))
        .andExpect(status().isForbidden());
  }

  @Test
  @Transactional
  void getHealth_adminUser_returns200() throws Exception {
    userRepository.save(User.builder()
        .clerkUserId(ADMIN_CLERK_USER_ID)
        .role(ConstantValue.ADMIN)
        .build());

    mockAuthForUser(ADMIN_CLERK_USER_ID);

    mockMvc.perform(withAuth(get(ApiPath.ACTUATOR_HEALTH)))
        .andExpect(status().isOk());
  }
}
