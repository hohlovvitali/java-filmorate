package ru.yandex.practicum.filmorate.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ru.yandex.practicum.filmorate.validation.annotations.MinimumReleaseDate;

import java.time.LocalDate;

public class MinimumReleaseDateValidator implements ConstraintValidator<MinimumReleaseDate, LocalDate> {
    private final LocalDate minimumDate = LocalDate.parse("1895-12-28");

    @Override
    public void initialize(MinimumReleaseDate constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(LocalDate localDate, ConstraintValidatorContext constraintValidatorContext) {
        return localDate == null || !localDate.isBefore(minimumDate);
    }
}
