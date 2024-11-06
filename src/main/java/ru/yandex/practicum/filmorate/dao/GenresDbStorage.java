package ru.yandex.practicum.filmorate.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.mappers.GenreMapper;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Comparator;
import java.util.List;

@Component
public class GenresDbStorage implements GenreStorage {
    private final JdbcTemplate jdbcTemplate;
    private static final Logger log = LoggerFactory.getLogger(FilmController.class);

    @Autowired
    public GenresDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Genre> findAllGenres() {
        String sql = "SELECT * FROM genres";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new GenreMapper().mapRow(rs, rowNum)).stream()
                .sorted(Comparator.comparing(Genre::getId)).toList();
    }

    @Override
    public Genre findGenreById(int id) {
        String sql = "SELECT * FROM genres WHERE id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new GenreMapper().mapRow(rs, rowNum), id).stream()
                .findAny().orElseThrow(() -> new NotFoundException("Genre not found"));
    }

    @Override
    public List<Genre> findAllGenresByFilm(Long id) {
        String sql = "SELECT g.id, g.name FROM genres g JOIN films_Genres fg ON g.id = fg.genre_id " +
                "JOIN films f ON fg.film_id = f.film_id WHERE f.FILM_ID =?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new GenreMapper().mapRow(rs, rowNum), id);
    }
}
