package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/films")
public class FilmController {
    private final Map<Long, Film> films = new HashMap<>();
    @JsonFormat(pattern="yyyy-MM-dd")
    private static LocalDate RELEASE_DATE_MIN = LocalDate.parse("1895-12-28");
    private static final Logger log = LoggerFactory.getLogger(FilmController.class);

    @GetMapping
    public Collection<Film> findAll() {
        log.info("Вывод списка всех фильмов");
        return films.values();
    }

    @PostMapping
    public Film create(@RequestBody Film film) {
        log.trace("Добавление нового фильма");
        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Пустое название фильма");
            throw new ValidationException("Имя должно быть указано");
        }

        if (checkDuplicateFilmName(film.getName())) {
            log.warn("Имя фильма {} уже занято", film.getName());
            throw new DuplicatedDataException("Это имя уже используется");
        }

        if (film.getDescription().length() > 200) {
            log.warn("Описание более 200 символов: {}", film.getDescription().length());
            throw new ValidationException("Описание больше 200 символов");
        }

        if (film.getReleaseDate() != null) {
            if (film.getReleaseDate().isBefore(RELEASE_DATE_MIN)) {
                log.warn("Дата релиза раньше {}", RELEASE_DATE_MIN);
                throw new ValidationException("Дата релиза не может быть до 1895.12.28");
            }
        }

        if (film.getDuration() != null) {
            if (film.getDuration() < 1) {
                log.warn("Неположительная продолжительность фильма: {}", film.getDuration());
                throw new ValidationException("Продолжительность фильма должна быть больше 0");
            }
        }

        film.setId(getNextId());
        log.trace("Установление id фильма: {}", film.getId());

        films.put(film.getId(), film);
        log.info("Добавление нового фильма {} c id: {}", film.getName(), film.getId());

        return film;
    }

    @PutMapping
    public Film update(@RequestBody Film newFilm) {
        log.trace("Обновление данных фильма");
        if (newFilm.getId() == null) {
            log.warn("Пустной id фильма");
            throw new ValidationException("Id должен быть указан");
        }

        if (films.containsKey(newFilm.getId())) {
            Film oldFilm = films.get(newFilm.getId());

            if (newFilm.getName() != null && !newFilm.getName().isBlank()){
                if (checkDuplicateFilmName(newFilm.getName()) && !oldFilm.getName().equals(newFilm.getName())) {
                    log.warn("Имя для обновленного фильма {} уже занято", newFilm.getName());
                    throw new DuplicatedDataException("Это имя уже используется");
                }

                log.info("Обновление названия фильма с id = {}", oldFilm.getId());
                oldFilm.setName(newFilm.getName());
            }

            if (newFilm.getDescription() != null && !newFilm.getDescription().isBlank()) {
                if (newFilm.getDescription().length() > 200) {
                    log.warn("Описание обновленного фильма более 200 символов: {}", newFilm.getDescription().length());
                    throw new ValidationException("Описание больше 200 символов");
                }

                log.info("Обновление описания для фильма с id: {}", oldFilm.getId());
                oldFilm.setDescription(newFilm.getDescription());
            }

            if (newFilm.getReleaseDate() != null) {
                if (newFilm.getReleaseDate().isBefore(RELEASE_DATE_MIN)) {
                    log.warn("Дата релиза нового фильма раньше {}", RELEASE_DATE_MIN);
                    throw new ValidationException("Дата релиза не может быть до 1895-12-28");
                }

                log.info("Обновление даты релиза для фильма с id: {}", oldFilm.getId());
                oldFilm.setReleaseDate(newFilm.getReleaseDate());
            }

            if (newFilm.getDuration() != null) {
                if (newFilm.getDuration() < 0) {
                    log.warn("Отрицательная продолжительность обновленного фильма: {}", oldFilm.getDuration());
                    throw new ValidationException("Продолжительность фильма должна быть больше 0");
                }

                log.info("Обновление продолжительности фильма с id: {}", oldFilm.getId());
                oldFilm.setDuration(newFilm.getDuration());
            }

            return newFilm;
        }

        log.warn("Несуществующий id фильма: {}", newFilm.getId());
        throw new NotFoundException("Фильм с id = " + newFilm.getId() + " не найден");
    }

    private long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    private boolean checkDuplicateFilmName(String filmNameForCheck) {
        return films.values().stream()
                .anyMatch(film -> film.getName().equals(filmNameForCheck));
    }
}
