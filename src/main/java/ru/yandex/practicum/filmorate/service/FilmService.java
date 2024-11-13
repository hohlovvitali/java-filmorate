package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.EventOperation;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.like.LikeStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.List;

@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final LikeStorage likeStorage;
    private final GenreStorage genreStorage;
    private final EventService eventService;
    private static final Logger log = LoggerFactory.getLogger(FilmService.class);

    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       LikeStorage likeStorage, GenreStorage genreStorage, EventService eventService) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.likeStorage = likeStorage;
        this.genreStorage = genreStorage;
        this.eventService = eventService;
    }

    public Collection<Film> findAll() {
        log.trace("Вывод списка всех фильмов");
        return filmStorage.findAll();
    }

    public Film create(Film film) {
        log.trace("Добавление нового фильма");
        return filmStorage.create(film);
    }

    public Film getFilmById(Long id) {
        log.trace("Получение фильма с id = {}", id);
        Film film = filmStorage.getFilmById(id);
        film.setGenres(genreStorage.findAllGenresByFilm(id));
        return film;
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

        likeStorage.addLike(filmId, userId);
        eventService.addEvent(EventType.LIKE, EventOperation.ADD, userId, filmId);
    }

    public void deleteLike(Long filmId, Long userId) {
        log.trace("Удаление лайка для фильма id={}, пользователем с id={}", filmId, userId);
        if (userId == null) {
            log.warn("Не указан id пользователя. Удаление лайка невозможно");
            throw new ValidationException("Id должен быть указан");
        }

        userStorage.getUserById(userId);

        likeStorage.removeLike(filmId, userId);
        eventService.addEvent(EventType.LIKE, EventOperation.REMOVE, userId, filmId);
    }

    public Collection<Film> getPopularFilms(Integer count, Integer genreId, Integer year) {
        log.info("Вывод первыx {} популярных фильмов жанра {}, в году {} ", count, genreId, year);
        return filmStorage.getPopularFilms(count, genreId, year);
    }

    public void deleteFilmById(Long filmId) {
        log.trace("Удаление фильма id={}", filmId);
        filmStorage.deleteFilmById(filmId);
    }

    public List<Film> getFilmsByDirector(Long directorId, String sortBy) {
        log.info("Получение фильмов режиссера с id = {} и сортировкой по {}", directorId, sortBy);
        return filmStorage.getFilmsByDirector(directorId, sortBy);
    }
}
