package ru.yandex.practicum.filmorate.storage.genre;

import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;

public interface GenreStorage {
    List<Genre> findAllGenres();

    Genre findGenreById(int id);

    List<Genre> findAllGenresByFilm(Long id);
}
