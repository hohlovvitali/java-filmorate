package ru.yandex.practicum.filmorate.controller;

import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.service.DirectorService;

import java.util.Collection;


@RestController
@RequestMapping("/directors")
public class DirectorController {

    private final DirectorService directorService;

    public DirectorController(DirectorService directorService) {
        this.directorService = directorService;
    }

    @PostMapping
    public Director createDirector(@RequestBody Director director) {
        return directorService.createDirector(director);
    }

    @PutMapping
    public Director updateDirector(@RequestBody Director director) {
        return directorService.updateDirector(director);
    }

    @DeleteMapping("/{id}")
    public void deleteDirector(@PathVariable Long id) {
        directorService.deleteDirector(id);
    }

    @GetMapping("/{id}")
    public Director getDirectorById(@PathVariable Long id) {
        return directorService.getDirectorById(id);
    }

    @GetMapping
    public Collection<Director> getAllDirectors() {
        return directorService.getAllDirectors();
    }
}