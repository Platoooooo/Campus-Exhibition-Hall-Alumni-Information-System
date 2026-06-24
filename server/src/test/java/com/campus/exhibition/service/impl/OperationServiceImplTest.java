package com.campus.exhibition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.exhibition.common.BizException;
import com.campus.exhibition.common.ErrorCode;
import com.campus.exhibition.dto.CarouselSaveRequest;
import com.campus.exhibition.entity.*;
import com.campus.exhibition.enums.ArchiveStatus;
import com.campus.exhibition.mapper.*;
import com.campus.exhibition.vo.ArchiveVO;
import com.campus.exhibition.vo.CarouselVO;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OperationServiceImpl 运营服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OperationService 运营服务")
class OperationServiceImplTest {

    @Mock private ArchiveMapper archiveMapper;
    @Mock private AlumniMapper alumniMapper;
    @Mock private SysCollegeMapper collegeMapper;
    @Mock private ArchiveCategoryMapper categoryMapper;
    @Mock private ArchiveMediaMapper mediaMapper;
    @Mock private ScreenCarouselMapper carouselMapper;
    @Mock private ScreenCarouselItemMapper carouselItemMapper;
    @Mock private Cache<String, Object> screenCache;
    @InjectMocks private OperationServiceImpl operationService;

    private Archive approvedArchive() {
        Archive a = new Archive();
        a.setId(1L);
        a.setTitle("已入库档案");
        a.setAlumniId(10L);
        a.setCollegeId(1L);
        a.setCategoryId(1L);
        a.setStatus(ArchiveStatus.APPROVED.getCode());
        return a;
    }

    private Archive publishedArchive() {
        Archive a = approvedArchive();
        a.setStatus(ArchiveStatus.PUBLISHED.getCode());
        return a;
    }

    // ── 上架/下架 ──

    @Nested
    @DisplayName("上架/下架")
    class PublishUnpublish {

        @Test
        @DisplayName("已入库档案可上架")
        void publish_fromApproved_success() {
            when(archiveMapper.selectById(1L)).thenReturn(approvedArchive());

            ArchiveVO result = operationService.publish(1L);

            verify(archiveMapper).updateById(argThat(a ->
                    ArchiveStatus.PUBLISHED.getCode().equals(a.getStatus())
                    && a.getPublishTime() != null));
            verify(screenCache).invalidate("carousel:default");
        }

        @Test
        @DisplayName("已下架档案可重新上架")
        void publish_fromUnpublished_success() {
            Archive unpublished = approvedArchive();
            unpublished.setStatus(ArchiveStatus.UNPUBLISHED.getCode());
            when(archiveMapper.selectById(1L)).thenReturn(unpublished);

            assertDoesNotThrow(() -> operationService.publish(1L));
        }

        @Test
        @DisplayName("草稿状态不可上架")
        void publish_fromDraft_throws() {
            Archive draft = approvedArchive();
            draft.setStatus(ArchiveStatus.DRAFT.getCode());
            when(archiveMapper.selectById(1L)).thenReturn(draft);

            BizException ex = assertThrows(BizException.class, () -> operationService.publish(1L));
            assertTrue(ex.getMessage().contains("上架"));
        }

        @Test
        @DisplayName("已上架档案可下架")
        void unpublish_fromPublished_success() {
            when(archiveMapper.selectById(1L)).thenReturn(publishedArchive());

            ArchiveVO result = operationService.unpublish(1L);

            verify(archiveMapper).updateById(argThat(a ->
                    ArchiveStatus.UNPUBLISHED.getCode().equals(a.getStatus())));
        }

        @Test
        @DisplayName("非已上架状态不可下架")
        void unpublish_fromApproved_throws() {
            when(archiveMapper.selectById(1L)).thenReturn(approvedArchive());

            assertThrows(BizException.class, () -> operationService.unpublish(1L));
        }
    }

    // ── 运营属性 ──

    @Nested
    @DisplayName("运营属性")
    class OperationAttributes {

        @Test
        @DisplayName("设置置顶")
        void setTop() {
            when(archiveMapper.selectById(1L)).thenReturn(publishedArchive());

            operationService.setTop(1L, 1);
            verify(archiveMapper).updateById(argThat(a -> a.getIsTop() == 1));
        }

        @Test
        @DisplayName("设置推荐")
        void setRecommend() {
            when(archiveMapper.selectById(1L)).thenReturn(publishedArchive());

            operationService.setRecommend(1L, 1);
            verify(archiveMapper).updateById(argThat(a -> a.getIsRecommend() == 1));
        }

        @Test
        @DisplayName("设置排序")
        void setDisplaySort() {
            when(archiveMapper.selectById(1L)).thenReturn(publishedArchive());

            operationService.setDisplaySort(1L, 100);
            verify(archiveMapper).updateById(argThat(a -> a.getDisplaySort() == 100));
        }
    }

    // ── 轮播方案 ──

    @Nested
    @DisplayName("轮播方案")
    class Carousel {

        @Test
        @DisplayName("创建默认轮播方案时清除其他默认")
        void createCarousel_default_clearsOldDefaults() {
            CarouselSaveRequest req = new CarouselSaveRequest();
            req.setName("新方案");
            req.setIsDefault(1);

            // 存在旧的默认方案
            ScreenCarousel oldDefault = new ScreenCarousel();
            oldDefault.setId(1L);
            oldDefault.setIsDefault(1);
            when(carouselMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(oldDefault));

            CarouselVO result = operationService.createCarousel(req);

            // 旧默认被清除
            verify(carouselMapper).updateById(argThat(c -> c.getIsDefault() == 0));
            verify(screenCache).invalidate("carousel:default");
        }

        @Test
        @DisplayName("创建非默认方案不触发清除")
        void createCarousel_notDefault_noConflict() {
            CarouselSaveRequest req = new CarouselSaveRequest();
            req.setName("普通方案");
            req.setIsDefault(0);

            CarouselVO result = operationService.createCarousel(req);

            verify(carouselMapper, never()).updateById(any());
            verify(screenCache).invalidate("carousel:default");
        }

        @Test
        @DisplayName("删除轮播方案级联删除轮播项")
        void deleteCarousel_cascadesItems() {
            when(carouselMapper.selectById(1L)).thenReturn(new ScreenCarousel());

            operationService.deleteCarousel(1L);

            verify(carouselItemMapper).delete(any(LambdaQueryWrapper.class));
            verify(carouselMapper).deleteById(1L);
        }

        @Test
        @DisplayName("删除不存在的方案抛出异常")
        void deleteCarousel_notFound_throws() {
            when(carouselMapper.selectById(999L)).thenReturn(null);

            assertThrows(BizException.class, () -> operationService.deleteCarousel(999L));
        }
    }
}
