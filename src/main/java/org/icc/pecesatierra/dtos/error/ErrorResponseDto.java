package org.icc.pecesatierra.dtos.error;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorResponseDto {
    private LocalDateTime localDateTime;
    private int status;
    private String error;
    private String message;
    private String path;
}
