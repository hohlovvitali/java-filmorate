package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dao.EventRepository;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventOperation;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.Instant;
import java.util.Collection;

@Service
public class EventService {

    private final UserStorage userStorage;
    private final EventRepository eventRepository;

    public EventService(@Qualifier("userDbStorage") UserStorage userStorage, EventRepository eventRepository) {
        this.userStorage = userStorage;
        this.eventRepository = eventRepository;
    }

    public Collection<Event> findByUserId(Long userId) {
        userStorage.getUserById(userId);
        return eventRepository.findByUserId(userId);
    }

    public void addEvent(EventType eventType, EventOperation eventOperation, Long userId, Long entityId) {
        Event newEvent = Event.builder()
                .eventType(eventType)
                .operation(eventOperation)
                .userId(userId)
                .entityId(entityId)
                .timestamp(Instant.now().toEpochMilli())
                .build();
        eventRepository.createEvent(newEvent);
    }
}
