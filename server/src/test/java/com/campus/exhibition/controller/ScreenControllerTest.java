package com.campus.exhibition.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.common.GlobalExceptionHandler;
import com.campus.exhibition.service.ScreenService;
import com.campus.exhibition.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ScreenController 大屏接口")
class ScreenControllerTest {

    private MockMvc mockMvc;
    private ScreenService screenService;

    @BeforeEach
    void setUp() {
        screenService = mock(ScreenService.class);
        ScreenController controller = new ScreenController(screenService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("获取默认轮播方案")
    void carousel_success() throws Exception {
        ScreenCarouselVO vo = new ScreenCarouselVO();
        vo.setId(1L);
        vo.setName("默认方案");
        when(screenService.getCarousel()).thenReturn(vo);

        mockMvc.perform(get("/api/screen/carousel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("默认方案"));
    }

    @Test
    @DisplayName("校友墙分页查询")
    void wall_success() throws Exception {
        Page<WallItemVO> page = new Page<>(1, 20, 0);
        page.setRecords(List.of());
        when(screenService.getWall(any(), eq(1), eq(20))).thenReturn(page);

        mockMvc.perform(get("/api/screen/wall")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("校友成长轨迹")
    void alumniTimeline_success() throws Exception {
        AlumniTimelineVO vo = new AlumniTimelineVO();
        vo.setAlumniId(1L);
        vo.setName("张三");
        vo.setTimeline(List.of());
        when(screenService.getAlumniTimeline(1L)).thenReturn(vo);

        mockMvc.perform(get("/api/screen/alumni/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.alumniId").value(1))
                .andExpect(jsonPath("$.data.name").value("张三"));
    }

    @Test
    @DisplayName("全文搜索")
    void search_success() throws Exception {
        Page<ScreenSearchResultVO> page = new Page<>(1, 20, 0);
        page.setRecords(List.of());
        when(screenService.search(any(), eq(1), eq(20))).thenReturn(page);

        mockMvc.perform(get("/api/screen/search")
                        .param("keyword", "ACM")
                        .param("pageNum", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
    }
}
