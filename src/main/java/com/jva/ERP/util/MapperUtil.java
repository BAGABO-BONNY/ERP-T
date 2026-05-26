package com.jva.ERP.util;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Mapper Utility
 * Provides mapping utilities for entity-DTO conversions
 */
public class MapperUtil {

    private MapperUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Map single entity to DTO
     */
    public static <E, D> D mapToDTO(E entity, Function<E, D> mapper) {
        return mapper.apply(entity);
    }

    /**
     * Map list of entities to DTOs
     */
    public static <E, D> List<D> mapToDTOList(List<E> entities, Function<E, D> mapper) {
        return entities.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }

    /**
     * Map single DTO to entity
     */
    public static <D, E> E mapToEntity(D dto, Function<D, E> mapper) {
        return mapper.apply(dto);
    }

    /**
     * Map list of DTOs to entities
     */
    public static <D, E> List<E> mapToEntityList(List<D> dtos, Function<D, E> mapper) {
        return dtos.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }
}

