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

  @Column(name = FieldNames.SHORT_URL, nullable = false, unique = true, length = 10)
  private String shortUrl;

  @Column(name = FieldNames.DESTINATION_URL, nullable = false)
  private String destinationUrl;

  @Column(name = FieldNames.PASSWORD)
  private String password;

  @Column(name = FieldNames.EXPIRY_DATE)
  private Instant expiryDate;

  @Column(name = FieldNames.DISABLED, nullable = false)
  private boolean disabled;

  @Column(name = FieldNames.IS_PROTECTED, nullable = false)
  private boolean isProtected;

  @CreatedBy
  @Column(name = FieldNames.CREATED_BY)
  private String createdBy;

  @LastModifiedBy
  @Column(name = FieldNames.UPDATED_BY)
  private String updatedBy;

  @Column(name = FieldNames.USER_ID)
  private Long userId;
}
