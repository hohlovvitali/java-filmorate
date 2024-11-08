package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class DirectorService {
    private final DirectorStorage directorStorage;

    public Director createDirector(Director director) {
        return directorStorage.create(director);
    }

    public Director updateDirector(Director director) {
        return directorStorage.update(director);
    }

    public void deleteDirector(Long id) {
        directorStorage.delete(id);
    }

    public Director getDirectorById(Long id) {
        return directorStorage.getById(id);
    }

    public Collection<Director> getAllDirectors() {
        return directorStorage.findAll();
    }
}