package com.campus.exhibition.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.dto.AlumniQuery;
import com.campus.exhibition.dto.AlumniSaveRequest;
import com.campus.exhibition.vo.AlumniVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AlumniService {

    Page<AlumniVO> page(AlumniQuery query, long pageNum, long pageSize);

    AlumniVO getById(Long id);

    AlumniVO create(AlumniSaveRequest request);

    AlumniVO update(Long id, AlumniSaveRequest request);

    void delete(Long id);

    /** 批量导入（Excel），返回错误行信息 */
    ImportResult importExcel(MultipartFile file);

    record ImportResult(int total, int success, int fail, List<String> errors) {}
}
