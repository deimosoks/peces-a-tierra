package org.icc.pecesatierra.utils.models;

import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportResponseDto<T> {

    private List<T> data;
    private int totalElements;

}
