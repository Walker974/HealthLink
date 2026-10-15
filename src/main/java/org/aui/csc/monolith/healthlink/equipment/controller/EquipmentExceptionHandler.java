package org.aui.csc.monolith.healthlink.equipment.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns database rule violations (e.g. a duplicate serial number,
 * which breaks the UNIQUE constraint) into a clean 409 Conflict
 * instead of a 500 error. Same pattern as OrganizationExceptionHandler.
 *
 * The message is generic on purpose: no database details leak to the client (CWE-209).
 */
@RestControllerAdvice(assignableTypes = EquipmentController.class)
public class EquipmentExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleConflict(DataIntegrityViolationException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The equipment conflicts with existing data (for example, a duplicate serial number)."
        );
    }
}
