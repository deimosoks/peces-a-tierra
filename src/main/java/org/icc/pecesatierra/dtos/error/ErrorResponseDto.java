package org.icc.pecesatierra.dtos.error;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorResponseDto {
    LocalDateTime localDateTime;
    int status;
    String error;
    String message;
    String path;
}
