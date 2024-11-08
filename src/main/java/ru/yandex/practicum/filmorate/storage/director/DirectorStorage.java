package ru.yandex.practicum.filmorate.storage.director;

import ru.yandex.practicum.filmorate.model.Director;

import java.util.Collection;

public interface DirectorStorage {
    Director create(Director director);

    Director update(Director director);

    void delete(Long id);

    Director getById(Long id);

    Collection<Director> findAll();
}