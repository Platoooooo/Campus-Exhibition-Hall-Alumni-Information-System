package com.campus.exhibition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.common.BizException;
import com.campus.exhibition.common.DataScopeHelper;
import com.campus.exhibition.common.ErrorCode;
import com.campus.exhibition.dto.AlumniQuery;
import com.campus.exhibition.dto.AlumniSaveRequest;
import com.campus.exhibition.entity.Alumni;
import com.campus.exhibition.entity.SysCollege;
import com.campus.exhibition.entity.SysMajor;
import com.campus.exhibition.mapper.AlumniMapper;
import com.campus.exhibition.mapper.SysCollegeMapper;
import com.campus.exhibition.mapper.SysMajorMapper;
import com.campus.exhibition.service.AlumniService;
import com.campus.exhibition.vo.AlumniVO;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlumniServiceImpl implements AlumniService {

    private final AlumniMapper alumniMapper;
    private final SysCollegeMapper collegeMapper;
    private final SysMajorMapper majorMapper;

    @Override
    public Page<AlumniVO> page(AlumniQuery query, long pageNum, long pageSize) {
        LambdaQueryWrapper<Alumni> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            wrapper.like(StringUtils.hasText(query.getName()), Alumni::getName, query.getName());
            wrapper.like(StringUtils.hasText(query.getStudentNo()), Alumni::getStudentNo, query.getStudentNo());
            wrapper.eq(query.getCollegeId() != null, Alumni::getCollegeId, query.getCollegeId());
            wrapper.eq(query.getMajorId() != null, Alumni::getMajorId, query.getMajorId());
            wrapper.eq(query.getGradYear() != null, Alumni::getGradYear, query.getGradYear());
            wrapper.eq(query.getIdentity() != null, Alumni::getIdentity, query.getIdentity());
            wrapper.eq(query.getStatus() != null, Alumni::getStatus, query.getStatus());
        }
        // 数据权限：学院管理员只能看本院
        DataScopeHelper.applyCollegeScope(wrapper, Alumni::getCollegeId);
        wrapper.orderByDesc(Alumni::getCreateTime);

        Page<Alumni> page = alumniMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
        Page<AlumniVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(toVOList(page.getRecords()));
        return voPage;
    }

    @Override
    public AlumniVO getById(Long id) {
        Alumni entity = alumniMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "校友不存在");
        return toVO(entity);
    }

    @Override
    @Transactional
    public AlumniVO create(AlumniSaveRequest request) {
        // 学号唯一校验
        if (alumniMapper.selectCount(
                new LambdaQueryWrapper<Alumni>().eq(Alumni::getStudentNo, request.getStudentNo())) > 0) {
            throw new BizException(400, "学号已存在");
        }
        // 学院存在校验
        if (collegeMapper.selectById(request.getCollegeId()) == null) {
            throw new BizException(400, "所属学院不存在");
        }
        Alumni entity = new Alumni();
        BeanUtils.copyProperties(request, entity);
        if (entity.getIdentity() == null) entity.setIdentity(1);
        if (entity.getFaceStatus() == null) entity.setFaceStatus(0);
        if (entity.getStatus() == null) entity.setStatus(1);
        alumniMapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public AlumniVO update(Long id, AlumniSaveRequest request) {
        Alumni entity = alumniMapper.selectById(id);
        if (entity == null) throw new BizException(ErrorCode.NOT_FOUND, "校友不存在");

        // 学号唯一校验（排除自身）
        Long count = alumniMapper.selectCount(
                new LambdaQueryWrapper<Alumni>()
                        .eq(Alumni::getStudentNo, request.getStudentNo())
                        .ne(Alumni::getId, id));
        if (count > 0) throw new BizException(400, "学号已存在");

        if (collegeMapper.selectById(request.getCollegeId()) == null) {
            throw new BizException(400, "所属学院不存在");
        }

        BeanUtils.copyProperties(request, entity);
        alumniMapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (alumniMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "校友不存在");
        }
        alumniMapper.deleteById(id);
    }

    /* ---- Excel 批量导入 ---- */

    @Override
    @Transactional
    public ImportResult importExcel(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int total = 0, success = 0;
            List<String> errors = new ArrayList<>();

            // 预加载学院编码→ID 映射
            Map<String, Long> collegeMap = collegeMapper.selectList(
                    new LambdaQueryWrapper<SysCollege>().eq(SysCollege::getStatus, 1)
            ).stream().collect(Collectors.toMap(SysCollege::getCode, SysCollege::getId, (a, b) -> a));

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {  // 跳过表头
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) continue;
                total++;
                try {
                    String studentNo = getCellStr(row, 0);
                    String name = getCellStr(row, 1);
                    String genderStr = getCellStr(row, 2);
                    String collegeCode = getCellStr(row, 3);
                    String enrollYearStr = getCellStr(row, 4);
                    String gradYearStr = getCellStr(row, 5);
                    String identityStr = getCellStr(row, 6);
                    String summary = getCellStr(row, 7);

                    // 必填校验
                    if (!StringUtils.hasText(studentNo)) throw new IllegalArgumentException("学号为空");
                    if (!StringUtils.hasText(name)) throw new IllegalArgumentException("姓名为空");
                    if (!StringUtils.hasText(collegeCode)) throw new IllegalArgumentException("学院编码为空");

                    // 学号唯一
                    if (alumniMapper.selectCount(
                            new LambdaQueryWrapper<Alumni>().eq(Alumni::getStudentNo, studentNo)) > 0) {
                        throw new IllegalArgumentException("学号 " + studentNo + " 已存在");
                    }

                    // 学院存在
                    Long collegeId = collegeMap.get(collegeCode);
                    if (collegeId == null) throw new IllegalArgumentException("学院编码 " + collegeCode + " 不存在");

                    Alumni alumni = new Alumni();
                    alumni.setStudentNo(studentNo);
                    alumni.setName(name);
                    alumni.setGender(parseGender(genderStr));
                    alumni.setCollegeId(collegeId);
                    alumni.setEnrollYear(parseInt(enrollYearStr));
                    alumni.setGradYear(parseInt(gradYearStr));
                    alumni.setIdentity(parseIdentity(identityStr));
                    alumni.setSummary(summary);
                    alumni.setFaceStatus(0);
                    alumni.setStatus(1);
                    alumniMapper.insert(alumni);
                    success++;
                } catch (Exception e) {
                    errors.add("第" + (i + 1) + "行: " + e.getMessage());
                }
            }
            return new ImportResult(total, success, total - success, errors);
        } catch (IOException e) {
            throw new BizException(400, "Excel 文件读取失败: " + e.getMessage());
        }
    }

    /** 生成导入模板 */
    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("校友导入模板");
            Row header = sheet.createRow(0);
            String[] titles = {"学号*", "姓名*", "性别(男/女)", "学院编码*", "入学年份", "毕业年份", "身份(在校生/校友)", "简介"};
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < titles.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(titles[i]);
                cell.setCellStyle(headerStyle);
            }
            for (int i = 0; i < titles.length; i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new BizException(500, "模板生成失败");
        }
    }

    /* ---- 辅助方法 ---- */

    private AlumniVO toVO(Alumni entity) {
        AlumniVO vo = new AlumniVO();
        BeanUtils.copyProperties(entity, vo);
        SysCollege college = collegeMapper.selectById(entity.getCollegeId());
        if (college != null) vo.setCollegeName(college.getName());
        if (entity.getMajorId() != null) {
            SysMajor major = majorMapper.selectById(entity.getMajorId());
            if (major != null) vo.setMajorName(major.getName());
        }
        return vo;
    }

    private List<AlumniVO> toVOList(List<Alumni> list) {
        if (list.isEmpty()) return List.of();
        Set<Long> collegeIds = list.stream().map(Alumni::getCollegeId).collect(Collectors.toSet());
        Set<Long> majorIds = list.stream().map(Alumni::getMajorId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> collegeMap = collegeIds.isEmpty() ? Map.of() :
                collegeMapper.selectBatchIds(collegeIds).stream()
                        .collect(Collectors.toMap(SysCollege::getId, SysCollege::getName));
        Map<Long, String> majorMap = majorIds.isEmpty() ? Map.of() :
                majorMapper.selectBatchIds(majorIds).stream()
                        .collect(Collectors.toMap(SysMajor::getId, SysMajor::getName));

        return list.stream().map(e -> {
            AlumniVO vo = new AlumniVO();
            BeanUtils.copyProperties(e, vo);
            vo.setCollegeName(collegeMap.getOrDefault(e.getCollegeId(), ""));
            if (e.getMajorId() != null) vo.setMajorName(majorMap.getOrDefault(e.getMajorId(), ""));
            return vo;
        }).collect(Collectors.toList());
    }

    private String getCellStr(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield v == (long) v ? String.valueOf((long) v) : String.valueOf(v);
            }
            default -> "";
        };
    }

    private Integer parseGender(String s) {
        if (!StringUtils.hasText(s)) return null;
        s = s.trim();
        if ("男".equals(s) || "1".equals(s)) return 1;
        if ("女".equals(s) || "2".equals(s)) return 2;
        return null;
    }

    private Integer parseInt(String s) {
        if (!StringUtils.hasText(s)) return null;
        try { return Integer.valueOf(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private Integer parseIdentity(String s) {
        if (!StringUtils.hasText(s)) return 1;
        s = s.trim();
        if ("校友".equals(s) || "2".equals(s)) return 2;
        return 1;
    }

    private boolean isEmptyRow(Row row) {
        for (int i = 0; i < 8; i++) {
            if (StringUtils.hasText(getCellStr(row, i))) return false;
        }
        return true;
    }
}
