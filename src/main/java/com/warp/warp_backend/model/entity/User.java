package com.warp.warp_backend.model.entity;

import com.warp.warp_backend.model.constant.EntityConstant;
import com.warp.warp_backend.model.constant.TableNames;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = EntityConstant.ID)
  private Long id;

  @Column(name = EntityConstant.CLERK_USER_ID, nullable = false, unique = true, length = 255)
  private String clerkUserId;

  @Column(name = EntityConstant.ROLE, nullable = false, length = 50)
  private String role;
}
