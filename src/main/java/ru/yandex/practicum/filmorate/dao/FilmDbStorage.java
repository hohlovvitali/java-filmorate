package ru.yandex.practicum.filmorate.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mappers.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Rating;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.rating.RatingStorage;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

@Component("filmDbStorage")
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final RatingStorage ratingStorage;
    private final GenreStorage genreStorage;
    private static final Logger log = LoggerFactory.getLogger(FilmStorage.class);

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbcTemplate, RatingStorage ratingStorage, GenreStorage genreStorage) {
        this.jdbcTemplate = jdbcTemplate;
        this.genreStorage = genreStorage;
        this.ratingStorage = ratingStorage;
    }

    @Override
    public Collection<Film> findAll() {
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.rating_id, r.rating_name " +
                "FROM films AS f JOIN ratings AS r ON f.rating_id=r.rating_id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new FilmMapper().mapRow(rs, rowNum));
    }

    @Override
    public Film create(Film film) {
        checkRating(film.getMpa().getId());
        String sql = "INSERT INTO films (name, description, release_date, duration, rating_id) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(sql, new String[]{"film_id"});
                    ps.setString(1, film.getName());
                    ps.setString(2, film.getDescription());
                    ps.setDate(3, Date.valueOf(film.getReleaseDate()));
                    ps.setInt(4, film.getDuration());
                    ps.setInt(5, film.getMpa().getId());
                    return ps;
                }, keyHolder);
        film.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        updateGenres(film.getGenres(), film.getId());
        return film;
    }

    @Override
    public Film update(Film film) {
        checkRating(film.getMpa().getId());
        Long id = film.getId();
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, rating_id = ? " +
                "WHERE film_id = ?";
        try {
            this.getFilmById(id);
        } catch (NotFoundException e) {
            log.warn("Фильм с id = {} не найден", id);
            throw new NotFoundException("Film not found");
        }

        jdbcTemplate.update(sql, film.getName(), film.getDescription(), film.getReleaseDate(), film.getDuration(),
                film.getMpa().getId(), id);

        updateGenres(film.getGenres(), id);
        return film;
    }

    @Override
    public void deleteFilmById(Long id) {
        String sql = "DELETE FROM films WHERE film_id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public Film getFilmById(Long id) {
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.rating_id, r.rating_name " +
                "FROM films AS f JOIN ratings AS r ON f.rating_id=r.rating_id WHERE film_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new FilmMapper().mapRow(rs, rowNum), id).stream()
                .findAny().orElseThrow(() -> new NotFoundException("Film not found"));
    }

    public Collection<Film> getPopularFilms(Integer count) {
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.rating_id, r.rating_name " +
                "FROM films AS f JOIN ratings AS r ON f.rating_id=r.rating_id " +
                "LEFT JOIN films_Likes ON f.film_id = films_Likes.film_id " +
                "GROUP BY f.film_id " +
                "ORDER BY COUNT(films_Likes.film_id) DESC " +
                "LIMIT ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new FilmMapper().mapRow(rs, rowNum), count);
    }

    public Collection<Film> getCommonFilms(Long userId, Long friendId) {
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.rating_id, r.rating_name " +
                "FROM films AS f JOIN ratings AS r ON f.rating_id=r.rating_id " +
                "LEFT JOIN films_Likes ON f.film_id = films_Likes.film_id " +
                "WHERE f.film_id IN (SELECT film_id FROM films_Likes WHERE user_id = ? GROUP BY film_id) AND " +
                "f.film_id IN (SELECT film_id FROM films_Likes WHERE user_id = ?) " +
                "GROUP BY f.film_id " +
                "ORDER BY COUNT(films_Likes.film_id) DESC;";
        Collection<Film> films = jdbcTemplate.query(sql, (rs, rowNum) -> new FilmMapper().mapRow(rs, rowNum), userId, friendId);
        return setFilmGenres(films);
    }

    private void updateGenres(List<Genre> genres, Long id) {
        if (genres == null) {
            return;
        }

        for (Genre genre : genres) {
            checkGenre(genre.getId());
        }

        jdbcTemplate.update("DELETE FROM films_Genres WHERE film_id = ?", id);

        genres = genres.stream().distinct().toList();

        if (!genres.isEmpty()) {
            String sql = "INSERT INTO films_Genres (film_id, genre_id) VALUES (?, ?)";
            Genre[] g = genres.toArray(new Genre[genres.size()]);
            List<Genre> finalGenres = genres;
            jdbcTemplate.batchUpdate(
                    sql,
                    new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            ps.setLong(1, id);
                            ps.setInt(2, g[i].getId());
                        }

                        public int getBatchSize() {
                            return finalGenres.size();
                        }
                    });
        }
    }

    private void checkRating(int rating_id) {
        List<Rating> ratingList = ratingStorage.findAllMpa();
        for (Rating rating : ratingList) {
            if (rating.getId() == rating_id) {
                return;
            }
        }

        throw new ValidationException("Incorrect rating_id = " + rating_id + ".");
    }

    private void checkGenre(int genre_id) {
        List<Genre> genreList = genreStorage.findAllGenres();
        for (Genre genre : genreList) {
            if (genre.getId() == genre_id) {
                return;
            }
        }

        log.warn("Жанр с id = {} не найден", genre_id);
        throw new ValidationException("Incorrect genre_id = " + genre_id + ".");
    }

    private Collection<Film> setFilmGenres(Collection<Film> films) {
        Map<Long, List<Genre>> filmGenresMap = genreStorage.findAllGenresForFilmCollection(films);
        films.forEach(film -> {
            Long filmId = film.getId();
            film.setGenres(filmGenresMap.getOrDefault(filmId, new ArrayList<>()));
        });

        return films;
    }
}
