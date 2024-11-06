package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.practicum.filmorate.dao.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Rating;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.like.LikeStorage;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class FilmControllerTest {
    private final FilmDbStorage filmDbStorage;
    private final UserDbStorage userDbStorage;
    private final LikeStorage likesStorage;

    @Test
    public void testFindFilmById() {
        Film validFilm1 = getValidFilm1();
        filmDbStorage.create(validFilm1);

        Optional<Film> filmOptional = Optional.ofNullable(filmDbStorage.getFilmById(1L));
        assertThat(filmOptional)
                .isPresent()
                .hasValueSatisfying(film ->
                        assertThat(film).hasFieldOrPropertyWithValue("id", 1L)
                );
    }

    @Test
    public void testFindUnknownIdFilm() {
        Film validFilm = getValidFilm1();
        filmDbStorage.create(validFilm);
        Film invalidFilm = Film.builder()
                .id(7L)
                .duration(100)
                .build();
        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> filmDbStorage.getFilmById(invalidFilm.getId())
        );
        assertEquals("Film not found", thrown.getMessage());
    }

    @Test
    public void testFindAllFilms() {
        Film validFilm1 = getValidFilm1();
        Film validFilm2 = getValidFilm2();
        filmDbStorage.create(validFilm1);
        filmDbStorage.create(validFilm2);

        Collection<Film> films = filmDbStorage.findAll();
        for (Film film : films) {
            Long id = film.getId();
            AssertionsForInterfaceTypes
                    .assertThat(id)
                    .isEqualTo(film.getId());
        }
    }

    @Test
    public void testUpdateFilm() {
        Film validFilm1 = getValidFilm1();
        Film validFilm2 = getValidFilm2();
        validFilm2.setId(1L);
        filmDbStorage.create(validFilm1);
        filmDbStorage.update(validFilm2);

        Film film = filmDbStorage.getFilmById(1L);
        assertThat(film)
                .hasFieldOrPropertyWithValue("id", 1L)
                .hasFieldOrPropertyWithValue("name", "Film 2");
    }

    @Test
    public void testAddFilm() {
        Film validFilm1 = getValidFilm1();
        filmDbStorage.create(validFilm1);

        Film film = filmDbStorage.getFilmById(1L);
        assertThat(film)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", 1L)
                .hasFieldOrPropertyWithValue("name", "Film 1");
    }

    @Test
    public void testAddLike() {
        Film validFilm1 = filmDbStorage.create(getValidFilm1());
        User validUser1 = userDbStorage.create(getValidUser1());

        likesStorage.addLike(validFilm1.getId(), validUser1.getId());
        Set<Long> filmLikes = likesStorage.getLikes(validFilm1.getId());

        AssertionsForInterfaceTypes
                .assertThat(filmLikes)
                .isNotEmpty()
                .contains(validUser1.getId());
    }

    @Test
    public void testDeleteLike() {
        Film validFilm1 = filmDbStorage.create(getValidFilm1());
        User validUser1 = userDbStorage.create(getValidUser1());

        likesStorage.addLike(validFilm1.getId(), validUser1.getId());
        likesStorage.removeLike(validFilm1.getId(), validUser1.getId());
        Set<Long> filmLikes = likesStorage.getLikes(validFilm1.getId());
        AssertionsForInterfaceTypes
                .assertThat(filmLikes)
                .isEmpty();
    }

    private Film getValidFilm1() {
        Film film = Film.builder()
                .name("Film 1")
                .description("Film 1 description")
                .releaseDate(LocalDate.parse("2000-01-01"))
                .duration(100)
                .build();
        Rating mpa = new Rating();
        mpa.setId(1);
        film.setMpa(mpa);
        return film;
    }

    private Film getValidFilm2() {
        Film film = Film.builder()
                .name("Film 2")
                .description("Film 2 description")
                .releaseDate(LocalDate.parse("2000-01-01"))
                .duration(100)
                .build();
        Rating mpa = new Rating();
        mpa.setId(2);
        film.setMpa(mpa);
        return film;
    }

    private User getValidUser1() {
        return User.builder()
                .login("user1Login")
                .name("user1Name")
                .email("user1@mail")
                .birthday(LocalDate.parse("1997-07-08"))
                .build();
    }
}