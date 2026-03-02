package org.icc.pecesatierra.utils.models;

import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagesResponseDto<T> {

    private List<T> data;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;

}