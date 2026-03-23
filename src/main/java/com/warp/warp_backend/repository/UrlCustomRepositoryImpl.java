package com.warp.warp_backend.repository;

import com.warp.warp_backend.model.constant.FieldNames;
import com.warp.warp_backend.model.entity.Url;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class UrlCustomRepositoryImpl implements UrlCustomRepository {

  private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
      FieldNames.CREATED_DATE, FieldNames.EXPIRY_DATE, FieldNames.SHORT_URL
  );
  private static final String DEFAULT_SORT_FIELD = FieldNames.CREATED_DATE;

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public List<Url> findUserUrls(Long userId, Boolean active, Boolean isProtected,
      int page, int size, String sortBy, String sortDir) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Url> query = cb.createQuery(Url.class);
    Root<Url> root = query.from(Url.class);

    query.where(buildPredicates(cb, root, userId, active, isProtected).toArray(new Predicate[0]));

    String resolvedSortField = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : DEFAULT_SORT_FIELD;
    Order order = "asc".equalsIgnoreCase(sortDir)
        ? cb.asc(root.get(resolvedSortField))
        : cb.desc(root.get(resolvedSortField));
    query.orderBy(order);

    TypedQuery<Url> typedQuery = entityManager.createQuery(query);
    typedQuery.setFirstResult(page * size);
    typedQuery.setMaxResults(size);
    return typedQuery.getResultList();
  }

  @Override
  public long countUserUrls(Long userId, Boolean active, Boolean isProtected) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Long> query = cb.createQuery(Long.class);
    Root<Url> root = query.from(Url.class);

    query.select(cb.count(root));
    query.where(buildPredicates(cb, root, userId, active, isProtected).toArray(new Predicate[0]));

    return entityManager.createQuery(query).getSingleResult();
  }

  private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Url> root,
      Long userId, Boolean active, Boolean isProtected) {
    List<Predicate> predicates = new ArrayList<>();

    predicates.add(cb.isNull(root.get(FieldNames.DELETED_DATE)));
    predicates.add(cb.equal(root.get(FieldNames.USER_ID), userId));

    if (Objects.nonNull(active)) {
      Instant now = Instant.now();
      if (active) {
        predicates.add(cb.or(
            cb.isNull(root.get(FieldNames.EXPIRY_DATE)),
            cb.greaterThan(root.get(FieldNames.EXPIRY_DATE), now)
        ));
      } else {
        predicates.add(cb.and(
            cb.isNotNull(root.get(FieldNames.EXPIRY_DATE)),
            cb.lessThanOrEqualTo(root.get(FieldNames.EXPIRY_DATE), now)
        ));
      }
    }

    if (Objects.nonNull(isProtected)) {
      predicates.add(cb.equal(root.get(FieldNames.IS_PROTECTED), isProtected));
    }

    return predicates;
  }
}
