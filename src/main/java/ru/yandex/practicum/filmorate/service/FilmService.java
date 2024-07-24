package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.*;

@Service
@RequiredArgsConstructor
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private static final Logger log = LoggerFactory.getLogger(FilmController.class);
    private final Comparator<Film> filmComparator = (film1, film2) -> {
        if (film1.getUserLikesIdSet().size() != film2.getUserLikesIdSet().size()) {
            return Integer.compare(film2.getUserLikesIdSet().size(), film1.getUserLikesIdSet().size());
        }

        return Long.compare(film1.getId(), film2.getId());
    };

    public Collection<Film> findAll() {
        log.trace("Вывод списка всех фильмов");
        return filmStorage.findAll();
    }

    public Film create(Film film) {
        log.trace("Добавление нового фильма");
        return filmStorage.create(film);
    }

    public Film update(Film newFilm) {
        log.trace("Обновление данных фильма");
        return filmStorage.update(newFilm);
    }

    public void addLike(Long filmId, Long userId) {
        log.trace("Добавление лайка для фильма id={}, пользователем с id={}", filmId, userId);
        if (userId == null) {
            log.warn("Не указан id пользователя. Добавление лайка невозможно");
            throw new ValidationException("Id должен быть указан");
        }

        userStorage.getUserById(userId);

        filmStorage.addLikeToFilm(filmId, userId);
    }

    public void deleteLike(Long filmId, Long userId) {
        log.trace("Удаление лайка для фильма id={}, пользователем с id={}", filmId, userId);
        if (userId == null) {
            log.warn("Не указан id пользователя. Удаление лайка невозможно");
            throw new ValidationException("Id должен быть указан");
        }

        userStorage.getUserById(userId);

        filmStorage.deleteLikeFromFilm(filmId, userId);
    }

    public Collection<Film> getPopularFilms(Integer count) {
        log.info("Вывод первыx {} популярных фильмов ", count);
        if (count <= 0) {
            log.warn("Количество выводимых фильмов должно быть больше 0: {}", count);
            throw new ValidationException("Количество выводимых фильмов должно быть больше 0");
        }

        if (count > filmStorage.findAll().size()) {
            log.debug("Количество выводимых фильмов {} больше общего количества {}. Выводятся все фильмы", count, filmStorage.findAll().size());
            count = filmStorage.findAll().size();
        }

        return filmStorage.findAll().stream().sorted(filmComparator).limit(count).toList();
    }
}
