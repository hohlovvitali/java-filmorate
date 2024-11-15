package ru.yandex.practicum.filmorate.mappers;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.GradeReview;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class GradeReviewMapper implements RowMapper<GradeReview> {
    @Override
    public GradeReview mapRow(ResultSet rs, int rowNum) throws SQLException {
        return GradeReview.builder()
                .reviewId(rs.getLong("review_id"))
                .userId(rs.getLong("user_id"))
                .grade(rs.getString("grade"))
                .build();
    }
}
