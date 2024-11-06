package ru.yandex.practicum.filmorate.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.mappers.RatingMapper;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Rating;
import ru.yandex.practicum.filmorate.storage.rating.RatingStorage;

import java.util.List;

@Component
public class RatingDbStorage implements RatingStorage {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public RatingDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Rating> findAllMpa() {
        String sql = "SELECT * FROM ratings";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new RatingMapper().mapRow(rs, rowNum));
    }

    @Override
    public Rating findMpaById(int id) {
        String sql = "SELECT * FROM ratings WHERE rating_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new RatingMapper().mapRow(rs, rowNum), id).stream()
                .findAny().orElseThrow(() -> new NotFoundException("Rating not found"));
    }
}
