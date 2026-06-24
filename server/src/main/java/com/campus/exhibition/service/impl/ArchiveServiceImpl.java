package com.campus.exhibition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.common.BizException;
import com.campus.exhibition.common.DataScopeHelper;
import com.campus.exhibition.common.ErrorCode;
import com.campus.exhibition.common.UserContext;
import com.campus.exhibition.dto.ArchiveQuery;
import com.campus.exhibition.dto.ArchiveSaveRequest;
import com.campus.exhibition.entity.*;
import com.campus.exhibition.enums.ArchiveStatus;
import com.campus.exhibition.mapper.*;
import com.campus.exhibition.service.ArchiveService;
import com.campus.exhibition.service.FileStorageService;
import com.campus.exhibition.vo.ArchiveVO;
import com.campus.exhibition.vo.MediaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArchiveServiceImpl implements ArchiveService {

    private final ArchiveMapper archiveMapper;
    private final ArchiveMediaMapper mediaMapper;
    private final FileStorageService fileStorage;
    private final AlumniMapper alumniMapper;
    private final SysCollegeMapper collegeMapper;
    private final ArchiveCategoryMapper categoryMapper;

    @Override
    public Page<ArchiveVO> page(ArchiveQuery query, long pageNum, long pageSize) {
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            wrapper.eq(query.getAlumniId() != null, Archive::getAlumniId, query.getAlumniId());
            wrapper.eq(query.getCategoryId() != null, Archive::getCategoryId, query.getCategoryId());
            wrapper.eq(StringUtils.hasText(query.getStatus()), Archive::getStatus, query.getStatus());
            wrapper.like(StringUtils.hasText(query.getTitle()), Archive::getTitle, query.getTitle());
            wrapper.eq(query.getCollegeId() != null, Archive::getCollegeId, query.getCollegeId());
            wrapper.eq(query.getIsRecommend() != null, Archive::getIsRecommend, query.getIsRecommend());
        }
        DataScopeHelper.applyCollegeScope(wrapper, Archive::getCollegeId);
        wrapper.orderByDesc(Archive::getCreateTime);

        Page<Archive> page = archiveMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        Page<ArchiveVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(toVOList(page.getRecords()));
        return voPage;
    }

    @Override
    public ArchiveVO getById(Long id) {
        Archive entity = archiveMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "档案不存在");
        return toVO(entity);
    }

    @Override
    @Transactional
    public ArchiveVO create(ArchiveSaveRequest request) {
        if (alumniMapper.selectById(request.getAlumniId()) == null)
            throw new BizException(400, "校友不存在");
        if (categoryMapper.selectById(request.getCategoryId()) == null)
            throw new BizException(400, "分类不存在");

        Archive entity = new Archive();
        BeanUtils.copyProperties(request, entity);
        entity.setStatus(ArchiveStatus.DRAFT.getCode());
        entity.setIsTop(0);
        entity.setIsRecommend(0);
        entity.setDisplaySort(0);
        entity.setSubmitUser(UserContext.userId());
        entity.setSubmitTime(LocalDateTime.now());
        archiveMapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public ArchiveVO update(Long id, ArchiveSaveRequest request) {
        Archive entity = archiveMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "档案不存在");
        if (!ArchiveStatus.DRAFT.getCode().equals(entity.getStatus())
                && !ArchiveStatus.REJECTED.getCode().equals(entity.getStatus())) {
            throw new BizException(400, "仅草稿或已驳回状态可编辑");
        }

        BeanUtils.copyProperties(request, entity, "status", "isTop", "isRecommend",
                "displaySort", "submitUser", "submitTime", "publishTime");
        entity.setStatus(ArchiveStatus.DRAFT.getCode()); // 编辑后回到草稿
        archiveMapper.updateById(entity);

        // 媒体排序
        if (request.getMediaIds() != null && !request.getMediaIds().isEmpty()) {
            for (int i = 0; i < request.getMediaIds().size(); i++) {
                ArchiveMedia media = mediaMapper.selectById(request.getMediaIds().get(i));
                if (media != null && media.getArchiveId().equals(id)) {
                    media.setSort(i);
                    mediaMapper.updateById(media);
                }
            }
        }
        return toVO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Archive entity = archiveMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "档案不存在");

        // 删除关联媒体文件
        List<ArchiveMedia> mediaList = mediaMapper.selectList(
                new LambdaQueryWrapper<ArchiveMedia>().eq(ArchiveMedia::getArchiveId, id));
        for (ArchiveMedia media : mediaList) {
            fileStorage.delete(media.getUrl());
            if (media.getThumbnail() != null) fileStorage.delete(media.getThumbnail());
        }
        mediaMapper.delete(new LambdaQueryWrapper<ArchiveMedia>().eq(ArchiveMedia::getArchiveId, id));
        archiveMapper.deleteById(id);
    }

    /* ---- 媒体管理 ---- */

    @Override
    @Transactional
    public ArchiveVO addMedia(Long archiveId, MultipartFile file) {
        Archive entity = archiveMapper.selectById(archiveId);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "档案不存在");

        FileStorageService.UploadResult result = fileStorage.upload(file);

        // 获取当前最大 sort
        Long count = mediaMapper.selectCount(
                new LambdaQueryWrapper<ArchiveMedia>().eq(ArchiveMedia::getArchiveId, archiveId));

        ArchiveMedia media = new ArchiveMedia();
        media.setArchiveId(archiveId);
        media.setType(result.type());
        media.setUrl(result.url());
        media.setThumbnail(result.thumbnail());
        media.setFileName(result.fileName());
        media.setFileSize(result.fileSize());
        media.setDuration(result.duration());
        media.setSort(count.intValue());
        mediaMapper.insert(media);

        return toVO(entity);
    }

    @Override
    @Transactional
    public void removeMedia(Long archiveId, Long mediaId) {
        ArchiveMedia media = mediaMapper.selectById(mediaId);
        if (media == null || !media.getArchiveId().equals(archiveId))
            throw new BizException(ErrorCode.NOT_FOUND, "媒体不存在");
        fileStorage.delete(media.getUrl());
        if (media.getThumbnail() != null) fileStorage.delete(media.getThumbnail());
        mediaMapper.deleteById(mediaId);
    }

    @Override
    @Transactional
    public void sortMedia(Long archiveId, List<Long> mediaIds) {
        for (int i = 0; i < mediaIds.size(); i++) {
            ArchiveMedia media = mediaMapper.selectById(mediaIds.get(i));
            if (media != null && media.getArchiveId().equals(archiveId)) {
                media.setSort(i);
                mediaMapper.updateById(media);
            }
        }
    }

    /* ---- VO 转换 ---- */

    private ArchiveVO toVO(Archive entity) {
        ArchiveVO vo = new ArchiveVO();
        BeanUtils.copyProperties(entity, vo);

        Alumni alumni = alumniMapper.selectById(entity.getAlumniId());
        if (alumni != null) vo.setAlumniName(alumni.getName());

        SysCollege college = collegeMapper.selectById(entity.getCollegeId());
        if (college != null) vo.setCollegeName(college.getName());

        ArchiveCategory cat = categoryMapper.selectById(entity.getCategoryId());
        if (cat != null) vo.setCategoryName(cat.getName());

        // 媒体列表
        List<ArchiveMedia> mediaList = mediaMapper.selectList(
                new LambdaQueryWrapper<ArchiveMedia>()
                        .eq(ArchiveMedia::getArchiveId, entity.getId())
                        .orderByAsc(ArchiveMedia::getSort));
        vo.setMediaList(mediaList.stream().map(m -> {
            MediaVO mv = new MediaVO();
            BeanUtils.copyProperties(m, mv);
            return mv;
        }).collect(Collectors.toList()));

        return vo;
    }

    private List<ArchiveVO> toVOList(List<Archive> list) {
        if (list.isEmpty()) return List.of();
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }
}
