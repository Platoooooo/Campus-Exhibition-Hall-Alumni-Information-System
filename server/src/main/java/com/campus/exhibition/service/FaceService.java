package com.campus.exhibition.service;

import com.campus.exhibition.dto.face.FaceRecognizeRequest;
import com.campus.exhibition.vo.FaceEnrollVO;
import com.campus.exhibition.vo.FaceRecognizeVO;
import org.springframework.web.multipart.MultipartFile;

public interface FaceService {

    /** 录入校友人脸 */
    FaceEnrollVO enroll(Long alumniId, MultipartFile file);

    /** 大屏识别 */
    FaceRecognizeVO recognize(FaceRecognizeRequest request);
}
