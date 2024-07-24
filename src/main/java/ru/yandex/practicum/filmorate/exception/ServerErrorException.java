package ru.yandex.practicum.filmorate.exception;

public class ServerErrorException extends RuntimeException {
    public ServerErrorException(String msg) {
        super(msg);
    }
}
