package ru.yandex.practicum.filmorate.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.mappers.EventMapper;
import ru.yandex.practicum.filmorate.model.Event;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class EventRepository {
    private final JdbcTemplate jdbcTemplate;

    public Collection<Event> findByUserId(Long userId) {
        final String sql = "select * from events where user_id = ?";
        return jdbcTemplate.query(sql, new EventMapper(), userId);
    }

    public void createEvent(Event event) {
        String sql = "INSERT INTO events ( user_id, entity_id, operation, event_type, timestamp) VALUES (?,?,?,?,?)";
        jdbcTemplate.update(sql, event.getUserId(), event.getEntityId(), event.getOperation().toString(), event.getEventType().toString(), event.getTimestamp());
    }
}
