package com.campus.exhibition.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.dto.ScreenSearchQuery;
import com.campus.exhibition.dto.WallQuery;
import com.campus.exhibition.vo.AlumniTimelineVO;
import com.campus.exhibition.vo.ScreenCarouselVO;
import com.campus.exhibition.vo.ScreenSearchResultVO;
import com.campus.exhibition.vo.WallItemVO;

public interface ScreenService {

    /** 默认轮播方案 + 项 */
    ScreenCarouselVO getCarousel();

    /** 校友墙分页 */
    Page<WallItemVO> getWall(WallQuery query, int pageNum, int pageSize);

    /** 校友成长轨迹 */
    AlumniTimelineVO getAlumniTimeline(Long alumniId);

    /** 搜索 */
    Page<ScreenSearchResultVO> search(ScreenSearchQuery query, int pageNum, int pageSize);
}
