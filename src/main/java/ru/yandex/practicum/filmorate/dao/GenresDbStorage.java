package ru.yandex.practicum.filmorate.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mappers.GenreMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;

import java.util.*;

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

    @Override
    public Map<Long, List<Genre>> findAllGenresForFilmCollection(Collection<Film> films) {
        final String sql = "select fg.film_id as film_id, g.id as genre_id, g.name as name from films_Genres fg " +
                "left join genres g on fg.genre_id = g.id where fg.film_id in (%s)";

        Map<Long, List<Genre>> filmGenresMap = new HashMap<>();
        Collection<String> ids = films.stream().map(film -> String.valueOf(film.getId())).toList();

        jdbcTemplate.query(String.format(sql, String.join(",", ids)), rs -> {
            Genre genre = Genre
                    .builder()
                    .id(rs.getInt("genre_id"))
                    .name(rs.getString("name"))
                    .build();

            Long filmId = rs.getLong("film_id");

            filmGenresMap.putIfAbsent(filmId, new ArrayList<>());
            filmGenresMap.get(filmId).add(genre);
        });

        return filmGenresMap;
    }
}
