package org.icc.pecesatierra.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.icc.pecesatierra.dtos.error.ErrorResponseDto;
import org.icc.pecesatierra.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalHandlerException {

    @ExceptionHandler(AttendanceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlerAttendanceNotFoundException(HttpServletRequest httpServletRequest,
                                                                               AttendanceNotFoundException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidImageFormatException.class)
    public ResponseEntity<ErrorResponseDto> handlerInvalidImageFormatException(HttpServletRequest httpServletRequest,
                                                                               InvalidImageFormatException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MemberAlreadyRegisteredWithUserException.class)
    public ResponseEntity<ErrorResponseDto> handlerMemberAlreadyRegisteredWithUserException(HttpServletRequest httpServletRequest,
                                                                                            MemberAlreadyRegisteredWithUserException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MemberDeactivatedException.class)
    public ResponseEntity<ErrorResponseDto> handlerMemberDeactivatedException(HttpServletRequest httpServletRequest,
                                                                              MemberDeactivatedException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MemberHasHistoricalRecordException.class)
    public ResponseEntity<ErrorResponseDto> handlerMemberHasHistoricalRecordException(HttpServletRequest httpServletRequest,
                                                                                      MemberHasHistoricalRecordException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlerMemberNotFoundException(HttpServletRequest httpServletRequest,
                                                                           MemberNotFoundException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PermissionNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlerPermissionNotFoundException(HttpServletRequest httpServletRequest,
                                                                               PermissionNotFoundException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RefreshTokenException.class)
    public ResponseEntity<ErrorResponseDto> handlerRefreshTokenException(HttpServletRequest httpServletRequest,
                                                                         RefreshTokenException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RoleHasRelatedUserException.class)
    public ResponseEntity<ErrorResponseDto> handlerRoleHasRelatedUserException(HttpServletRequest httpServletRequest,
                                                                               RoleHasRelatedUserException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlerRoleNotFoundException(HttpServletRequest httpServletRequest,
                                                                         RoleNotFoundException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ServiceDeactivatedException.class)
    public ResponseEntity<ErrorResponseDto> handlerServiceDeactivatedException(HttpServletRequest httpServletRequest,
                                                                               ServiceDeactivatedException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ServiceHasHistoricalRecordException.class)
    public ResponseEntity<ErrorResponseDto> handlerServiceHasHistoricalRecordException(HttpServletRequest httpServletRequest,
                                                                                       ServiceHasHistoricalRecordException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ServicesNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlerServicesNotFoundException(HttpServletRequest httpServletRequest,
                                                                             ServicesNotFoundException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UsernameAlreadyRegister.class)
    public ResponseEntity<ErrorResponseDto> handlerUsernameAlreadyRegister(HttpServletRequest httpServletRequest,
                                                                           UsernameAlreadyRegister exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlerUserNotFoundException(HttpServletRequest httpServletRequest,
                                                                         UserNotFoundException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlerUsernameNotFoundException(HttpServletRequest httpServletRequest,
                                                                             UsernameNotFoundException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserDeactivatedException.class)
    public ResponseEntity<ErrorResponseDto> handlerUserDeactivatedException(HttpServletRequest httpServletRequest,
                                                                            UserDeactivatedException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DeactivateYourselfException.class)
    public ResponseEntity<ErrorResponseDto> handlerDeactivateYourselfException(HttpServletRequest httpServletRequest,
                                                                               DeactivateYourselfException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ServerErrorException.class)
    public ResponseEntity<ErrorResponseDto> handlerServerErrorException(HttpServletRequest httpServletRequest,
                                                                        ServerErrorException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(UpdateYourselfException.class)
    public ResponseEntity<ErrorResponseDto> handlerUpdateYourselfException(HttpServletRequest httpServletRequest,
                                                                           UpdateYourselfException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(BaptismNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlerBaptismNotFoundException(HttpServletRequest httpServletRequest,
                                                                            BaptismNotFoundException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MemberNoteNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlerMemberNoteNotFoundException(HttpServletRequest httpServletRequest,
                                                                               MemberNoteNotFoundException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MemberAlreadyHasRegisteredBaptismActiveException.class)
    public ResponseEntity<ErrorResponseDto> handlerMemberAlreadyHasRegisteredBaptismActiveException(HttpServletRequest httpServletRequest,
                                                                                                    MemberAlreadyHasRegisteredBaptismActiveException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MemberCategoryInUseException.class)
    public ResponseEntity<ErrorResponseDto> handlerMemberCategoryInUseException(HttpServletRequest httpServletRequest,
                                                                                MemberCategoryInUseException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MemberCategoryNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlerMemberCategoryNotFoundException(HttpServletRequest httpServletRequest,
                                                                                   MemberCategoryNotFoundException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MemberTypeInUseException.class)
    public ResponseEntity<ErrorResponseDto> handlerMemberTypeInUseException(HttpServletRequest httpServletRequest,
                                                                            MemberTypeInUseException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MemberTypeNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlerMemberTypeNotFoundException(HttpServletRequest httpServletRequest,
                                                                               MemberTypeNotFoundException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(CategoryWithNameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handlerCategoryWithNameAlreadyExistsException(HttpServletRequest httpServletRequest,
                                                                                          CategoryWithNameAlreadyExistsException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(TypeWithNameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handlerCategoryTypeWithNameAlreadyExistsException(HttpServletRequest httpServletRequest,
                                                                                              TypeWithNameAlreadyExistsException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handlerMethodArgumentNotValidException(HttpServletRequest httpServletRequest,
                                                                                   MethodArgumentNotValidException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getBindingResult()
                        .getFieldErrors()
                        .get(0)
                        .getDefaultMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MemberNoHasCategoryForThisSubCategoryException.class)
    public ResponseEntity<ErrorResponseDto> handlerMemberNoHasCategoryForThisSubCategoryException(HttpServletRequest httpServletRequest,
                                                                                   MemberNoHasCategoryForThisSubCategoryException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MemberSubCategoryNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlerMemberSubCategoryNotFoundException(HttpServletRequest httpServletRequest,
                                                                                                  MemberSubCategoryNotFoundException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MemberSubCategoryInUseException.class)
    public ResponseEntity<ErrorResponseDto> handlerMemberSubCategoryInUseException(HttpServletRequest httpServletRequest,
                                                                                      MemberSubCategoryInUseException exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(exception.getMessage())
                .path(httpServletRequest.getRequestURI())
                .build();

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handlerGenericException(HttpServletRequest httpServletRequest,
                                                                                   Exception exception) {
        ErrorResponseDto errorResponseDto = ErrorResponseDto.builder()
                .localDateTime(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Error inesperado, por favor intente mas tarde.")
                .path(httpServletRequest.getRequestURI())
                .build();

        System.out.println("==========================");
        System.out.println("Exception: " + exception.getMessage());
        System.out.println("==========================");

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

}
