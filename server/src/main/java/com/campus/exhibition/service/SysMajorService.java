package com.campus.exhibition.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.dto.MajorQuery;
import com.campus.exhibition.dto.MajorSaveRequest;
import com.campus.exhibition.vo.MajorVO;

import java.util.List;

public interface SysMajorService {

    Page<MajorVO> page(MajorQuery query, long pageNum, long pageSize);

    /** 按学院级联查询 */
    List<MajorVO> listByCollege(Long collegeId);

    MajorVO getById(Long id);

    MajorVO create(MajorSaveRequest request);

    MajorVO update(Long id, MajorSaveRequest request);

    void toggleStatus(Long id);
}
