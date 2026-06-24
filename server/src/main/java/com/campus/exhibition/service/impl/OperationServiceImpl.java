package com.campus.exhibition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.exhibition.common.BizException;
import com.campus.exhibition.common.ErrorCode;
import com.campus.exhibition.dto.CarouselItemRequest;
import com.campus.exhibition.dto.CarouselSaveRequest;
import com.campus.exhibition.entity.*;
import com.campus.exhibition.enums.ArchiveStatus;
import com.campus.exhibition.mapper.*;
import com.campus.exhibition.service.OperationService;
import com.campus.exhibition.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OperationServiceImpl implements OperationService {

    private final ArchiveMapper archiveMapper;
    private final AlumniMapper alumniMapper;
    private final SysCollegeMapper collegeMapper;
    private final ArchiveCategoryMapper categoryMapper;
    private final ArchiveMediaMapper mediaMapper;
    private final ScreenCarouselMapper carouselMapper;
    private final ScreenCarouselItemMapper carouselItemMapper;

    /* ========== 上架/下架 ========== */

    @Override
    @Transactional
    public ArchiveVO publish(Long archiveId) {
        Archive archive = getArchive(archiveId);
        if (!ArchiveStatus.APPROVED.getCode().equals(archive.getStatus())
                && !ArchiveStatus.UNPUBLISHED.getCode().equals(archive.getStatus())) {
            throw new BizException(400, "仅已入库或已下架档案可上架");
        }
        archive.setStatus(ArchiveStatus.PUBLISHED.getCode());
        archive.setPublishTime(LocalDateTime.now());
        archiveMapper.updateById(archive);
        return toArchiveVO(archive);
    }

    @Override
    @Transactional
    public ArchiveVO unpublish(Long archiveId) {
        Archive archive = getArchive(archiveId);
        if (!ArchiveStatus.PUBLISHED.getCode().equals(archive.getStatus())) {
            throw new BizException(400, "仅已上架档案可下架");
        }
        archive.setStatus(ArchiveStatus.UNPUBLISHED.getCode());
        archiveMapper.updateById(archive);
        return toArchiveVO(archive);
    }

    /* ========== 运营属性 ========== */

    @Override
    @Transactional
    public ArchiveVO setTop(Long archiveId, int isTop) {
        Archive archive = getArchive(archiveId);
        archive.setIsTop(isTop);
        archiveMapper.updateById(archive);
        return toArchiveVO(archive);
    }

    @Override
    @Transactional
    public ArchiveVO setRecommend(Long archiveId, int isRecommend) {
        Archive archive = getArchive(archiveId);
        archive.setIsRecommend(isRecommend);
        archiveMapper.updateById(archive);
        return toArchiveVO(archive);
    }

    @Override
    @Transactional
    public ArchiveVO setDisplaySort(Long archiveId, int sort) {
        Archive archive = getArchive(archiveId);
        archive.setDisplaySort(sort);
        archiveMapper.updateById(archive);
        return toArchiveVO(archive);
    }

    /* ========== 轮播方案 CRUD ========== */

    @Override
    @Transactional
    public CarouselVO createCarousel(CarouselSaveRequest request) {
        // 若设为默认，先取消其他默认方案
        if (request.getIsDefault() != null && request.getIsDefault() == 1) {
            clearDefaultCarousel();
        }

        ScreenCarousel entity = new ScreenCarousel();
        BeanUtils.copyProperties(request, entity);
        if (entity.getIntervalSec() == null) entity.setIntervalSec(8);
        if (entity.getEffect() == null) entity.setEffect("fade");
        if (entity.getOrderType() == null) entity.setOrderType("sort");
        if (entity.getIsDefault() == null) entity.setIsDefault(0);
        entity.setStatus(1);
        carouselMapper.insert(entity);
        return toCarouselVO(entity);
    }

    @Override
    @Transactional
    public CarouselVO updateCarousel(Long id, CarouselSaveRequest request) {
        ScreenCarousel entity = carouselMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "轮播方案不存在");

        if (request.getIsDefault() != null && request.getIsDefault() == 1
                && entity.getIsDefault() != 1) {
            clearDefaultCarousel();
        }

        BeanUtils.copyProperties(request, entity);
        carouselMapper.updateById(entity);
        return toCarouselVO(entity);
    }

    @Override
    @Transactional
    public void deleteCarousel(Long id) {
        if (carouselMapper.selectById(id) == null)
            throw new BizException(ErrorCode.NOT_FOUND, "轮播方案不存在");
        // 级联删除轮播项
        carouselItemMapper.delete(
                new LambdaQueryWrapper<ScreenCarouselItem>().eq(ScreenCarouselItem::getCarouselId, id));
        carouselMapper.deleteById(id);
    }

    @Override
    public CarouselVO getCarousel(Long id) {
        ScreenCarousel entity = carouselMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "轮播方案不存在");
        return toCarouselVO(entity);
    }

    @Override
    public List<CarouselVO> listCarousels() {
        List<ScreenCarousel> list = carouselMapper.selectList(
                new LambdaQueryWrapper<ScreenCarousel>()
                        .eq(ScreenCarousel::getStatus, 1)
                        .orderByDesc(ScreenCarousel::getIsDefault)
                        .orderByAsc(ScreenCarousel::getCreateTime));
        return list.stream().map(this::toCarouselVO).collect(Collectors.toList());
    }

    /* ========== 轮播池内容 ========== */

    @Override
    @Transactional
    public CarouselVO addCarouselItem(Long carouselId, CarouselItemRequest request) {
        if (carouselMapper.selectById(carouselId) == null)
            throw new BizException(ErrorCode.NOT_FOUND, "轮播方案不存在");

        ScreenCarouselItem item = new ScreenCarouselItem();
        item.setCarouselId(carouselId);
        item.setArchiveId(request.getArchiveId());
        item.setAlumniId(request.getAlumniId());
        item.setSort(request.getSort() != null ? request.getSort() : 999);
        carouselItemMapper.insert(item);

        return toCarouselVO(carouselMapper.selectById(carouselId));
    }

    @Override
    @Transactional
    public void removeCarouselItem(Long carouselId, Long itemId) {
        ScreenCarouselItem item = carouselItemMapper.selectById(itemId);
        if (item == null || !item.getCarouselId().equals(carouselId))
            throw new BizException(ErrorCode.NOT_FOUND, "轮播项不存在");
        carouselItemMapper.deleteById(itemId);
    }

    @Override
    @Transactional
    public void sortCarouselItems(Long carouselId, List<Long> itemIds) {
        for (int i = 0; i < itemIds.size(); i++) {
            ScreenCarouselItem item = carouselItemMapper.selectById(itemIds.get(i));
            if (item != null && item.getCarouselId().equals(carouselId)) {
                item.setSort(i);
                carouselItemMapper.updateById(item);
            }
        }
    }

    /* ========== 内部方法 ========== */

    private void clearDefaultCarousel() {
        List<ScreenCarousel> defaults = carouselMapper.selectList(
                new LambdaQueryWrapper<ScreenCarousel>().eq(ScreenCarousel::getIsDefault, 1));
        for (ScreenCarousel c : defaults) {
            c.setIsDefault(0);
            carouselMapper.updateById(c);
        }
    }

    private Archive getArchive(Long id) {
        Archive archive = archiveMapper.selectById(id);
        if (archive == null) throw new BizException(ErrorCode.ARCHIVE_NOT_FOUND);
        return archive;
    }

    private CarouselVO toCarouselVO(ScreenCarousel entity) {
        CarouselVO vo = new CarouselVO();
        BeanUtils.copyProperties(entity, vo);

        List<ScreenCarouselItem> items = carouselItemMapper.selectList(
                new LambdaQueryWrapper<ScreenCarouselItem>()
                        .eq(ScreenCarouselItem::getCarouselId, entity.getId())
                        .orderByAsc(ScreenCarouselItem::getSort));

        vo.setItems(items.stream().map(item -> {
            CarouselItemVO iv = new CarouselItemVO();
            BeanUtils.copyProperties(item, iv);
            if (item.getArchiveId() != null) {
                Archive archive = archiveMapper.selectById(item.getArchiveId());
                if (archive != null) iv.setArchiveTitle(archive.getTitle());
            }
            if (item.getAlumniId() != null) {
                Alumni alumni = alumniMapper.selectById(item.getAlumniId());
                if (alumni != null) iv.setAlumniName(alumni.getName());
            }
            return iv;
        }).collect(Collectors.toList()));

        return vo;
    }

    private ArchiveVO toArchiveVO(Archive entity) {
        ArchiveVO vo = new ArchiveVO();
        BeanUtils.copyProperties(entity, vo);
        Alumni alumni = alumniMapper.selectById(entity.getAlumniId());
        if (alumni != null) vo.setAlumniName(alumni.getName());
        SysCollege college = collegeMapper.selectById(entity.getCollegeId());
        if (college != null) vo.setCollegeName(college.getName());
        ArchiveCategory cat = categoryMapper.selectById(entity.getCategoryId());
        if (cat != null) vo.setCategoryName(cat.getName());

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
}
