package com.campus.exhibition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.exhibition.common.BizException;
import com.campus.exhibition.common.ErrorCode;
import com.campus.exhibition.dto.ArchiveSaveRequest;
import com.campus.exhibition.entity.*;
import com.campus.exhibition.enums.ArchiveStatus;
import com.campus.exhibition.mapper.*;
import com.campus.exhibition.service.FileStorageService;
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
 * ArchiveServiceImpl 档案服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ArchiveService 档案服务")
class ArchiveServiceImplTest {

    @Mock private ArchiveMapper archiveMapper;
    @Mock private ArchiveMediaMapper mediaMapper;
    @Mock private FileStorageService fileStorage;
    @Mock private AlumniMapper alumniMapper;
    @Mock private SysCollegeMapper collegeMapper;
    @Mock private ArchiveCategoryMapper categoryMapper;
    @InjectMocks private ArchiveServiceImpl archiveService;

    private Archive draftArchive() {
        Archive a = new Archive();
        a.setId(1L);
        a.setTitle("测试档案");
        a.setAlumniId(10L);
        a.setCollegeId(1L);
        a.setCategoryId(1L);
        a.setStatus(ArchiveStatus.DRAFT.getCode());
        return a;
    }

    private ArchiveSaveRequest createRequest(Long alumniId, Long categoryId) {
        ArchiveSaveRequest req = new ArchiveSaveRequest();
        req.setAlumniId(alumniId);
        req.setCategoryId(categoryId);
        req.setTitle("新档案");
        req.setCollegeId(1L);
        return req;
    }

    // ── 创建 ──

    @Nested
    @DisplayName("创建档案")
    class Create {

        @Test
        @DisplayName("创建成功，状态为草稿")
        void create_success_statusIsDraft() {
            when(alumniMapper.selectById(10L)).thenReturn(new Alumni());
            when(categoryMapper.selectById(1L)).thenReturn(new ArchiveCategory());

            archiveService.create(createRequest(10L, 1L));

            verify(archiveMapper).insert(argThat(a ->
                    ArchiveStatus.DRAFT.getCode().equals(a.getStatus())
                    && a.getIsTop() == 0
                    && a.getIsRecommend() == 0));
        }

        @Test
        @DisplayName("校友不存在抛出异常")
        void alumniNotFound_throws() {
            when(alumniMapper.selectById(999L)).thenReturn(null);

            BizException ex = assertThrows(BizException.class,
                    () -> archiveService.create(createRequest(999L, 1L)));
            assertTrue(ex.getMessage().contains("校友"));
        }

        @Test
        @DisplayName("分类不存在抛出异常")
        void categoryNotFound_throws() {
            when(alumniMapper.selectById(10L)).thenReturn(new Alumni());
            when(categoryMapper.selectById(999L)).thenReturn(null);

            BizException ex = assertThrows(BizException.class,
                    () -> archiveService.create(createRequest(10L, 999L)));
            assertTrue(ex.getMessage().contains("分类"));
        }
    }

    // ── 更新 ──

    @Nested
    @DisplayName("更新档案")
    class Update {

        @Test
        @DisplayName("草稿状态可编辑")
        void draft_canEdit() {
            Archive draft = draftArchive();
            when(archiveMapper.selectById(1L)).thenReturn(draft);

            assertDoesNotThrow(() -> archiveService.update(1L, createRequest(10L, 1L)));
            verify(archiveMapper).updateById(any(Archive.class));
        }

        @Test
        @DisplayName("已驳回状态可编辑")
        void rejected_canEdit() {
            Archive rejected = draftArchive();
            rejected.setStatus(ArchiveStatus.REJECTED.getCode());
            when(archiveMapper.selectById(1L)).thenReturn(rejected);

            assertDoesNotThrow(() -> archiveService.update(1L, createRequest(10L, 1L)));
        }

        @Test
        @DisplayName("已入库状态不可编辑")
        void approved_cannotEdit_throws() {
            Archive approved = draftArchive();
            approved.setStatus(ArchiveStatus.APPROVED.getCode());
            when(archiveMapper.selectById(1L)).thenReturn(approved);

            BizException ex = assertThrows(BizException.class,
                    () -> archiveService.update(1L, createRequest(10L, 1L)));
            assertTrue(ex.getMessage().contains("编辑"));
        }

