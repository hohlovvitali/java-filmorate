package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import ru.yandex.practicum.filmorate.validation.annotations.MinimumReleaseDate;

import java.time.LocalDate;
import java.util.*;

@Data
@Builder
public class Film {
    private Long id;
    @NotBlank(message = "Incorrect film's name")
    private String name;
    @NotNull
    @Size(max = 200, message = "Too long description")
    private String description;
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    @MinimumReleaseDate
    private LocalDate releaseDate;
    @Positive(message = "Duration must be positive value")
    private Integer duration;
    @Builder.Default
    private Set<Long> userLikesIdSet = new TreeSet<>();
    @Builder.Default
    private List<Genre> genres = new ArrayList<>();
    @NotNull
    private Rating mpa;
    private Set<Director> directors = new HashSet<>();

    public void addLike(Long id) {
        this.userLikesIdSet.add(id);
    }
}
