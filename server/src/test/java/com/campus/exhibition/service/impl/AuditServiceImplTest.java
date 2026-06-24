package com.campus.exhibition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.common.BizException;
import com.campus.exhibition.common.ErrorCode;
import com.campus.exhibition.common.UserContext;
import com.campus.exhibition.entity.*;
import com.campus.exhibition.enums.ArchiveStatus;
import com.campus.exhibition.enums.RoleEnum;
import com.campus.exhibition.mapper.*;
import com.campus.exhibition.security.LoginUser;
import com.campus.exhibition.vo.ArchiveVO;
import com.campus.exhibition.vo.AuditLogVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuditServiceImpl 审核服务单元测试
 * 策略：纯 Mockito，不启动 Spring Context
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService 审核服务")
class AuditServiceImplTest {

    @Mock private ArchiveMapper archiveMapper;
    @Mock private AuditLogMapper auditLogMapper;
    @Mock private AlumniMapper alumniMapper;
    @Mock private SysCollegeMapper collegeMapper;
    @Mock private ArchiveCategoryMapper categoryMapper;
    @Mock private ArchiveMediaMapper mediaMapper;
    @InjectMocks private AuditServiceImpl auditService;

    private MockedStatic<UserContext> userContextMock;
    private Archive draftArchive;

    @BeforeEach
    void setUp() {
        userContextMock = mockStatic(UserContext.class);
        // 默认用户：学院管理员
        userContextMock.when(UserContext::userId).thenReturn(100L);
        userContextMock.when(UserContext::role).thenReturn("college");
        userContextMock.when(UserContext::collegeId).thenReturn(1L);

        LoginUser mockUser = new LoginUser(100L, "college_admin", "pwd", "college", 1L, true);
        userContextMock.when(UserContext::currentUser).thenReturn(mockUser);

        draftArchive = new Archive();
        draftArchive.setId(1L);
        draftArchive.setTitle("测试档案");
        draftArchive.setAlumniId(10L);
        draftArchive.setCollegeId(1L);
        draftArchive.setStatus(ArchiveStatus.DRAFT.getCode());
    }

    @AfterEach
    void tearDown() {
        userContextMock.close();
    }

    // ── 提交审核 ──

    @Nested
    @DisplayName("提交审核")
    class Submit {

        @Test
        @DisplayName("草稿状态提交成功")
        void submitDraft_success() {
            when(archiveMapper.selectById(1L)).thenReturn(draftArchive);

            ArchiveVO result = auditService.submit(1L);

            assertNotNull(result);
            verify(archiveMapper).updateById(argThat(a ->
                    ArchiveStatus.PENDING_COLLEGE.getCode().equals(a.getStatus())));
        }

