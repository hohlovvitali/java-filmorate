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
    private static final String INSERT_QUERY = "INSERT INTO reviews(content, type, user_id, film_id," +
            " usefulness_rating) VALUES(?, ?, ?, ?, 0);";
    private static final String UPDATE_QUERY = "UPDATE reviews SET content = ?, type = ? WHERE id = ?";
    private static final String FIND_REVIEW_BY_ID_QUERY = "SELECT id, content, type, user_id, film_id, " +
            " usefulness_rating FROM reviews WHERE id = ?";
    private static final String FIND_ALL_REVIEWS_QUERY = "SELECT id, content, type, user_id, film_id, usefulness_rating " +
            " FROM reviews ORDER BY usefulness_rating DESC LIMIT ?";
    private static final String FIND_REVIEWS_BY_FILM_ID_QUERY = "SELECT id, content, type, user_id, film_id, " +
            "usefulness_rating FROM reviews WHERE film_id = ? ORDER BY usefulness_rating DESC LIMIT ?";
    private static final String DELETE_QUERY = "DELETE FROM reviews WHERE id = ?";
    private static final String INCREASE_USEFUL_QUERY = "UPDATE reviews SET usefulness_rating = usefulness_rating + ? " +
            "WHERE id = ?";
    private static final String DECREASE_USEFUL_QUERY = "UPDATE reviews SET usefulness_rating = usefulness_rating - ? " +
            "WHERE id = ?";

    private static final String ADD_GRADE_USER_QUERY = "INSERT INTO reviews_grade(review_id, user_id, grade) VALUES(?, " +
            "?, ?)";
    private static final String DELETE_GRADE_USER_QUERY = "DELETE FROM reviews_grade WHERE review_id = ? AND " +
            "user_id = ?";
    private static final String UPDATE_GRADE_USER_QUERY = "UPDATE reviews_grade SET grade = ? WHERE review_id = ? " +
            "AND  user_id = ?";
    private static final String FIND_GRAND_REVIEW_QUERY = "SELECT review_id, user_id, grade FROM reviews_grade WHERE" +
            " review_id = ? AND user_id = ?";

    public Optional<Review> getReviewById(Long id) {
        try {
            Review review = jdbc.queryForObject(FIND_REVIEW_BY_ID_QUERY, mapperReview, id);
            return Optional.ofNullable(review);
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    public Review create(Review review) {
        try {
            Long id = insert(INSERT_QUERY, review.getContent(), Review.getStringType(review.getIsPositive()),
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
            int rowsUpdated = jdbc.update(UPDATE_QUERY, review.getContent(), Review.getStringType(review.getIsPositive()),
                    review.getReviewId());
            if (rowsUpdated == 0) {
                throw new ServerErrorException("Не удалось обновить данные");
            }
            return review;
        } catch (DuplicateKeyException exp) {
            throw new DuplicatedDataException("Пользователь с id = " + review.getUserId() + " уже оставил отзыв к " +
                    "фильму с id = " + review.getFilmId());
        } catch (DataIntegrityViolationException exception) {
            throw new NotFoundException(exception.getMessage());
        }
    }

    public List<Review> getAll(int count) {
        return jdbc.query(FIND_ALL_REVIEWS_QUERY, mapperReview, count);
    }

    public List<Review> getFilmReviews(Long id, int count) {
        return jdbc.query(FIND_REVIEWS_BY_FILM_ID_QUERY, mapperReview, id, count);
    }

    public boolean deleteReview(Long id) {
        int rowDeleted = jdbc.update(DELETE_QUERY, id);
        return rowDeleted > 0;
    }

    public void increaseUseful(int count, Long id) {
        int rowsUpdated = jdbc.update(INCREASE_USEFUL_QUERY, count, id);
        if (rowsUpdated == 0) {
            throw new ServerErrorException("Не удалось обновить данные");
        }
    }

    public void decreaseUseful(int count, Long id) {
        int rowsUpdated = jdbc.update(DECREASE_USEFUL_QUERY, count, id);
        if (rowsUpdated == 0) {
            throw new ServerErrorException("Не удалось обновить данные");
        }
    }

    public void addGradeReview(Long reviewID, Long userId, String grade) {
        int rowsUpdated = jdbc.update(ADD_GRADE_USER_QUERY, reviewID, userId, grade);
        if (rowsUpdated == 0) {
            throw new ServerErrorException("Не удалось обновить данные");
        }
    }

    public void deleteGradeReview(Long reviewID, Long userId) {
        jdbc.update(DELETE_GRADE_USER_QUERY, reviewID, userId);
    }

    public void updateGrandReview(String grade, Long reviewID, Long userId) {
        int rowsUpdated = jdbc.update(UPDATE_GRADE_USER_QUERY, grade, reviewID, userId);
        if (rowsUpdated == 0) {
            throw new ServerErrorException("Не удалось обновить данные");
        }
    }

    public Optional<GradeReview> getGrandReview(Long reviewID, Long userId) {
        try {
            GradeReview gradeReview = jdbc.queryForObject(FIND_GRAND_REVIEW_QUERY, mapperGrade, reviewID, userId);
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
