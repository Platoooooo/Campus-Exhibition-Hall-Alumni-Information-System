package com.campus.exhibition.controller;

import com.campus.exhibition.common.BizException;
import com.campus.exhibition.common.R;
import com.campus.exhibition.dto.face.FaceRecognizeRequest;
import com.campus.exhibition.service.FaceService;
import com.campus.exhibition.vo.FaceEnrollVO;
import com.campus.exhibition.vo.FaceRecognizeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 人脸识别接口
 * - enroll: 录入校友人脸（admin）
 * - recognize: 大屏识别定位（匿名）
 */
@RestController
@RequestMapping("/api/face")
@RequiredArgsConstructor
public class FaceController {

    private final FaceService faceService;

    /** 录入校友人脸 */
    @PostMapping("/enroll")
    @PreAuthorize("hasAuthority('admin')")
    public R<FaceEnrollVO> enroll(@RequestParam Long alumniId,
                                  @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return R.fail(400, "文件为空");
        try {
            return R.ok(faceService.enroll(alumniId, file));
        } catch (BizException e) {
            return R.fail(e.getCode(), e.getMessage());
        }
    }

    /** 大屏识别（匿名） */
    @PostMapping("/recognize")
    public R<FaceRecognizeVO> recognize(@Valid @RequestBody FaceRecognizeRequest request) {
        try {
            return R.ok(faceService.recognize(request));
        } catch (Exception e) {
            // 降级：永远不向大屏抛异常
            return R.ok(FaceRecognizeVO.builder()
                    .status("DEGRADED").alumniId(null).score(null)
                    .alumni(null).timeline(java.util.List.of()).build());
        }
    }
}
