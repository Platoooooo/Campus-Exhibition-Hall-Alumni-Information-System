package com.campus.exhibition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 轮播方案创建/更新
 */
@Data
public class CarouselSaveRequest {

    @NotBlank(message = "方案名称不能为空")
    private String name;

    @NotNull(message = "停留时长不能为空")
    private Integer intervalSec;

    /** fade / slide / zoom */
    private String effect;

    /** sort / random / time */
    private String orderType;

    /** 是否默认方案 */
    private Integer isDefault;
}
