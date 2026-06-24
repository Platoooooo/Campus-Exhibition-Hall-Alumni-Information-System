package com.campus.exhibition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.common.BizException;
import com.campus.exhibition.common.ErrorCode;
import com.campus.exhibition.dto.MajorQuery;
import com.campus.exhibition.dto.MajorSaveRequest;
import com.campus.exhibition.entity.SysCollege;
import com.campus.exhibition.entity.SysMajor;
import com.campus.exhibition.mapper.SysCollegeMapper;
import com.campus.exhibition.mapper.SysMajorMapper;
import com.campus.exhibition.service.SysMajorService;
import com.campus.exhibition.vo.MajorVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMajorServiceImpl implements SysMajorService {

    private final SysMajorMapper majorMapper;
    private final SysCollegeMapper collegeMapper;

    @Override
    public Page<MajorVO> page(MajorQuery query, long pageNum, long pageSize) {
        LambdaQueryWrapper<SysMajor> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            wrapper.eq(query.getCollegeId() != null, SysMajor::getCollegeId, query.getCollegeId());
            wrapper.like(StringUtils.hasText(query.getName()), SysMajor::getName, query.getName());
            wrapper.like(StringUtils.hasText(query.getCode()), SysMajor::getCode, query.getCode());
            wrapper.eq(query.getStatus() != null, SysMajor::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysMajor::getSort);
        Page<SysMajor> page = majorMapper.selectPage(Page.of(pageNum, pageSize), wrapper);

        List<MajorVO> vos = toVOList(page.getRecords());
        Page<MajorVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    @Override
    public List<MajorVO> listByCollege(Long collegeId) {
        List<SysMajor> list = majorMapper.selectList(
                new LambdaQueryWrapper<SysMajor>()
                        .eq(collegeId != null, SysMajor::getCollegeId, collegeId)
                        .eq(SysMajor::getStatus, 1)
                        .orderByAsc(SysMajor::getSort)
        );
        return toVOList(list);
    }

    @Override
    public MajorVO getById(Long id) {
        SysMajor entity = majorMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "专业不存在");
        return toVO(entity);
    }

    @Override
    @Transactional
    public MajorVO create(MajorSaveRequest request) {
        // 校验学院存在
        if (collegeMapper.selectById(request.getCollegeId()) == null) {
            throw new BizException(400, "所属学院不存在");
        }
        SysMajor entity = new SysMajor();
        BeanUtils.copyProperties(request, entity);
        if (entity.getStatus() == null) entity.setStatus(1);
        if (entity.getSort() == null) entity.setSort(0);
        majorMapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public MajorVO update(Long id, MajorSaveRequest request) {
        SysMajor entity = majorMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "专业不存在");
        if (collegeMapper.selectById(request.getCollegeId()) == null) {
            throw new BizException(400, "所属学院不存在");
        }
        BeanUtils.copyProperties(request, entity);
        majorMapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id) {
        SysMajor entity = majorMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "专业不存在");
        entity.setStatus(entity.getStatus() == 1 ? 0 : 1);
        majorMapper.updateById(entity);
    }

    private MajorVO toVO(SysMajor entity) {
        MajorVO vo = new MajorVO();
        BeanUtils.copyProperties(entity, vo);
        // 填充学院名称
        SysCollege college = collegeMapper.selectById(entity.getCollegeId());
        if (college != null) vo.setCollegeName(college.getName());
        return vo;
    }

    private List<MajorVO> toVOList(List<SysMajor> list) {
        if (list.isEmpty()) return List.of();
        // 批量查学院名称
        List<Long> collegeIds = list.stream().map(SysMajor::getCollegeId).distinct().toList();
        Map<Long, String> collegeMap = collegeMapper.selectBatchIds(collegeIds).stream()
                .collect(Collectors.toMap(SysCollege::getId, SysCollege::getName));

        return list.stream().map(e -> {
            MajorVO vo = new MajorVO();
            BeanUtils.copyProperties(e, vo);
            vo.setCollegeName(collegeMap.getOrDefault(e.getCollegeId(), ""));
            return vo;
        }).collect(Collectors.toList());
    }
}
