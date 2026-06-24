package com.campus.exhibition.controller;

import com.campus.exhibition.common.R;
import com.campus.exhibition.dto.CarouselItemRequest;
import com.campus.exhibition.dto.CarouselSaveRequest;
import com.campus.exhibition.service.OperationService;
import com.campus.exhibition.vo.ArchiveVO;
import com.campus.exhibition.vo.CarouselVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资料库运营（仅校级管理员 admin）
 */
@RestController
@RequestMapping("/api/operation")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('admin')")
public class OperationController {

    private final OperationService operationService;

    /* ---- 上架/下架 ---- */

    @PutMapping("/archive/{id}/publish")
    public R<ArchiveVO> publish(@PathVariable Long id) {
        return R.ok(operationService.publish(id));
    }

    @PutMapping("/archive/{id}/unpublish")
    public R<ArchiveVO> unpublish(@PathVariable Long id) {
        return R.ok(operationService.unpublish(id));
    }

    /* ---- 运营属性 ---- */

    @PutMapping("/archive/{id}/top")
    public R<ArchiveVO> setTop(@PathVariable Long id, @RequestParam int value) {
        return R.ok(operationService.setTop(id, value));
    }

    @PutMapping("/archive/{id}/recommend")
    public R<ArchiveVO> setRecommend(@PathVariable Long id, @RequestParam int value) {
        return R.ok(operationService.setRecommend(id, value));
    }

    @PutMapping("/archive/{id}/sort")
    public R<ArchiveVO> setDisplaySort(@PathVariable Long id, @RequestParam int value) {
        return R.ok(operationService.setDisplaySort(id, value));
    }

    /* ---- 轮播方案 ---- */

    @GetMapping("/carousel")
    public R<List<CarouselVO>> listCarousels() {
        return R.ok(operationService.listCarousels());
    }

    @GetMapping("/carousel/{id}")
    public R<CarouselVO> getCarousel(@PathVariable Long id) {
        return R.ok(operationService.getCarousel(id));
    }

    @PostMapping("/carousel")
    public R<CarouselVO> createCarousel(@Valid @RequestBody CarouselSaveRequest request) {
        return R.ok(operationService.createCarousel(request));
    }

    @PutMapping("/carousel/{id}")
    public R<CarouselVO> updateCarousel(@PathVariable Long id,
                                        @Valid @RequestBody CarouselSaveRequest request) {
        return R.ok(operationService.updateCarousel(id, request));
    }

    @DeleteMapping("/carousel/{id}")
    public R<Void> deleteCarousel(@PathVariable Long id) {
        operationService.deleteCarousel(id);
        return R.ok();
    }

    /* ---- 轮播池内容 ---- */

    @PostMapping("/carousel/{id}/item")
    public R<CarouselVO> addItem(@PathVariable Long id,
                                 @RequestBody CarouselItemRequest request) {
        return R.ok(operationService.addCarouselItem(id, request));
    }

    @DeleteMapping("/carousel/{carouselId}/item/{itemId}")
    public R<Void> removeItem(@PathVariable Long carouselId, @PathVariable Long itemId) {
        operationService.removeCarouselItem(carouselId, itemId);
        return R.ok();
    }

    @PutMapping("/carousel/{id}/items/sort")
    public R<Void> sortItems(@PathVariable Long id, @RequestBody List<Long> itemIds) {
        operationService.sortCarouselItems(id, itemIds);
        return R.ok();
    }
}
