package com.warp.warp_backend.model.entity;

import com.warp.warp_backend.model.constant.EntityConstant;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@MappedSuperclass
@Data
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public abstract class BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = EntityConstant.ID)
  private Long id;

  @CreatedDate
  @Column(name = EntityConstant.CREATED_DATE, nullable = false, updatable = false)
  private Instant createdDate;

  @LastModifiedDate
  @Column(name = EntityConstant.UPDATED_DATE)
  private Instant updatedDate;

  @Column(name = EntityConstant.DELETED_DATE)
  private Instant deletedDate;
}