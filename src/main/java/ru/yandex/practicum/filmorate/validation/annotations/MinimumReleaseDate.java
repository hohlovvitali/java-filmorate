package ru.yandex.practicum.filmorate.validation.annotations;

import jakarta.validation.Constraint;
import ru.yandex.practicum.filmorate.validation.MinimumReleaseDateValidator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MinimumReleaseDateValidator.class)
public @interface MinimumReleaseDate {
    String message() default "Date before {value}";

    Class<?>[] groups() default {};

    Class<?>[] payload() default {};

    String value() default "1895-12-28";
}
