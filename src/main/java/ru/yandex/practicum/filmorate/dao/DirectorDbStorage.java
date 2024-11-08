package ru.yandex.practicum.filmorate.dao;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.mappers.DirectorMapper;

import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.Objects;

@Component
public class DirectorDbStorage implements DirectorStorage {
    private final JdbcTemplate jdbcTemplate;
    private final DirectorMapper directorMapper = new DirectorMapper();

    public DirectorDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Director create(Director director) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO directors (name) VALUES (?)", new String[]{"director_id"});
            statement.setString(1, director.getName());
            return statement;
        }, keyHolder);
        director.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());

        return director;
    }

    public Director update(Director director) {
        String sql = "SELECT COUNT(*) FROM directors WHERE director_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, director.getId());

        if (count == null || count == 0) {
            throw new NotFoundException("Режиссер с id " + director.getId() + " не найден");
        }

        sql = "UPDATE directors SET name = ? WHERE director_id = ?";
        jdbcTemplate.update(sql, director.getName(), director.getId());

        return director;
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM directors WHERE director_id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public Director getById(Long id) {
        String sql = "SELECT * FROM directors WHERE director_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, directorMapper, id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Режиссер с ID " + id + " не найден.");
        }
    }

    @Override
    public Collection<Director> findAll() {
        String sql = "SELECT * FROM directors";
        return jdbcTemplate.query(sql, new DirectorMapper());
    }
}