        @Test
        @DisplayName("档案不存在抛出异常")
        void archiveNotFound_throws() {
            when(archiveMapper.selectById(999L)).thenReturn(null);

            BizException ex = assertThrows(BizException.class, () -> auditService.submit(999L));
            assertEquals(ErrorCode.ARCHIVE_NOT_FOUND.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非草稿状态提交失败")
        void nonDraft_throws() {
            draftArchive.setStatus(ArchiveStatus.APPROVED.getCode());
            when(archiveMapper.selectById(1L)).thenReturn(draftArchive);

            BizException ex = assertThrows(BizException.class, () -> auditService.submit(1L));
            assertEquals(ErrorCode.AUDIT_STATUS_INVALID.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("提交时设置提交人和时间")
        void submit_setsSubmitUserAndTime() {
            when(archiveMapper.selectById(1L)).thenReturn(draftArchive);

            auditService.submit(1L);

            ArgumentCaptor<Archive> captor = ArgumentCaptor.forClass(Archive.class);
            verify(archiveMapper).updateById(captor.capture());
            Archive updated = captor.getValue();
            assertEquals(100L, updated.getSubmitUser());
            assertNotNull(updated.getSubmitTime());
        }
    }

    // ── 学院审核 ──

    @Nested
    @DisplayName("学院审核")
    class CollegeAudit {

        @Test
        @DisplayName("学院通过：待学院审核 → 待教务处审核")
        void approve_success() {
            draftArchive.setStatus(ArchiveStatus.PENDING_COLLEGE.getCode());
            when(archiveMapper.selectById(1L)).thenReturn(draftArchive);

            ArchiveVO result = auditService.collegeApprove(1L, "审核通过");

            verify(archiveMapper).updateById(argThat(a ->
                    ArchiveStatus.PENDING_ACADEMIC.getCode().equals(a.getStatus())));
            verify(auditLogMapper).insert(any(AuditLog.class));
        }

        @Test
        @DisplayName("学院驳回：必须填写意见")
        void reject_withOpinion_success() {
            draftArchive.setStatus(ArchiveStatus.PENDING_COLLEGE.getCode());
            when(archiveMapper.selectById(1L)).thenReturn(draftArchive);

            ArchiveVO result = auditService.collegeReject(1L, "照片不清晰");

            verify(archiveMapper).updateById(argThat(a ->
                    ArchiveStatus.REJECTED.getCode().equals(a.getStatus())));
        }

        @Test
        @DisplayName("学院驳回：无意见抛出异常")
        void reject_withoutOpinion_throws() {
            draftArchive.setStatus(ArchiveStatus.PENDING_COLLEGE.getCode());
            when(archiveMapper.selectById(1L)).thenReturn(draftArchive);

            BizException ex = assertThrows(BizException.class,
                    () -> auditService.collegeReject(1L, null));
            assertEquals(ErrorCode.AUDIT_OPINION_REQUIRED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("学院驳回：空白意见抛出异常")
        void reject_blankOpinion_throws() {
            draftArchive.setStatus(ArchiveStatus.PENDING_COLLEGE.getCode());
            when(archiveMapper.selectById(1L)).thenReturn(draftArchive);

            BizException ex = assertThrows(BizException.class,
                    () -> auditService.collegeReject(1L, "   "));
            assertEquals(ErrorCode.AUDIT_OPINION_REQUIRED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("非学院角色操作被拒绝")
        void wrongRole_throws() {
            userContextMock.when(UserContext::role).thenReturn("academic");

            BizException ex = assertThrows(BizException.class,
                    () -> auditService.collegeApprove(1L, ""));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("跨学院操作被拒绝")
        void outOfScope_throws() {
            // user collegeId=1, but archive collegeId=2
            draftArchive.setCollegeId(2L);
            when(archiveMapper.selectById(1L)).thenReturn(draftArchive);

            BizException ex = assertThrows(BizException.class,
                    () -> auditService.collegeApprove(1L, ""));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("Admin 角色可绕过学院范围限制")
        void adminBypassScope_success() {
            userContextMock.when(UserContext::role).thenReturn("admin");
            draftArchive.setCollegeId(2L); // different college
            draftArchive.setStatus(ArchiveStatus.PENDING_COLLEGE.getCode());
            when(archiveMapper.selectById(1L)).thenReturn(draftArchive);

            assertDoesNotThrow(() -> auditService.collegeApprove(1L, "校级审核通过"));
        }

        @Test
        @DisplayName("未登录用户操作被拒绝")
        void noAuth_throws() {
            userContextMock.when(UserContext::role).thenReturn(null);

            BizException ex = assertThrows(BizException.class,
                    () -> auditService.collegeApprove(1L, ""));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }
    }

    // ── 教务处审核 ──

    @Nested
    @DisplayName("教务处审核")
    class AcademicAudit {

        @BeforeEach
        void setAcademicUser() {
            userContextMock.when(UserContext::role).thenReturn("academic");
        }

        @Test
        @DisplayName("教务处通过：待教务处审核 → 已入库")
        void approve_success() {
            draftArchive.setStatus(ArchiveStatus.PENDING_ACADEMIC.getCode());
            when(archiveMapper.selectById(1L)).thenReturn(draftArchive);

            ArchiveVO result = auditService.academicApprove(1L, "同意入库");

            verify(archiveMapper).updateById(argThat(a ->
                    ArchiveStatus.APPROVED.getCode().equals(a.getStatus())));
        }

        @Test
        @DisplayName("教务处驳回：需填写意见")
        void reject_withOpinion_success() {
            draftArchive.setStatus(ArchiveStatus.PENDING_ACADEMIC.getCode());
            when(archiveMapper.selectById(1L)).thenReturn(draftArchive);

            ArchiveVO result = auditService.academicReject(1L, "内容不完整");

            verify(archiveMapper).updateById(argThat(a ->
                    ArchiveStatus.REJECTED.getCode().equals(a.getStatus())));
        }

        @Test
        @DisplayName("教务处驳回：无意见抛出异常")
        void reject_withoutOpinion_throws() {
            draftArchive.setStatus(ArchiveStatus.PENDING_ACADEMIC.getCode());
            when(archiveMapper.selectById(1L)).thenReturn(draftArchive);

            assertThrows(BizException.class,
                    () -> auditService.academicReject(1L, ""));
        }

        @Test
        @DisplayName("非教务处角色被拒绝")
        void wrongRole_throws() {
            userContextMock.when(UserContext::role).thenReturn("college");

            BizException ex = assertThrows(BizException.class,
                    () -> auditService.academicApprove(1L, ""));
            assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        }
    }

    // ── 待办列表 ──

    @Nested
    @DisplayName("待办列表")
    class TodoLists {

        @Test
        @DisplayName("学院待办：college 用户只看本院")
        void collegeTodo_collegeUser_filtersByCollege() {
            Page<Archive> mockPage = new Page<>(1, 10, 0);
            mockPage.setRecords(List.of());
            when(archiveMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            Page<ArchiveVO> result = auditService.collegeTodo(1, 10);
            assertNotNull(result);
        }

        @Test
        @DisplayName("学院待办：admin 看全校")
        void collegeTodo_adminUser_seesAll() {
            userContextMock.when(UserContext::role).thenReturn("admin");
            Page<Archive> mockPage = new Page<>(1, 10, 0);
            mockPage.setRecords(List.of());
            when(archiveMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            Page<ArchiveVO> result = auditService.collegeTodo(1, 10);
            assertNotNull(result);
        }

        @Test
        @DisplayName("学院待办：未绑定学院的 college 用户抛异常")
        void collegeTodo_noCollegeId_throws() {
            userContextMock.when(UserContext::collegeId).thenReturn(null);

            assertThrows(BizException.class, () -> auditService.collegeTodo(1, 10));
        }

        @Test
        @DisplayName("教务处待办：按 PENDING_ACADEMIC 筛选")
        void academicTodo_returnsCorrectStatus() {
            userContextMock.when(UserContext::role).thenReturn("academic");
            Page<Archive> mockPage = new Page<>(1, 10, 0);
            mockPage.setRecords(List.of());
            when(archiveMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            Page<ArchiveVO> result = auditService.academicTodo(1, 10);
            assertNotNull(result);
        }
    }

    // ── 审核记录 ──

    @Nested
    @DisplayName("审核记录")
    class AuditLogs {

        @Test
        @DisplayName("返回审核日志列表（含档案标题）")
        void auditLogs_returnsWithArchiveTitle() {
            AuditLog log = new AuditLog();
            log.setArchiveId(1L);
            log.setNode("college");
            log.setAction("approve");

            when(auditLogMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(log));
            when(archiveMapper.selectById(1L)).thenReturn(draftArchive);

            List<AuditLogVO> logs = auditService.auditLogs(1L);
            assertEquals(1, logs.size());
            assertEquals("测试档案", logs.get(0).getArchiveTitle());
        }

        @Test
        @DisplayName("无审核记录时返回空列表")
        void auditLogs_empty_returnsEmptyList() {
            when(auditLogMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());

            List<AuditLogVO> logs = auditService.auditLogs(1L);
            assertTrue(logs.isEmpty());
        }
    }
}
