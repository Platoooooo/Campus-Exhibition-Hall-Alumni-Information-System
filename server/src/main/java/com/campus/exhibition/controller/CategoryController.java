package com.campus.exhibition.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.exhibition.common.BizException;
import com.campus.exhibition.common.OperLog;
import com.campus.exhibition.common.R;
import com.campus.exhibition.entity.ArchiveCategory;
import com.campus.exhibition.mapper.ArchiveCategoryMapper;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资料分类字典
 */
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final ArchiveCategoryMapper categoryMapper;

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic') or hasAuthority('college')")
    public R<List<ArchiveCategory>> listAll() {
        return R.ok(categoryMapper.selectList(
                new LambdaQueryWrapper<ArchiveCategory>()
                        .orderByAsc(ArchiveCategory::getSort)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin')")
    @OperLog("新增分类")
    public R<ArchiveCategory> create(@Valid @RequestBody CategorySaveRequest request) {
        ArchiveCategory entity = new ArchiveCategory();
        entity.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        entity.setName(request.getName());
        entity.setIcon(request.getIcon());
        entity.setSort(request.getSort() != null ? request.getSort() : 0);
        entity.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        categoryMapper.insert(entity);
        return R.ok(entity);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    @OperLog("编辑分类")
    public R<ArchiveCategory> update(@PathVariable Long id,
                                      @Valid @RequestBody CategorySaveRequest request) {
        ArchiveCategory entity = categoryMapper.selectById(id);
        if (entity == null) throw new BizException(404, "分类不存在");
        entity.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        entity.setName(request.getName());
        entity.setIcon(request.getIcon());
        entity.setSort(request.getSort() != null ? request.getSort() : 0);
        entity.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        categoryMapper.updateById(entity);
        return R.ok(entity);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    @OperLog("删除分类")
    public R<Void> delete(@PathVariable Long id) {
        if (categoryMapper.selectById(id) == null) throw new BizException(404, "分类不存在");
        categoryMapper.deleteById(id);
        return R.ok();
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('admin')")
    @OperLog("启停分类")
    public R<Void> toggle(@PathVariable Long id) {
        ArchiveCategory entity = categoryMapper.selectById(id);
        if (entity == null) throw new BizException(404, "分类不存在");
        entity.setStatus(entity.getStatus() == 1 ? 0 : 1);
        categoryMapper.updateById(entity);
        return R.ok();
    }

    @Data
    public static class CategorySaveRequest {
        private Long parentId;
        @jakarta.validation.constraints.NotBlank(message = "名称不能为空")
        private String name;
        private String icon;
        private Integer sort;
        private Integer status;
    }
}
