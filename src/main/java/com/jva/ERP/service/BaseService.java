package com.jva.ERP.service;

import java.util.List;
import java.util.Optional;

/**
 * Base service interface for common CRUD operations
 */
public interface BaseService<T, ID> {
    T save(T entity);
    T update(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void delete(ID id);
    void deleteAll();
    long count();
}

