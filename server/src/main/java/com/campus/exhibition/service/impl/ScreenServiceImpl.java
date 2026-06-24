package com.campus.exhibition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.dto.ScreenSearchQuery;
import com.campus.exhibition.dto.WallQuery;
import com.campus.exhibition.entity.*;
import com.campus.exhibition.enums.ArchiveStatus;
import com.campus.exhibition.mapper.*;
import com.campus.exhibition.service.ScreenService;
import com.campus.exhibition.vo.*;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenServiceImpl implements ScreenService {

    private final ScreenCarouselMapper carouselMapper;
    private final ScreenCarouselItemMapper carouselItemMapper;
    private final ArchiveMapper archiveMapper;
    private final ArchiveMediaMapper mediaMapper;
    private final AlumniMapper alumniMapper;
    private final SysCollegeMapper collegeMapper;
    private final ArchiveCategoryMapper categoryMapper;
    private final Cache<String, Object> screenCache;

    /* ========== 轮播 ========== */

    @Override
    public ScreenCarouselVO getCarousel() {
        return getCachedOrLoad("carousel:default", () -> {
            ScreenCarousel carousel = carouselMapper.selectOne(
                    new LambdaQueryWrapper<ScreenCarousel>()
                            .eq(ScreenCarousel::getIsDefault, 1)
                            .eq(ScreenCarousel::getStatus, 1));
            if (carousel == null) {
                // fallback: return first active
                List<ScreenCarousel> list = carouselMapper.selectList(
                        new LambdaQueryWrapper<ScreenCarousel>()
                                .eq(ScreenCarousel::getStatus, 1)
                                .orderByDesc(ScreenCarousel::getCreateTime)
                                .last("LIMIT 1"));
                if (list.isEmpty()) return new ScreenCarouselVO();
                carousel = list.get(0);
            }

            ScreenCarouselVO vo = new ScreenCarouselVO();
            vo.setId(carousel.getId());
            vo.setName(carousel.getName());
            vo.setIntervalSec(carousel.getIntervalSec());
            vo.setEffect(carousel.getEffect());
            vo.setOrderType(carousel.getOrderType());

            List<ScreenCarouselItem> items = carouselItemMapper.selectList(
                    new LambdaQueryWrapper<ScreenCarouselItem>()
                            .eq(ScreenCarouselItem::getCarouselId, carousel.getId())
                            .orderByAsc(ScreenCarouselItem::getSort));

            vo.setItems(items.stream().map(this::buildCarouselItem).filter(Objects::nonNull).collect(Collectors.toList()));
            return vo;
        });
    }

    private ScreenCarouselVO.ScreenCarouselItem buildCarouselItem(ScreenCarouselItem item) {
        ScreenCarouselVO.ScreenCarouselItem si = new ScreenCarouselVO.ScreenCarouselItem();
        si.setId(item.getId());
        si.setSort(item.getSort());

        if (item.getArchiveId() != null) {
            Archive archive = archiveMapper.selectById(item.getArchiveId());
            if (archive == null || !ArchiveStatus.PUBLISHED.getCode().equals(archive.getStatus()))
                return null; // 静默跳过未发布
            si.setArchiveId(archive.getId());
            si.setTitle(archive.getTitle());
            si.setContent(truncate(archive.getContent(), 200));
            si.setEventDate(archive.getEventDate() != null ? archive.getEventDate().toString() : null);
            si.setAlumniId(archive.getAlumniId());

            Alumni alumni = alumniMapper.selectById(archive.getAlumniId());
            if (alumni != null) {
                si.setAlumniName(alumni.getName());
                si.setAlumniAvatar(alumni.getAvatar());
                si.setGradYear(alumni.getGradYear() != null ? String.valueOf(alumni.getGradYear()) : null);
                SysCollege college = collegeMapper.selectById(alumni.getCollegeId());
                if (college != null) si.setCollegeName(college.getName());
            }
            si.setMediaList(buildMediaList(archive.getId()));
        }

        if (item.getAlumniId() != null) {
            Alumni alumni = alumniMapper.selectById(item.getAlumniId());
            if (alumni == null) return null;
            si.setAlumniId(alumni.getId());
            si.setAlumniName(alumni.getName());
            si.setAlumniAvatar(alumni.getAvatar());
            si.setGradYear(alumni.getGradYear() != null ? String.valueOf(alumni.getGradYear()) : null);
            SysCollege college = collegeMapper.selectById(alumni.getCollegeId());
            if (college != null) si.setCollegeName(college.getName());
            // 取该校友第一条已发布档案作封面
            List<Archive> archives = archiveMapper.selectList(
                    new LambdaQueryWrapper<Archive>()
                            .eq(Archive::getAlumniId, alumni.getId())
                            .eq(Archive::getStatus, ArchiveStatus.PUBLISHED.getCode())
                            .orderByDesc(Archive::getCreateTime)
                            .last("LIMIT 1"));
            if (!archives.isEmpty()) {
                si.setTitle(archives.get(0).getTitle());
                si.setMediaList(buildMediaList(archives.get(0).getId()));
            }
        }

        return si;
    }

    /* ========== 校友墙 ========== */

    @Override
    public Page<WallItemVO> getWall(WallQuery query, int pageNum, int pageSize) {
        // 找出所有已发布档案关联的校友（去重）
        LambdaQueryWrapper<Archive> archiveQuery = new LambdaQueryWrapper<Archive>()
                .eq(Archive::getStatus, ArchiveStatus.PUBLISHED.getCode())
                .eq(query.getCategoryId() != null, Archive::getCategoryId, query.getCategoryId())
                .select(Archive::getAlumniId);
        // 通过 last 追加 GROUP BY 去重
        archiveQuery.last("GROUP BY alumni_id");
        List<Archive> published = archiveMapper.selectList(archiveQuery);
        Set<Long> alumniIds = published.stream().map(Archive::getAlumniId).collect(Collectors.toSet());
        if (alumniIds.isEmpty()) return new Page<>(pageNum, pageSize, 0);

        LambdaQueryWrapper<Alumni> wrapper = new LambdaQueryWrapper<Alumni>()
                .in(Alumni::getId, alumniIds)
                .eq(Alumni::getStatus, 1)
                .eq(query.getGradYear() != null, Alumni::getGradYear, query.getGradYear())
                .orderByDesc(Alumni::getGradYear);

        Page<Alumni> page = alumniMapper.selectPage(Page.of(pageNum, pageSize), wrapper);

        List<WallItemVO> vos = page.getRecords().stream().map(a -> {
            WallItemVO vo = new WallItemVO();
            vo.setAlumniId(a.getId());
            vo.setName(a.getName());
            vo.setAvatar(a.getAvatar());
            vo.setGradYear(a.getGradYear());
            SysCollege c = collegeMapper.selectById(a.getCollegeId());
            if (c != null) vo.setCollegeName(c.getName());

            // 代表标签：取该校友第一条已发布档案标题
            List<Archive> archives = archiveMapper.selectList(
                    new LambdaQueryWrapper<Archive>()
                            .eq(Archive::getAlumniId, a.getId())
                            .eq(Archive::getStatus, ArchiveStatus.PUBLISHED.getCode())
                            .orderByDesc(Archive::getCreateTime)
                            .last("LIMIT 1"));
            if (!archives.isEmpty()) {
                vo.setTag(archives.get(0).getTitle());
                // 代表缩略图
                List<ArchiveMedia> medias = mediaMapper.selectList(
                        new LambdaQueryWrapper<ArchiveMedia>()
                                .eq(ArchiveMedia::getArchiveId, archives.get(0).getId())
                                .orderByAsc(ArchiveMedia::getSort)
                                .last("LIMIT 1"));
                if (!medias.isEmpty()) vo.setThumbnail(medias.get(0).getThumbnail());
            }
            return vo;
        }).collect(Collectors.toList());

        Page<WallItemVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    /* ========== 成长轨迹 ========== */

    @Override
    public AlumniTimelineVO getAlumniTimeline(Long alumniId) {
        Alumni alumni = alumniMapper.selectById(alumniId);
        if (alumni == null) return null;

        AlumniTimelineVO vo = new AlumniTimelineVO();
        vo.setAlumniId(alumni.getId());
        vo.setName(alumni.getName());
        vo.setAvatar(alumni.getAvatar());
        vo.setSummary(alumni.getSummary());
        vo.setGradYear(alumni.getGradYear());
        SysCollege c = collegeMapper.selectById(alumni.getCollegeId());
        if (c != null) vo.setCollegeName(c.getName());

        // only published archives, ordered by event_date
        List<Archive> archives = archiveMapper.selectList(
                new LambdaQueryWrapper<Archive>()
                        .eq(Archive::getAlumniId, alumniId)
                        .eq(Archive::getStatus, ArchiveStatus.PUBLISHED.getCode())
                        .orderByAsc(Archive::getEventDate));

        List<AlumniTimelineVO.TimelineNode> timeline = archives.stream().map(a -> {
            AlumniTimelineVO.TimelineNode node = new AlumniTimelineVO.TimelineNode();
            node.setArchiveId(a.getId());
            node.setTitle(a.getTitle());
            node.setContent(a.getContent());
            node.setEventDate(a.getEventDate());
            ArchiveCategory cat = categoryMapper.selectById(a.getCategoryId());
            if (cat != null) node.setCategoryName(cat.getName());
            node.setMediaList(buildMediaList(a.getId()));
            return node;
        }).collect(Collectors.toList());

        vo.setTimeline(timeline);
        return vo;
    }

    /* ========== 搜索 ========== */

    @Override
    public Page<ScreenSearchResultVO> search(ScreenSearchQuery query, int pageNum, int pageSize) {
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<Archive>()
                .eq(Archive::getStatus, ArchiveStatus.PUBLISHED.getCode());

        if (query != null) {
            if (StringUtils.hasText(query.getKeyword())) {
                wrapper.and(w -> w
                        .like(Archive::getTitle, query.getKeyword())
                        .or().like(Archive::getContent, query.getKeyword()));
            }
            wrapper.eq(query.getCollegeId() != null, Archive::getCollegeId, query.getCollegeId());
            wrapper.eq(query.getCategoryId() != null, Archive::getCategoryId, query.getCategoryId());
        }

        // 按校友毕业年份过滤（join 思路：先查符合年份的校友ID，再过滤）
        if (query != null && query.getGradYear() != null) {
            List<Alumni> alumniList = alumniMapper.selectList(
                    new LambdaQueryWrapper<Alumni>().eq(Alumni::getGradYear, query.getGradYear()));
            Set<Long> ids = alumniList.stream().map(Alumni::getId).collect(Collectors.toSet());
            if (ids.isEmpty()) return new Page<>(pageNum, pageSize, 0);
            wrapper.in(Archive::getAlumniId, ids);
        }

        wrapper.orderByDesc(Archive::getDisplaySort).orderByDesc(Archive::getCreateTime);
        Page<Archive> page = archiveMapper.selectPage(Page.of(pageNum, pageSize), wrapper);

        List<ScreenSearchResultVO> vos = page.getRecords().stream().map(a -> {
            ScreenSearchResultVO r = new ScreenSearchResultVO();
            r.setArchiveId(a.getId());
            r.setTitle(a.getTitle());
            r.setContent(truncate(a.getContent(), 200));
            r.setEventDate(a.getEventDate());
            r.setAlumniId(a.getAlumniId());

            Alumni alumni = alumniMapper.selectById(a.getAlumniId());
            if (alumni != null) {
                r.setAlumniName(alumni.getName());
                r.setAlumniAvatar(alumni.getAvatar());
            }
            SysCollege college = collegeMapper.selectById(a.getCollegeId());
            if (college != null) r.setCollegeName(college.getName());
            ArchiveCategory cat = categoryMapper.selectById(a.getCategoryId());
            if (cat != null) r.setCategoryName(cat.getName());
            r.setMediaList(buildMediaList(a.getId()));
            return r;
        }).collect(Collectors.toList());

        Page<ScreenSearchResultVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(vos);
        return voPage;
    }

    /* ========== 内部方法 ========== */

    private List<ScreenCarouselVO.MediaItem> buildMediaList(Long archiveId) {
        List<ArchiveMedia> medias = mediaMapper.selectList(
                new LambdaQueryWrapper<ArchiveMedia>()
                        .eq(ArchiveMedia::getArchiveId, archiveId)
                        .orderByAsc(ArchiveMedia::getSort));
        return medias.stream().map(m -> {
            ScreenCarouselVO.MediaItem mi = new ScreenCarouselVO.MediaItem();
            mi.setId(m.getId());
            mi.setType(m.getType());
            mi.setUrl(m.getUrl());
            mi.setThumbnail(m.getThumbnail());
            return mi;
        }).collect(Collectors.toList());
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    @SuppressWarnings("unchecked")
    private <T> T getCachedOrLoad(String key, java.util.function.Supplier<T> loader) {
        T cached = (T) screenCache.getIfPresent(key);
        if (cached != null) return cached;
        T value = loader.get();
        if (value != null) screenCache.put(key, value);
        return value;
    }
}
