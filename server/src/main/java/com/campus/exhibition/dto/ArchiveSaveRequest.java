package com.campus.exhibition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 档案创建/更新请求
 */
@Data
public class ArchiveSaveRequest {

    @NotNull(message = "请选择校友")
    private Long alumniId;

    @NotNull(message = "请选择分类")
    private Long categoryId;

    @NotBlank(message = "标题不能为空")
    private String title;

    private String content;

    private LocalDate eventDate;

    @NotNull(message = "所属学院不能为空")
    private Long collegeId;

    /** 已有媒体 ID 列表（排序用） */
    private java.util.List<Long> mediaIds;
}
