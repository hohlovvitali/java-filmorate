package ru.yandex.practicum.filmorate.storage.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.*;

@Component
@Qualifier("InMemoryUserStorage")
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(InMemoryUserStorage.class);

    @Override
    public User create(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Пустой e-mail пользователя");
            throw new ValidationException("E-mail должен быть указан");
        }

        if (!user.getEmail().contains("@")) {
            log.warn("E-mail не содержит знак @: {}", user.getEmail());
            throw new ValidationException("E-mail должен содержать знак @");
        }

        if (checkDuplicateEmail(user.getEmail())) {
            log.warn("E-mail: {} уже занят", user.getEmail());
            throw new DuplicatedDataException("E-mail уже используется");
        }

        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.warn("Неверный формат логина: {}", user.getLogin());
            throw new ValidationException("Логин должен быть указан и не содержать пробелы");
        }

        if (user.getBirthday() != null) {
            if (user.getBirthday().isAfter(LocalDate.now())) {
                log.warn("Указана дата рождения после {}", LocalDate.now());
                throw new ValidationException("Дата рождения не может быть в будущем");
            }
        }

        if (user.getName() == null || user.getName().isBlank()) {
            log.debug("Имя пользователя не указано. Используется логин: {}", user.getLogin());
            user.setName(user.getLogin());
        }

        user.setId(getNextId());
        user.setFriendsIdSet(new TreeSet<>());
        log.trace("Установление id пользователя: {}", user.getId());

        users.put(user.getId(), user);
        log.info("Добавление нового пользователя {} c id: {}", user.getLogin(), user.getId());

        return user;
    }

    public void addFriendToUser(Long idUser, Long idFriend) {
        checkUserById(idUser);

        checkUserById(idFriend);

        log.info("Добавление пользователю с id={} в список друзей пользователя c id={}", idUser, idFriend);
        this.getUserById(idUser).addFriend(idFriend);
    }

    public void deleteFriendFromUser(Long idUser, Long idFriend) {
        checkUserById(idUser);

        checkUserById(idFriend);

        log.info("Удаление id={} из списка друзей пользователя с id={}", idFriend, idUser);
        this.getUserById(idUser).getFriendsIdSet().remove(idFriend);
    }

    @Override
    public void delete(Long userId) {
        checkUserById(userId);

        users.remove(userId);
    }

    @Override
    public User update(User newUser) {
        if (newUser.getId() == null) {
            log.warn("Не указан id пользователя");
            throw new ValidationException("Id должен быть указан");
        }

        if (users.containsKey(newUser.getId())) {
            User oldUser = users.get(newUser.getId());

            if (newUser.getEmail() != null && !newUser.getEmail().isBlank()) {
                if (!newUser.getEmail().contains("@")) {
                    log.warn("Новый E-mail не содержит знак @: {}", newUser.getEmail());
                    throw new ValidationException("E-mail должен содержать знак @");
                }

                if (checkDuplicateEmail(newUser.getEmail()) && !oldUser.getEmail().equals(newUser.getEmail())) {
                    log.warn("Этот E-mail: {} уже занят", newUser.getEmail());
                    throw new DuplicatedDataException("Этот e-mail уже используется");
                }

                log.info("Изменение E-mail пользователя с id={} на {}", oldUser.getId(), newUser.getEmail());
                oldUser.setEmail(newUser.getEmail());
            }

            if (newUser.getLogin() != null && !newUser.getLogin().isBlank() && !newUser.getLogin().contains(" ")) {
                log.info("Обновление логина пользователся с id={} на {}", oldUser.getId(), newUser.getLogin());
                oldUser.setLogin(newUser.getLogin());
            }

            if (newUser.getBirthday() != null) {
                if (newUser.getBirthday().isAfter(LocalDate.now())) {
                    log.info("Обновленная дата рождения указана после {}", LocalDate.now());
                    throw new ValidationException("Дата рождения не может быть в будущем");
                }

                log.info("Обновление даты рождения на {}", newUser.getBirthday());
                oldUser.setBirthday(newUser.getBirthday());
            }

            if (newUser.getName() != null && !newUser.getName().isBlank()) {
                log.info("Обновление имени пользователя с id={} на {}", oldUser.getId(), newUser.getName());
                oldUser.setName(newUser.getName());
            }

            return newUser;

        }

        log.warn("Несуществующий id пользователя: {}", newUser.getId());
        throw new NotFoundException("Пользователь с id = " + newUser.getId() + " не найден");
    }

    public Collection<User> getFriends(Long userId) {
        checkUserById(userId);

        Set<Long> friendsId = this.getUserById(userId).getFriendsIdSet();

        List<User> userFriendList = new ArrayList<>();
        for (Long id : friendsId) {
            userFriendList.add(users.get(id));
        }

        return userFriendList;
    }

    @Override
    public Collection<User> findAll() {
        return users.values();
    }

    @Override
    public User getUserById(Long userId) {
        checkUserById(userId);

        return users.get(userId);
    }

    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    private boolean checkDuplicateEmail(String emailForCheck) {
        return users.values().stream()
                .anyMatch(user -> user.getEmail().equals(emailForCheck));
    }

    private void checkUserById(Long userId) {
        if (userId == null) {
            log.warn("Пустой переданный id фильма");
            throw new ValidationException("Id должен быть указан");
        }

        if (userId < 1) {
            log.warn("Id пользователя должен быть больше 0: {}", userId);
            throw new ValidationException("Id должен быть больше 0");
        }

        if (!users.containsKey(userId)) {
            log.warn("Пользователь с id={} не найден", userId);
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
    }
}
