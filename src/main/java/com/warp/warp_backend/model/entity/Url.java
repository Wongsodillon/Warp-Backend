package com.warp.warp_backend.model.entity;

import com.warp.warp_backend.model.constant.EntityConstant;
import com.warp.warp_backend.model.constant.TableNames;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

import java.time.Instant;

@Entity
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = TableNames.URLS)
public class Url extends BaseEntity {

  @Id
  @Column(name = EntityConstant.ID)
  private Long id;

  @Column(name = EntityConstant.SHORT_URL, nullable = false, unique = true, length = 10)
  private String shortUrl;

  @Column(name = EntityConstant.DESTINATION_URL, nullable = false)
  private String destinationUrl;

  @Column(name = EntityConstant.PASSWORD)
  private String password;

  @Column(name = EntityConstant.EXPIRY_DATE)
  private Instant expiryDate;

  @Column(name = EntityConstant.DISABLED, nullable = false)
  private boolean disabled;

  @Column(name = EntityConstant.IS_PROTECTED, nullable = false)
  private boolean isProtected;

  @CreatedBy
  @Column(name = EntityConstant.CREATED_BY)
  private String createdBy;

  @LastModifiedBy
  @Column(name = EntityConstant.UPDATED_BY)
  private String updatedBy;

  @Column(name = EntityConstant.USER_ID)
  private Long userId;
}
