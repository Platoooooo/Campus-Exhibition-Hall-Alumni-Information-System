package com.campus.exhibition.service;

import com.campus.exhibition.dto.CarouselItemRequest;
import com.campus.exhibition.dto.CarouselSaveRequest;
import com.campus.exhibition.vo.ArchiveVO;
import com.campus.exhibition.vo.CarouselVO;

import java.util.List;

public interface OperationService {

    /* ---- 上架/下架 ---- */
    ArchiveVO publish(Long archiveId);
    ArchiveVO unpublish(Long archiveId);

    /* ---- 运营属性 ---- */
    ArchiveVO setTop(Long archiveId, int isTop);
    ArchiveVO setRecommend(Long archiveId, int isRecommend);
    ArchiveVO setDisplaySort(Long archiveId, int sort);

    /* ---- 轮播方案 CRUD ---- */
    CarouselVO createCarousel(CarouselSaveRequest request);
    CarouselVO updateCarousel(Long id, CarouselSaveRequest request);
    void deleteCarousel(Long id);
    CarouselVO getCarousel(Long id);
    List<CarouselVO> listCarousels();

    /* ---- 轮播池内容 ---- */
    CarouselVO addCarouselItem(Long carouselId, CarouselItemRequest request);
    void removeCarouselItem(Long carouselId, Long itemId);
    void sortCarouselItems(Long carouselId, List<Long> itemIds);
}
