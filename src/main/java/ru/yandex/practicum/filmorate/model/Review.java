package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import ru.yandex.practicum.filmorate.exception.NotFoundException;


@Data
@Builder
public class Review {
    private Long reviewId;
    @NotBlank
    private String content;
    @JsonProperty("isPositive")
    @NotNull
    private Boolean isPositive;
    @NotNull
    private Long userId;
    @NotNull
    private Long filmId;
    private Long useful;

    public static boolean getBooleanType(String str) {
        boolean isPositive;
        isPositive = switch (str) {
            case "Positive" -> true;
            case "Negative" -> false;
            default -> throw new NotFoundException("Неизвестный тип отзыва");
        };
        return isPositive;
    }

    public static String getStringType(boolean isPositive) {
        if (isPositive) {
            return "Positive";
        }
        return "Negative";
    }
}
