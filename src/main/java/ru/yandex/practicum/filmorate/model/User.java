package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class User {
    private Long id;
    private String email;
    private String login;
    private String name;
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate birthday;
}
