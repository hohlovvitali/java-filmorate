package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.EventOperation;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.friend.FriendStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;

@Service
public class UserService {

    private final UserStorage userStorage;
    private final FriendStorage friendStorage;
    private final EventService eventService;
    private static final Logger log = LoggerFactory.getLogger(FilmController.class);

    public UserService(@Qualifier("userDbStorage") UserStorage userStorage, FriendStorage friendStorage,EventService eventService) {
        this.userStorage = userStorage;
        this.friendStorage = friendStorage;
        this.eventService = eventService;
    }

    public Collection<User> findAll() {
        log.info("Вывод списка всех пользователей");
        return userStorage.findAll();
    }

    public User create(User user) {
        log.info("Создание нового пользователя");
        return userStorage.create(user);
    }

    public User update(User newUser) {
        log.trace("Обновление данных пользователя");
        return userStorage.update(newUser);
    }

    public Collection<User> getFriends(Long userId) {
        log.info("Вывод списка всех друзей пользователя c id={}", userId);
        checkUserId(userId);
        return friendStorage.findAllFriends(userId);
    }

    public void addFriend(Long userId, Long friendId) {
        log.info("Добавление друга c id={} пользователю с id={}", friendId, userId);
        checkUserId(userId);
        checkUserId(friendId);
        friendStorage.addFriend(userId, friendId);
        eventService.addEvent(EventType.FRIEND, EventOperation.ADD,userId, friendId);
    }

    public void deleteFriend(Long userId, Long friendId) {
        log.info("Удаление пользователем с id={} из списка друзей пользователя с id={}", userId, friendId);
        checkUserId(userId);
        checkUserId(friendId);
        friendStorage.removeFriend(userId, friendId);
        eventService.addEvent(EventType.FRIEND, EventOperation.REMOVE,userId, friendId);
    }

    public User getUserById(Long userId) {
        log.info("Вывод пользователя c id={}", userId);
        return userStorage.getUserById(userId);
    }

    public void deleteUserById(Long userId) {
        log.info("Удаление пользователя с id={} ", userId);
        checkUserId(userId);
        userStorage.deleteUserById(userId);
    }

    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        log.info("Вывод списка общих друзей пользователя с id={} и пользователя с id={}", userId, otherId);
        checkUserId(userId);
        checkUserId(otherId);
        return friendStorage.findCommonFriends(userId, otherId);
    }

    private void checkUserId(Long userId) {
        try {
            userStorage.getUserById(userId);
        } catch (NotFoundException e) {
            log.warn("Пользователь с id = {} не найден", userId);
            throw new NotFoundException("User not found");
        }
    }
}
