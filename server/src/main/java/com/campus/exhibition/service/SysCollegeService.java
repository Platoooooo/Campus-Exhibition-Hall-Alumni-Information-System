package com.campus.exhibition.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.dto.CollegeQuery;
import com.campus.exhibition.dto.CollegeSaveRequest;
import com.campus.exhibition.vo.CollegeVO;

import java.util.List;

public interface SysCollegeService {

    Page<CollegeVO> page(CollegeQuery query, long pageNum, long pageSize);

    List<CollegeVO> listAll();

    CollegeVO getById(Long id);

    CollegeVO create(CollegeSaveRequest request);

    CollegeVO update(Long id, CollegeSaveRequest request);

    void toggleStatus(Long id);
}
