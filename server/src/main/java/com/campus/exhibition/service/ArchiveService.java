package com.campus.exhibition.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.dto.ArchiveQuery;
import com.campus.exhibition.dto.ArchiveSaveRequest;
import com.campus.exhibition.vo.ArchiveVO;
import org.springframework.web.multipart.MultipartFile;

public interface ArchiveService {

    Page<ArchiveVO> page(ArchiveQuery query, long pageNum, long pageSize);

    ArchiveVO getById(Long id);

    ArchiveVO create(ArchiveSaveRequest request);

    ArchiveVO update(Long id, ArchiveSaveRequest request);

    void delete(Long id);

    /** 为档案添加媒体 */
    ArchiveVO addMedia(Long archiveId, MultipartFile file);

    /** 删除档案媒体 */
    void removeMedia(Long archiveId, Long mediaId);

    /** 媒体排序 */
    void sortMedia(Long archiveId, java.util.List<Long> mediaIds);
}
