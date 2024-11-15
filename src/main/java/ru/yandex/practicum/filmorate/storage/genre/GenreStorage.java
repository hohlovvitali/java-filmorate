package ru.yandex.practicum.filmorate.storage.genre;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface GenreStorage {
    List<Genre> findAllGenres();

    Genre findGenreById(int id);

    List<Genre> findAllGenresByFilm(Long id);

    Map<Long, List<Genre>> findAllGenresForFilmCollection(Collection<Film> films);
}