        @Test
        @DisplayName("已上架状态不可编辑")
        void published_cannotEdit_throws() {
            Archive published = draftArchive();
            published.setStatus(ArchiveStatus.PUBLISHED.getCode());
            when(archiveMapper.selectById(1L)).thenReturn(published);

            assertThrows(BizException.class,
                    () -> archiveService.update(1L, createRequest(10L, 1L)));
        }

        @Test
        @DisplayName("编辑后状态重置为草稿")
        void editResetsToDraft() {
            Archive rejected = draftArchive();
            rejected.setStatus(ArchiveStatus.REJECTED.getCode());
            when(archiveMapper.selectById(1L)).thenReturn(rejected);

            archiveService.update(1L, createRequest(10L, 1L));

            verify(archiveMapper).updateById(argThat(a ->
                    ArchiveStatus.DRAFT.getCode().equals(a.getStatus())));
        }

        @Test
        @DisplayName("档案不存在抛出异常")
        void archiveNotFound_throws() {
            when(archiveMapper.selectById(999L)).thenReturn(null);

            assertThrows(BizException.class,
                    () -> archiveService.update(999L, createRequest(10L, 1L)));
        }
    }

    // ── 删除 ──

    @Nested
    @DisplayName("删除档案（级联）")
    class Delete {

        @Test
        @DisplayName("删除档案时级联删除媒体文件和记录")
        void cascadesMediaAndFiles() {
            when(archiveMapper.selectById(1L)).thenReturn(draftArchive());
            ArchiveMedia media = new ArchiveMedia();
            media.setUrl("/uploads/images/test.jpg");
            media.setThumbnail("/uploads/thumbnails/thumb_test.jpg");
            when(mediaMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(media));

            archiveService.delete(1L);

            verify(fileStorage).delete("/uploads/images/test.jpg");
            verify(fileStorage).delete("/uploads/thumbnails/thumb_test.jpg");
            verify(mediaMapper).delete(any(LambdaQueryWrapper.class));
            verify(archiveMapper).deleteById(1L);
        }

        @Test
        @DisplayName("无媒体文件时正常删除")
        void noMedia_cleanDelete() {
            when(archiveMapper.selectById(1L)).thenReturn(draftArchive());
            when(mediaMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());

            assertDoesNotThrow(() -> archiveService.delete(1L));
            verify(archiveMapper).deleteById(1L);
        }

        @Test
        @DisplayName("档案不存在抛出异常")
        void notFound_throws() {
            when(archiveMapper.selectById(999L)).thenReturn(null);

            assertThrows(BizException.class, () -> archiveService.delete(999L));
        }
    }

    // ── 媒体排序 ──

    @Nested
    @DisplayName("媒体排序")
    class MediaSort {

        @Test
        @DisplayName("按 ID 列表顺序更新排序值")
        void sort_updatesCorrectly() {
            ArchiveMedia m1 = new ArchiveMedia(); m1.setId(10L); m1.setArchiveId(1L);
            ArchiveMedia m2 = new ArchiveMedia(); m2.setId(20L); m2.setArchiveId(1L);
            when(mediaMapper.selectById(10L)).thenReturn(m1);
            when(mediaMapper.selectById(20L)).thenReturn(m2);

            archiveService.sortMedia(1L, List.of(20L, 10L));

            verify(mediaMapper).updateById(argThat(m -> m.getId().equals(20L) && m.getSort() == 0));
            verify(mediaMapper).updateById(argThat(m -> m.getId().equals(10L) && m.getSort() == 1));
        }

        @Test
        @DisplayName("非本档案的媒体不参与排序")
        void foreignArchive_ignored() {
            ArchiveMedia m = new ArchiveMedia(); m.setId(10L); m.setArchiveId(2L); // different archive
            when(mediaMapper.selectById(10L)).thenReturn(m);

            archiveService.sortMedia(1L, List.of(10L));

            verify(mediaMapper, never()).updateById(any());
        }
    }
}
