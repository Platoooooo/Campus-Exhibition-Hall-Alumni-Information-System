package com.campus.exhibition.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.common.R;
import com.campus.exhibition.dto.ScreenSearchQuery;
import com.campus.exhibition.dto.WallQuery;
import com.campus.exhibition.service.ScreenService;
import com.campus.exhibition.vo.AlumniTimelineVO;
import com.campus.exhibition.vo.ScreenCarouselVO;
import com.campus.exhibition.vo.ScreenSearchResultVO;
import com.campus.exhibition.vo.WallItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 大屏公开只读 API —— 无需认证，严格仅返回 published 数据
 */
@RestController
@RequestMapping("/api/screen")
@RequiredArgsConstructor
public class ScreenController {

    private final ScreenService screenService;

    /** 默认轮播方案及项 */
    @GetMapping("/carousel")
    public R<ScreenCarouselVO> carousel() {
        return R.ok(screenService.getCarousel());
    }

    /** 校友墙（分页，支持分类/届别筛选） */
    @GetMapping("/wall")
    public R<Page<WallItemVO>> wall(
            WallQuery query,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(screenService.getWall(query, pageNum, pageSize));
    }

    /** 校友成长轨迹 */
    @GetMapping("/alumni/{id}")
    public R<AlumniTimelineVO> alumniTimeline(@PathVariable Long id) {
        return R.ok(screenService.getAlumniTimeline(id));
    }

    /** 搜索 */
    @GetMapping("/search")
    public R<Page<ScreenSearchResultVO>> search(
            ScreenSearchQuery query,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(screenService.search(query, pageNum, pageSize));
    }
}
