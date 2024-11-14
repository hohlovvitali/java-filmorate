package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
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
    private static final Logger log = LoggerFactory.getLogger(FilmController.class);

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

    public Collection<Film> getCommonFilms(Long userId, Long friendId) {
        log.info("Вывод общих фильмов пользователя {} и пользователя {}", userId, friendId);
        return filmStorage.getCommonFilms(userId, friendId);
    }


    public List<Film> search(String query, List<String> by) {
        log.info("Поиск фильмов c подстрокой " + query);
        if (query == null || query.isEmpty()) {
            log.warn("Текст для поиска не может быть null или пустым");
            throw new ValidationException("Не указан текст для посика");
        }
        if (by.size() == 1) {
            if (by.getFirst().equals("director")) {
                log.trace("Осуществляем поиск по режиссеру");
                return filmStorage.searchFilms(query, true, false);
            }
            if (by.getFirst().equals("title")) {
                log.trace("Осуществляем поиск по названию");
                return filmStorage.searchFilms(query, false, true);
            }
            log.warn("Поиск может осуществляться только по названию или по режиссеру");
            throw new NotFoundException("Неизвестное значение параметра поиска. Ввидите director для поиска по" +
                    " режиссеру, title для поиска по названию или оба значения через запятую для посика одновременно " +
                    "и по режиссеру и по названию");
        }
        if (by.size() == 2 && by.contains("director") && by.contains("title")) {
            log.trace("Осуществляем поиск по названию и по режиссеру одновременно");
            return filmStorage.searchFilms(query, true, true);
        }
        throw new ValidationException("Неверный формат параметров поска");
    }
}