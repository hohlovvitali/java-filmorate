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
import ru.yandex.practicum.filmorate.model.Director;
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
    private final DirectorDbStorage directorDbStorage;
    private static final Logger log = LoggerFactory.getLogger(FilmStorage.class);

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbcTemplate, RatingStorage ratingStorage, GenreStorage genreStorage, DirectorDbStorage directorDbStorage) {
        this.jdbcTemplate = jdbcTemplate;
        this.genreStorage = genreStorage;
        this.ratingStorage = ratingStorage;
        this.directorDbStorage = directorDbStorage;
    }

    @Override
    public Collection<Film> findAll() {
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.rating_id, r.rating_name " +
                "FROM films AS f JOIN ratings AS r ON f.rating_id=r.rating_id";
        List<Film> films = jdbcTemplate.query(sql, (rs, rowNum) -> new FilmMapper().mapRow(rs, rowNum));
        setFilmGenres(films);
        addDirectorsToFilms(films);

        return films;
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
        directorDbStorage.updateDirectorsForFilm(film);
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
        directorDbStorage.updateDirectorsForFilm(film);
        return getFilmById(id);
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
        Film film = jdbcTemplate.query(sql, (rs, rowNum) -> new FilmMapper().mapRow(rs, rowNum), id).stream()
                .findAny().orElseThrow(() -> new NotFoundException("Film not found"));
        film.setGenres(genreStorage.findAllGenresByFilm(film.getId()));
        Map<Long, Set<Director>> directorsByFilm = directorDbStorage.getDirectorsForFilms(Collections.singletonList(id));
        film.setDirectors(directorsByFilm.getOrDefault(id, new HashSet<>()));
        return film;
    }

    @Override
    public List<Film> getFilmsByDirector(Long directorId, String sortBy) {
        StringBuilder sql = new StringBuilder(
                "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.rating_id, r.rating_name, " +
                        "COUNT(l.user_id) AS likes " +
                        "FROM films f " +
                        "INNER JOIN film_director fd ON f.film_id = fd.film_id " +
                        "INNER JOIN ratings r ON f.rating_id = r.rating_id " +
                        "LEFT JOIN films_Likes l ON f.film_id = l.film_id " +
                        "WHERE fd.director_id = ? " +
                        "GROUP BY f.film_id, r.rating_name "
        );

        if ("likes".equals(sortBy)) {
            sql.append(" ORDER BY likes DESC");
        } else if ("year".equals(sortBy)) {
            sql.append(" ORDER BY f.release_date");
        }

        List<Film> films = jdbcTemplate.query(sql.toString(), new FilmMapper(), directorId);

        List<Long> filmIds = films.stream().map(Film::getId).toList();
        Map<Long, Set<Director>> directorsByFilm = directorDbStorage.getDirectorsForFilms(filmIds);

        for (Film film : films) {
            film.setDirectors(directorsByFilm.getOrDefault(film.getId(), new HashSet<>()));
        }
        setFilmGenres(films);
        return films;
    }

    public Collection<Film> getPopularFilms(Integer count, Integer genreId, Integer year) {
        String sql = "SELECT f.*, r.rating_name " +
                "FROM films AS f JOIN ratings AS r ON f.rating_id=r.rating_id " +
                "LEFT JOIN films_Likes AS fl ON f.film_id = fl.film_id %s" +
                "GROUP BY f.film_id " +
                "ORDER BY COUNT(fl.film_id) DESC " +
                "LIMIT ?";
        List<Film> films = null;
        if (Objects.nonNull(year) && Objects.nonNull(genreId)) {
            sql = String.format(sql, "LEFT JOIN films_Genres AS fg ON f.film_id = fg.film_id WHERE fg.genre_id = ? AND YEAR(f.release_date) = ?");
            films = jdbcTemplate.query(sql, (rs, rowNum) -> new FilmMapper().mapRow(rs, rowNum), genreId, year, count);
        } else if (Objects.nonNull(genreId)) {
            sql = String.format(sql, "LEFT JOIN films_Genres AS fg ON f.film_id = fg.film_id WHERE fg.genre_id = ?");
            films = jdbcTemplate.query(sql, (rs, rowNum) -> new FilmMapper().mapRow(rs, rowNum), genreId, count);
        } else if (Objects.nonNull(year)) {
            sql = String.format(sql, "WHERE YEAR(f.release_date) = ?");
            films = jdbcTemplate.query(sql, (rs, rowNum) -> new FilmMapper().mapRow(rs, rowNum), year, count);
        } else {
            sql = String.format(sql, "");
            films = jdbcTemplate.query(sql, (rs, rowNum) -> new FilmMapper().mapRow(rs, rowNum), count);
        }
        setFilmGenres(films);
        addDirectorsToFilms(films);
        return films;
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

    @Override
    public List<Film> searchFilms(String query, boolean isDirector, boolean isTitle) {
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.rating_id, r.rating_name " +
                "FROM films AS f JOIN ratings AS r ON f.rating_id = r.rating_id " +
                "LEFT JOIN films_Likes AS fl ON f.film_id = fl.film_id %s" +
                "GROUP BY f.film_id ORDER BY COUNT(fl.film_id) DESC";
        List<Film> films = null;
        if (isDirector && isTitle) {
            sql = String.format(sql, "LEFT JOIN film_director fd ON f.film_id = fd.film_id LEFT JOIN directors AS d " +
                    "ON fd.director_id = d.director_id  WHERE (d.name ILIKE CONCAT('%', ?, '%')) OR " +
                    "(f.name ILIKE CONCAT('%', ?, '%'))");
            films = jdbcTemplate.query(sql, (rs, rowNum) -> new FilmMapper().mapRow(rs, rowNum), query, query);
        } else if (isTitle) {
            sql = String.format(sql, "WHERE f.name ILIKE CONCAT('%', ?, '%')");
            films = jdbcTemplate.query(sql, (rs, rowNum) -> new FilmMapper().mapRow(rs, rowNum), query);
        } else if (isDirector) {
            sql = String.format(sql, "INNER JOIN film_director fd ON f.film_id = fd.film_id LEFT JOIN directors AS d " +
                    "ON fd.director_id = d.director_id WHERE d.name ILIKE CONCAT('%', ?, '%')");
            films = jdbcTemplate.query(sql, (rs, rowNum) -> new FilmMapper().mapRow(rs, rowNum), query);
        }
        setFilmGenres(films);
        addDirectorsToFilms(films);
        return films;
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

    private void addDirectorsToFilms(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }
        List<Long> filmIds = films.stream().map(Film::getId).toList();
        Map<Long, Set<Director>> directorsByFilm = directorDbStorage.getDirectorsForFilms(filmIds);

        for (Film film : films) {
            film.setDirectors(directorsByFilm.getOrDefault(film.getId(), new HashSet<>()));
        }
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
