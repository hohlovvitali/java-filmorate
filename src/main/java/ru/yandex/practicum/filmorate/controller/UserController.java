package ru.yandex.practicum.filmorate.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController()
@RequestMapping("/users")
public class UserController {
    private final Map<Long, User> users = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(FilmController.class);

    @GetMapping
    public Collection<User> findAll() {
        log.info("Вывод списка всех пользователей");
        return users.values();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        log.trace("Добавление нового фильма");
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
        log.trace("Установление id пользователя: {}", user.getId());

        users.put(user.getId(), user);
        log.info("Добавление нового пользователя {} c id: {}", user.getLogin(), user.getId());

        return user;
    }

    @PutMapping
    public User update(@RequestBody User newUser) {
        log.trace("Обновление данных пользователя");
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
}
