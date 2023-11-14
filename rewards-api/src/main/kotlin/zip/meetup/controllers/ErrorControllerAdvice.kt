package zip.meetup.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ErrorControllerAdvice {

    @ExceptionHandler
    fun handleNotFoundException(
        e: NotFoundException
    ) = ProblemDetail.forStatus(HttpStatus.NOT_FOUND)
        .apply {
            title = "resource_not_found"
            detail = e.message ?: "Resource Not Found"
        }
}

class NotFoundException(msg: String) : RuntimeException(msg)