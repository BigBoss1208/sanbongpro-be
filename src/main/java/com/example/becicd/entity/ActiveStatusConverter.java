package com.example.becicd.entity;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class ActiveStatusConverter implements AttributeConverter<Boolean, Boolean> {

    @Override
    public Boolean convertToDatabaseColumn(Boolean active) {
        return Boolean.FALSE.equals(active);
    }

    @Override
    public Boolean convertToEntityAttribute(Boolean locked) {
        return !Boolean.TRUE.equals(locked);
    }
}