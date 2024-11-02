package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

@Data
@Builder
public class User {
    private Long id;
    @NotBlank(message = "Email must be not empty")
    @Email(message = "Email must be with @")
    private String email;
    @NotBlank(message = "Login must be not empty")
    @Pattern(regexp = "\\S*$", message = "login must be without space")
    private String login;
    private String name;
    @JsonFormat(pattern = "yyyy-MM-dd")
    @PastOrPresent(message = "Birthday couldn't be in future")
    private LocalDate birthday;
    @Builder.Default
    private Set<Long> friendsIdSet = new TreeSet<>();

    public void addFriend(Long id) {
        this.friendsIdSet.add(id);
    }
}
