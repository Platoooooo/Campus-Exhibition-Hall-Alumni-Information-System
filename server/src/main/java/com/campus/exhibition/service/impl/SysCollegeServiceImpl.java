package com.campus.exhibition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.common.BizException;
import com.campus.exhibition.common.ErrorCode;
import com.campus.exhibition.dto.CollegeQuery;
import com.campus.exhibition.dto.CollegeSaveRequest;
import com.campus.exhibition.entity.SysCollege;
import com.campus.exhibition.mapper.SysCollegeMapper;
import com.campus.exhibition.service.SysCollegeService;
import com.campus.exhibition.vo.CollegeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysCollegeServiceImpl implements SysCollegeService {

    private final SysCollegeMapper collegeMapper;

    @Override
    public Page<CollegeVO> page(CollegeQuery query, long pageNum, long pageSize) {
        LambdaQueryWrapper<SysCollege> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            wrapper.like(StringUtils.hasText(query.getName()), SysCollege::getName, query.getName());
            wrapper.like(StringUtils.hasText(query.getCode()), SysCollege::getCode, query.getCode());
            wrapper.eq(query.getStatus() != null, SysCollege::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysCollege::getSort);
        Page<SysCollege> page = collegeMapper.selectPage(Page.of(pageNum, pageSize), wrapper);

        Page<CollegeVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(toVOList(page.getRecords()));
        return voPage;
    }

    @Override
    public List<CollegeVO> listAll() {
        List<SysCollege> list = collegeMapper.selectList(
                new LambdaQueryWrapper<SysCollege>()
                        .eq(SysCollege::getStatus, 1)
                        .orderByAsc(SysCollege::getSort)
        );
        return toVOList(list);
    }

    @Override
    public CollegeVO getById(Long id) {
        SysCollege entity = collegeMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "学院不存在");
        return toVO(entity);
    }

    @Override
    @Transactional
    public CollegeVO create(CollegeSaveRequest request) {
        // 编码唯一校验
        if (collegeMapper.selectCount(
                new LambdaQueryWrapper<SysCollege>().eq(SysCollege::getCode, request.getCode())) > 0) {
            throw new BizException(400, "学院编码已存在");
        }
        SysCollege entity = new SysCollege();
        BeanUtils.copyProperties(request, entity);
        if (entity.getStatus() == null) entity.setStatus(1);
        if (entity.getSort() == null) entity.setSort(0);
        collegeMapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public CollegeVO update(Long id, CollegeSaveRequest request) {
        SysCollege entity = collegeMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "学院不存在");

        // 编码唯一校验（排除自身）
        Long count = collegeMapper.selectCount(
                new LambdaQueryWrapper<SysCollege>()
                        .eq(SysCollege::getCode, request.getCode())
                        .ne(SysCollege::getId, id));
        if (count > 0) throw new BizException(400, "学院编码已存在");

        BeanUtils.copyProperties(request, entity);
        collegeMapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        SysCollege entity = collegeMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "学院不存在");
        entity.setStatus(entity.getStatus() == 1 ? 0 : 1);
        collegeMapper.updateById(entity);
    }

    private CollegeVO toVO(SysCollege entity) {
        CollegeVO vo = new CollegeVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private List<CollegeVO> toVOList(List<SysCollege> list) {
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }
}
