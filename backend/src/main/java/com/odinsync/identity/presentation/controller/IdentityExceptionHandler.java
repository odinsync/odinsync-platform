package com.odinsync.identity.presentation.controller;

import com.odinsync.identity.domain.exception.EmailAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.odinsync.identity.presentation.dto.ErrorResponse;

@RestControllerAdvice(assignableTypes = RegisterOrganizationController.class)
class IdentityExceptionHandler {

	@ExceptionHandler(EmailAlreadyExistsException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	ErrorResponse handleDuplicateEmail(EmailAlreadyExistsException exception) {
		return new ErrorResponse(exception.getMessage());
	}
}
