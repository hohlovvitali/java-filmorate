package ru.yandex.practicum.filmorate.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ServerErrorException;
import ru.yandex.practicum.filmorate.mappers.GradeReviewMapper;
import ru.yandex.practicum.filmorate.mappers.ReviewMapper;
import ru.yandex.practicum.filmorate.model.GradeReview;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReviewDbStorage {
    private final JdbcTemplate jdbc;
    private final ReviewMapper mapperReview;
    private final GradeReviewMapper mapperGrade;

    public Optional<Review> getReviewById(Long id) {
        try {
            String sql = "SELECT id, content, type, user_id, film_id, usefulness_rating FROM reviews WHERE id = ?";
            Review review = jdbc.queryForObject(sql, mapperReview, id);
            return Optional.ofNullable(review);
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    public Review create(Review review) {
        try {
            String sql = "INSERT INTO reviews(content, type, user_id, film_id, usefulness_rating) VALUES(?, ?, ?, ?, 0);";
            Long id = insert(sql, review.getContent(), Review.getStringType(review.getIsPositive()),
                    review.getUserId(), review.getFilmId());
            review.setReviewId(id);
            return review;
        } catch (DuplicateKeyException exception) {
            throw new DuplicatedDataException("Пользователь с id = " + review.getUserId() + " уже оставил отзыв к " +
                    "фильму с id = " + review.getFilmId());
        }
    }

    public Review update(Review review) {
        try {
            String sql = "UPDATE reviews SET content = ?, type = ? WHERE id = ?";
            int rowsUpdated = jdbc.update(sql, review.getContent(), Review.getStringType(review.getIsPositive()),
                    review.getReviewId());
            if (rowsUpdated == 0) {
                throw new ServerErrorException("Не удалось обновить данные");
            }
            return getReviewById(review.getReviewId()).get();
        } catch (DuplicateKeyException exp) {
            throw new DuplicatedDataException("Пользователь с id = " + review.getUserId() + " уже оставил отзыв к " +
                    "фильму с id = " + review.getFilmId());
        } catch (DataIntegrityViolationException exception) {
            throw new NotFoundException(exception.getMessage());
        }
    }

    public List<Review> getAll(int count) {
        String sql = "SELECT id, content, type, user_id, film_id, usefulness_rating FROM reviews ORDER BY " +
                "usefulness_rating DESC LIMIT ?";
        return jdbc.query(sql, mapperReview, count);
    }

    public List<Review> getFilmReviews(Long id, int count) {
        String sql = "SELECT id, content, type, user_id, film_id, usefulness_rating FROM reviews WHERE film_id = ? " +
                "ORDER BY usefulness_rating DESC LIMIT ?";
        return jdbc.query(sql, mapperReview, id, count);
    }

    public boolean deleteReview(Long id) {
        String sql = "DELETE FROM reviews WHERE id = ?";
        int rowDeleted = jdbc.update(sql, id);
        return rowDeleted > 0;
    }

    public void increaseUseful(int count, Long id) {
        String sql = "UPDATE reviews SET usefulness_rating = usefulness_rating + ? WHERE id = ?";
        int rowsUpdated = jdbc.update(sql, count, id);
        if (rowsUpdated == 0) {
            throw new ServerErrorException("Не удалось обновить данные");
        }
    }

    public void decreaseUseful(int count, Long id) {
        String sql = "UPDATE reviews SET usefulness_rating = usefulness_rating - ? WHERE id = ?";
        int rowsUpdated = jdbc.update(sql, count, id);
        if (rowsUpdated == 0) {
            throw new ServerErrorException("Не удалось обновить данные");
        }
    }

    public void addGradeReview(Long reviewID, Long userId, String grade) {
        String sql = "INSERT INTO reviews_grade(review_id, user_id, grade) VALUES(?, ?, ?)";
        int rowsUpdated = jdbc.update(sql, reviewID, userId, grade);
        if (rowsUpdated == 0) {
            throw new ServerErrorException("Не удалось обновить данные");
        }
    }

    public void deleteGradeReview(Long reviewID, Long userId) {
        String sql = "DELETE FROM reviews_grade WHERE review_id = ? AND user_id = ?";
        jdbc.update(sql, reviewID, userId);
    }

    public void updateGrandReview(String grade, Long reviewID, Long userId) {
        String sql = "UPDATE reviews_grade SET grade = ? WHERE review_id = ? AND  user_id = ?";
        int rowsUpdated = jdbc.update(sql, grade, reviewID, userId);
        if (rowsUpdated == 0) {
            throw new ServerErrorException("Не удалось обновить данные");
        }
    }

    public Optional<GradeReview> getGrandReview(Long reviewID, Long userId) {
        try {
            String sql = "SELECT review_id, user_id, grade FROM reviews_grade WHERE review_id = ? AND user_id = ?";
            GradeReview gradeReview = jdbc.queryForObject(sql, mapperGrade, reviewID, userId);
            return Optional.ofNullable(gradeReview);
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }


    private long insert(String query, Object... params) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection
                    .prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            for (int idx = 0; idx < params.length; idx++) {
                ps.setObject(idx + 1, params[idx]);
            }
            return ps;
        }, keyHolder);

        Long id = keyHolder.getKeyAs(Long.class);
        if (id != null) {
            return id;
        } else {
            throw new ServerErrorException("Не удалось сохранить данные");
        }
    }
}
