package com.warp.warp_backend.model.entity;

import com.warp.warp_backend.model.constant.FieldNames;
import com.warp.warp_backend.model.constant.TableNames;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = TableNames.USERS)
public class User extends BaseEntity {

  @Column(name = FieldNames.CLERK_USER_ID, nullable = false, unique = true, length = 255)
  private String clerkUserId;

  @Column(name = FieldNames.ROLE, nullable = false, length = 50)
  private String role;
}
