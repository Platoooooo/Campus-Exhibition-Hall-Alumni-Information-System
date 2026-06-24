package com.campus.exhibition.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.common.R;
import com.campus.exhibition.dto.AlumniQuery;
import com.campus.exhibition.dto.AlumniSaveRequest;
import com.campus.exhibition.service.AlumniService;
import com.campus.exhibition.service.impl.AlumniServiceImpl;
import com.campus.exhibition.vo.AlumniVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 校友管理
 */
@RestController
@RequestMapping("/api/alumni")
@RequiredArgsConstructor
public class AlumniController {

    private final AlumniService alumniService;
    private final AlumniServiceImpl alumniServiceImpl;

    @GetMapping
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic') or hasAuthority('college')")
    public R<Page<AlumniVO>> page(
            AlumniQuery query,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(alumniService.page(query, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic') or hasAuthority('college')")
    public R<AlumniVO> getById(@PathVariable Long id) {
        return R.ok(alumniService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic') or hasAuthority('college')")
    public R<AlumniVO> create(@Valid @RequestBody AlumniSaveRequest request) {
        return R.ok(alumniService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic') or hasAuthority('college')")
    public R<AlumniVO> update(@PathVariable Long id, @Valid @RequestBody AlumniSaveRequest request) {
        return R.ok(alumniService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public R<Void> delete(@PathVariable Long id) {
        alumniService.delete(id);
        return R.ok();
    }

    /* ---- Excel ---- */

    @GetMapping("/template")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic')")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] data = alumniServiceImpl.generateTemplate();
        String filename = URLEncoder.encode("校友导入模板.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic')")
    public R<AlumniService.ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return R.fail(400, "文件为空");
        String name = file.getOriginalFilename();
        if (name == null || (!name.endsWith(".xlsx") && !name.endsWith(".xls"))) {
            return R.fail(400, "仅支持 .xlsx / .xls 格式");
        }
        return R.ok(alumniService.importExcel(file));
    }
}
