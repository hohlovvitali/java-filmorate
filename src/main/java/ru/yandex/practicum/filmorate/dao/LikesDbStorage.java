package ru.yandex.practicum.filmorate.dao;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.storage.like.LikeStorage;

import java.util.HashSet;
import java.util.Set;

@Component
@AllArgsConstructor
public class LikesDbStorage implements LikeStorage {
    private final JdbcTemplate jdbcTemplate;
    private static final Logger log = LoggerFactory.getLogger(FilmController.class);

    @Override
    public void addLike(Long id, Long userId) {
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
}
