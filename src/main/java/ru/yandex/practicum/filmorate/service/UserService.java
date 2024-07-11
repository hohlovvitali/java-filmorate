package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserStorage userStorage;
    private static final Logger log = LoggerFactory.getLogger(FilmController.class);

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
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
        return userStorage.getFriends(userId);
    }

    public void addFriend(Long userId, Long friendId) {
        log.info("Добавление друга c id={} пользователю с id={}", friendId, userId);
        userStorage.addFriendToUser(userId, friendId);
        log.info("Ответное добавление друга c id={} пользователю с id={}", userId, friendId);
        userStorage.addFriendToUser(friendId, userId);
    }

    public void deleteFriend(Long userId, Long friendId) {
        log.info("Удаление пользователем с id={} из списка друзей пользователя с id={}", userId, friendId);
        userStorage.deleteFriendFromUser(userId, friendId);
        log.info("Ответное удаление пользователем с id={} из списка друзей пользователя с id={}", friendId, userId);
        userStorage.deleteFriendFromUser(friendId, userId);
    }

    public void deleteUser(Long id) {
        log.info("Удаление пользователем с id={}", id);
        userStorage.delete(id);
    }

    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        log.info("Вывод списка общих друзей пользователя с id={} и пользователя с id={}", userId, otherId);
        Set<Long> userIdFriend = userStorage.getUserById(userId).getFriendsIdSet();
        Set<Long> otherIdFriend = userStorage.getUserById(otherId).getFriendsIdSet();

        Set<Long> commonFriendsId = userIdFriend.stream()
                .distinct()
                .filter(otherIdFriend::contains)
                .collect(Collectors.toSet());

        return commonFriendsId.stream()
                .map(userStorage::getUserById)
                .collect(Collectors.toSet());
    }
}
