package ru.yandex.practicum.filmorate.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.like.LikeStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.*;

@Component
public class LikesDbStorage implements LikeStorage {
    private final JdbcTemplate jdbcTemplate;
    @Qualifier("userDbStorage")
    private final UserStorage userStorage;
    @Qualifier("filmDbStorage")
    private final FilmStorage filmStorage;
    private static final Logger log = LoggerFactory.getLogger(FilmController.class);

    public LikesDbStorage(JdbcTemplate jdbcTemplate, @Qualifier("userDbStorage") UserStorage userStorage,
                          @Qualifier("filmDbStorage") FilmStorage filmStorage) {
        this.jdbcTemplate = jdbcTemplate;
        this.userStorage = userStorage;
        this.filmStorage = filmStorage;
    }

    @Override
    public void addLike(Long id, Long userId) {
        try {
            userStorage.getUserById(userId);
            filmStorage.getFilmById(id);
        } catch (NotFoundException e) {
            throw new NotFoundException("Объект не найден");
        }

        String sql = "INSERT INTO films_Likes (film_id, user_id) VALUES (?,?)";
        jdbcTemplate.update(sql, id, userId);
    }

    @Override
    public void removeLike(Long id, Long userId) {
        String sql = "DELETE FROM films_Likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, id, userId);
    }

    @Override
    public Set<Long> getLikes(Long filmId) {
        String sql = "SELECT user_id FROM films_Likes WHERE film_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, Long.class, filmId));
    }

    public Set<Long> getUsersFilmLikes(Long userId) {
        String sql = "SELECT film_id FROM films_Likes WHERE user_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, Long.class, userId));
    }
}
