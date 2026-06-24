package com.campus.exhibition.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.common.GlobalExceptionHandler;
import com.campus.exhibition.service.AuditService;
import com.campus.exhibition.vo.ArchiveVO;
import com.campus.exhibition.vo.AuditLogVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuditController 审核接口")
class AuditControllerTest {

    private MockMvc mockMvc;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = mock(AuditService.class);
        AuditController controller = new AuditController(auditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("提交审核成功")
    void submit_success() throws Exception {
        ArchiveVO vo = new ArchiveVO();
        vo.setId(1L);
        vo.setTitle("测试档案");
        when(auditService.submit(1L)).thenReturn(vo);

        mockMvc.perform(put("/api/audit/1/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("学院通过审核")
    void collegeApprove_success() throws Exception {
        ArchiveVO vo = new ArchiveVO();
        vo.setId(1L);
        when(auditService.collegeApprove(eq(1L), any())).thenReturn(vo);

        mockMvc.perform(put("/api/audit/1/college/approve")
                        .contentType("application/json")
                        .content("{\"opinion\":\"通过\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("学院驳回审核")
    void collegeReject_success() throws Exception {
        ArchiveVO vo = new ArchiveVO();
        vo.setId(1L);
        when(auditService.collegeReject(eq(1L), eq("照片不清"))).thenReturn(vo);

        mockMvc.perform(put("/api/audit/1/college/reject")
                        .contentType("application/json")
                        .content("{\"opinion\":\"照片不清\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("学院待办列表")
    void collegeTodo_success() throws Exception {
        Page<ArchiveVO> page = new Page<>(1, 20, 5);
        page.setRecords(List.of());
        when(auditService.collegeTodo(1, 20)).thenReturn(page);

        mockMvc.perform(get("/api/audit/college/todo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(5));
    }

    @Test
    @DisplayName("审核记录查询")
    void auditLogs_success() throws Exception {
        AuditLogVO log = new AuditLogVO();
        log.setNode("college");
        log.setAction("approve");
        when(auditService.auditLogs(1L)).thenReturn(List.of(log));

        mockMvc.perform(get("/api/audit/1/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].node").value("college"));
    }
}
