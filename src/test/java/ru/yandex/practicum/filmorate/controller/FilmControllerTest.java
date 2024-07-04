package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {
    FilmController filmController;

    @BeforeEach
    public void beforeEach() {
        filmController = new FilmController();
    }

    @Test
    void findAllTest() {
        Film film1 = Film.builder()
                .name("Film 1")
                .description("Film 1 description")
                .releaseDate(LocalDate.parse("2000-01-01"))
                .duration(100)
                .build();

        Film film2 = Film.builder()
                .name("Film 2")
                .description("Film 2 description")
                .releaseDate(LocalDate.parse("2000-01-01"))
                .duration(100)
                .build();

        filmController.create(film1);
        filmController.create(film2);

        Collection<Film> filmCollectionFromMethod = filmController.findAll();
        Collection<Film> filmCollection = new ArrayList<>();
        filmCollection.add(film1);
        filmCollection.add(film2);

        assertNotNull(filmCollectionFromMethod, "Список фильмов не возращается");
        assertEquals(2, filmCollectionFromMethod.size(), "Неверный размер возращаемого списка");
        assertEquals(filmCollection, filmCollectionFromMethod.stream().toList(), "Возращается не тот список");
    }

    @Test
    void createTest() {
        Film film1 = Film.builder()
                .name("Film 1")
                .description("Film 1 description")
                .releaseDate(LocalDate.parse("2000-01-01"))
                .duration(100)
                .build();

        filmController.create(film1);

        Collection<Film> filmCollectionFromMethod = filmController.findAll();

        assertNotNull(filmCollectionFromMethod, "Список фильмов не возращается");
        assertEquals(1, filmCollectionFromMethod.size(), "Неверный размер возращаемого списка");
        assertEquals(film1, filmCollectionFromMethod.stream().toList().getFirst(), "Возращается не тот фильм");
    }

    @Test
    void createWithEmptyName() {
        Film film1 = Film.builder()
                .name("")
                .description("Film 1 description")
                .releaseDate(LocalDate.parse("2000-01-01"))
                .duration(100)
                .build();

        ValidationException ex = Assertions.assertThrows(
                ValidationException.class,
                generateCreateException(film1)
        );

        Assertions.assertEquals("Имя должно быть указано", ex.getMessage());
    }

    @Test
    void createWithDescriptionLength200() {
        Film film1 = Film.builder()
                .name("Film 1")
                .description("Film 1 description")
                .releaseDate(LocalDate.parse("2000-01-01"))
                .duration(100)
                .build();

        String newDescription = String.format("%200s", film1.getDescription()).replace(" ", "0");
        film1.setDescription(newDescription);

        filmController.create(film1);

        Collection<Film> filmCollectionFromMethod = filmController.findAll();

        assertNotNull(filmCollectionFromMethod, "Список фильмов не возращается");
        assertEquals(1, filmCollectionFromMethod.size(), "Неверный размер возращаемого списка");
        assertEquals(film1.getDescription(), filmCollectionFromMethod.stream().toList().getFirst().getDescription(), "Возращается не то описание фильма");
    }

    @Test
    void createWithDescriptionLength300() {
        Film film1 = Film.builder()
                .name("Film 1")
                .description("Film 1 description")
                .releaseDate(LocalDate.parse("2000-01-01"))
                .duration(100)
                .build();

        String newDescription = String.format("%300s", film1.getDescription()).replace(" ", "0");
        film1.setDescription(newDescription);

        ValidationException ex = Assertions.assertThrows(
                ValidationException.class,
                generateCreateException(film1)
        );

        Assertions.assertEquals("Описание больше 200 символов", ex.getMessage());
    }

    @Test
    void createWithDateRelease18951228() {
        Film film1 = Film.builder()
                .name("Film 1")
                .description("Film 1 description")
                .releaseDate(LocalDate.parse("1895-12-28"))
                .duration(100)
                .build();

        filmController.create(film1);

        Collection<Film> filmCollectionFromMethod = filmController.findAll();

        assertNotNull(filmCollectionFromMethod, "Список фильмов не возращается");
        assertEquals(1, filmCollectionFromMethod.size(), "Неверный размер возращаемого списка");
        assertEquals(film1.getReleaseDate(), filmCollectionFromMethod.stream().toList().getFirst().getReleaseDate(), "Возращается не та дата релиза");
    }

    @Test
    void createWithDateRelease18900101() {
        Film film1 = Film.builder()
                .name("Film 1")
                .description("Film 1 description")
                .releaseDate(LocalDate.parse("1890-01-01"))
                .duration(100)
                .build();

        ValidationException ex = Assertions.assertThrows(
                ValidationException.class,
                generateCreateException(film1)
        );

        Assertions.assertEquals("Дата релиза не может быть до 1895.12.28", ex.getMessage());
    }

    @Test
    void createWithZeroDuration() {
        Film film1 = Film.builder()
                .name("Film 1")
                .description("Film 1 description")
                .releaseDate(LocalDate.parse("2000-01-01"))
                .duration(0)
                .build();

        ValidationException ex = Assertions.assertThrows(
                ValidationException.class,
                generateCreateException(film1)
        );

        Assertions.assertEquals("Продолжительность фильма должна быть больше 0", ex.getMessage());
    }

    @Test
    void createWithNegativeDuration() {
        Film film1 = Film.builder()
                .name("Film 1")
                .description("Film 1 description")
                .releaseDate(LocalDate.parse("2000-01-01"))
                .duration(-5)
                .build();

        ValidationException ex = Assertions.assertThrows(
                ValidationException.class,
                generateCreateException(film1)
        );

        Assertions.assertEquals("Продолжительность фильма должна быть больше 0", ex.getMessage());
    }

    @Test
    void createDuplicateFilm() {
        Film film1 = Film.builder()
                .name("Film 1")
                .description("Film 1 description")
                .releaseDate(LocalDate.parse("2000-01-01"))
                .duration(100)
                .build();

        filmController.create(film1);

        DuplicatedDataException ex = Assertions.assertThrows(
                DuplicatedDataException.class,
                generateCreateException(film1)
        );

        Assertions.assertEquals("Это имя уже используется", ex.getMessage());
    }

    @Test
    void updateTest() {
        Film film1 = Film.builder()
                .name("Film 1")
                .description("Film 1 description")
                .releaseDate(LocalDate.parse("2000-01-01"))
                .duration(100)
                .build();

        filmController.create(film1);

        Film film1new = Film.builder()
                .id(1L)
                .name("Film 1 new")
                .description("Film 1 new description")
                .releaseDate(LocalDate.parse("2000-01-12"))
                .duration(110)
                .build();

        filmController.update(film1new);

        Collection<Film> filmCollectionFromMethod = filmController.findAll();

        assertNotNull(filmCollectionFromMethod, "Список фильмов не возращается");
        assertEquals(1, filmCollectionFromMethod.size(), "Неверный размер возращаемого списка");
        assertEquals(film1new, filmCollectionFromMethod.stream().toList().getFirst(), "Возращается неправильно обновленный фильм");
    }

    @Test
    void updateWithEmptyId() {
        Film film1 = Film.builder()
                .name("Film 1")
                .description("Film 1 description")
                .releaseDate(LocalDate.parse("2000-01-01"))
                .duration(100)
                .build();

        ValidationException ex = Assertions.assertThrows(
                ValidationException.class,
                generateUpdateException(film1)
        );

        Assertions.assertEquals("Id должен быть указан", ex.getMessage());
    }

    @Test
    void updateWithNonExistentId() {
        Film film1 = Film.builder()
                .name("Film 1")
                .description("Film 1 description")
                .releaseDate(LocalDate.parse("2000-01-01"))
                .duration(100)
                .build();

        filmController.create(film1);
        film1.setId(5L);

        NotFoundException ex = Assertions.assertThrows(
                NotFoundException.class,
                generateUpdateException(film1)
        );

        Assertions.assertEquals("Фильм с id = 5 не найден", ex.getMessage());
    }

    @Test
    void updateWithDuplicateFilmName() {
        Film film1 = Film.builder()
                .name("Film 1")
                .description("Film 1 description")
                .releaseDate(LocalDate.parse("2000-01-01"))
                .duration(100)
                .build();

        Film film2 = Film.builder()
                .name("Film 2")
                .description("Film 2 description")
                .releaseDate(LocalDate.parse("2000-01-01"))
                .duration(100)
                .build();

        filmController.create(film1);
        filmController.create(film2);

        Film filmDuplicate = Film.builder()
                .id(1L)
                .name("Film 2")
                .description("Film 1 description")
                .releaseDate(LocalDate.parse("2000-01-01"))
                .duration(100)
                .build();

        DuplicatedDataException ex = Assertions.assertThrows(
                DuplicatedDataException.class,
                generateUpdateException(filmDuplicate)
        );

        Assertions.assertEquals("Это имя уже используется", ex.getMessage());
    }

    private Executable generateCreateException(Film film) {
        return () -> filmController.create(film);
    }

    private Executable generateUpdateException(Film film) {
        return () -> filmController.update(film);
    }
}