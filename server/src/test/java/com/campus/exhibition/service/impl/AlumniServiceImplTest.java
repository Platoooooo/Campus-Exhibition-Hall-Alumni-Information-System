package com.campus.exhibition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.exhibition.common.BizException;
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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AlumniServiceImpl 校友服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlumniService 校友服务")
class AlumniServiceImplTest {

    @Mock private AlumniMapper alumniMapper;
    @Mock private SysCollegeMapper collegeMapper;
    @Mock private SysMajorMapper majorMapper;
    @InjectMocks private AlumniServiceImpl alumniService;

    private Alumni createAlumni(Long id, String studentNo, String name, Long collegeId) {
        Alumni a = new Alumni();
        a.setId(id);
        a.setStudentNo(studentNo);
        a.setName(name);
        a.setCollegeId(collegeId);
        a.setStatus(1);
        return a;
    }

    private AlumniSaveRequest createRequest(String studentNo, String name, Long collegeId) {
        AlumniSaveRequest req = new AlumniSaveRequest();
        req.setStudentNo(studentNo);
        req.setName(name);
        req.setCollegeId(collegeId);
        return req;
    }

    // ── 查询 ──

    @Nested
    @DisplayName("分页查询")
    class Paging {

        @Test
        @DisplayName("默认分页返回空")
        void page_empty() {
            Page<Alumni> mockPage = new Page<>(1, 10, 0);
            mockPage.setRecords(List.of());
            when(alumniMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);

            Page<AlumniVO> result = alumniService.page(new AlumniQuery(), 1, 10);
            assertEquals(0, result.getTotal());
        }

        @Test
        @DisplayName("查询结果含学院名称")
        void page_withResults_includesCollegeName() {
            Alumni alumni = createAlumni(1L, "2024001", "张三", 1L);
            Page<Alumni> mockPage = new Page<>(1, 10, 1);
            mockPage.setRecords(List.of(alumni));
            when(alumniMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(mockPage);
            SysCollege college = new SysCollege();
            college.setId(1L);
            college.setName("计算机学院");
            when(collegeMapper.selectBatchIds(anySet())).thenReturn(List.of(college));

            Page<AlumniVO> result = alumniService.page(new AlumniQuery(), 1, 10);
            assertEquals(1, result.getTotal());
            assertEquals("张三", result.getRecords().get(0).getName());
        }
    }

    @Nested
    @DisplayName("详情查询")
    class GetById {

        @Test
        @DisplayName("查询存在的校友")
        void getById_found() {
            Alumni alumni = createAlumni(1L, "2024001", "张三", 1L);
            when(alumniMapper.selectById(1L)).thenReturn(alumni);
            SysCollege college = new SysCollege();
            college.setId(1L);
            college.setName("计算机学院");
            when(collegeMapper.selectById(1L)).thenReturn(college);

            AlumniVO vo = alumniService.getById(1L);
            assertEquals("张三", vo.getName());
            assertEquals("计算机学院", vo.getCollegeName());
        }

        @Test
        @DisplayName("查询不存在的校友抛出异常")
        void getById_notFound_throws() {
            when(alumniMapper.selectById(999L)).thenReturn(null);

            BizException ex = assertThrows(BizException.class, () -> alumniService.getById(999L));
            assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        }
    }

    // ── 创建 ──

    @Nested
    @DisplayName("创建校友")
    class Create {

        @Test
        @DisplayName("正常创建成功")
        void create_success() {
            when(alumniMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            SysCollege college = new SysCollege();
            college.setId(1L);
            college.setName("计算机学院");
            when(collegeMapper.selectById(1L)).thenReturn(college);

            AlumniSaveRequest req = createRequest("2024001", "张三", 1L);
            AlumniVO vo = alumniService.create(req);

            verify(alumniMapper).insert(any(Alumni.class));
            assertNotNull(vo);
        }

        @Test
        @DisplayName("学号重复抛出异常")
        void duplicateStudentNo_throws() {
            when(alumniMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            AlumniSaveRequest req = createRequest("2024001", "张三", 1L);
            BizException ex = assertThrows(BizException.class, () -> alumniService.create(req));
            assertTrue(ex.getMessage().contains("学号"));
        }

        @Test
        @DisplayName("学院不存在抛出异常")
        void unknownCollege_throws() {
            when(alumniMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(collegeMapper.selectById(999L)).thenReturn(null);

            AlumniSaveRequest req = createRequest("2024001", "张三", 999L);
            BizException ex = assertThrows(BizException.class, () -> alumniService.create(req));
            assertTrue(ex.getMessage().contains("学院"));
        }

        @Test
        @DisplayName("创建时设置默认值")
        void create_setsDefaults() {
            when(alumniMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            SysCollege college = new SysCollege(); college.setId(1L);
            when(collegeMapper.selectById(1L)).thenReturn(college);

            alumniService.create(createRequest("2024001", "张三", 1L));

            verify(alumniMapper).insert(argThat(a ->
                    a.getStatus() != null && a.getStatus() == 1
                    && a.getFaceStatus() != null && a.getFaceStatus() == 0));
        }
    }

    // ── 更新 ──

    @Nested
    @DisplayName("更新校友")
    class Update {

        @Test
        @DisplayName("正常更新成功")
        void update_success() {
            Alumni existing = createAlumni(1L, "2024001", "张三", 1L);
            when(alumniMapper.selectById(1L)).thenReturn(existing);
            when(alumniMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            SysCollege college = new SysCollege(); college.setId(1L);
            when(collegeMapper.selectById(1L)).thenReturn(college);

            AlumniSaveRequest req = createRequest("2024002", "张三改", 1L);
            AlumniVO vo = alumniService.update(1L, req);

            verify(alumniMapper).updateById(any(Alumni.class));
        }

        @Test
        @DisplayName("学号重复（排除自身）抛出异常")
        void duplicateStudentNoExcludingSelf_throws() {
            Alumni existing = createAlumni(1L, "2024001", "张三", 1L);
            when(alumniMapper.selectById(1L)).thenReturn(existing);
            when(alumniMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            AlumniSaveRequest req = createRequest("2024002", "张三改", 1L);
            BizException ex = assertThrows(BizException.class, () -> alumniService.update(1L, req));
            assertTrue(ex.getMessage().contains("学号"));
        }

        @Test
        @DisplayName("校友不存在抛出异常")
        void alumniNotFound_throws() {
            when(alumniMapper.selectById(999L)).thenReturn(null);

            assertThrows(BizException.class,
                    () -> alumniService.update(999L, createRequest("x", "x", 1L)));
        }
    }

    // ── 删除 ──

    @Nested
    @DisplayName("删除校友")
    class Delete {

        @Test
        @DisplayName("正常删除成功")
        void delete_success() {
            when(alumniMapper.selectById(1L)).thenReturn(createAlumni(1L, "x", "x", 1L));

            assertDoesNotThrow(() -> alumniService.delete(1L));
            verify(alumniMapper).deleteById(1L);
        }

        @Test
        @DisplayName("删除不存在的校友抛出异常")
        void delete_notFound_throws() {
            when(alumniMapper.selectById(999L)).thenReturn(null);

            assertThrows(BizException.class, () -> alumniService.delete(999L));
        }
    }

    // ── Excel 导入 ──

    @Nested
    @DisplayName("Excel 导入")
    class ExcelImport {

        @Test
        @DisplayName("模板生成返回非空字节")
        void generateTemplate_returnsNonEmptyBytes() {
            byte[] result = alumniService.generateTemplate();
            assertNotNull(result);
            assertTrue(result.length > 0);
        }

        @Test
        @DisplayName("有效行全部导入成功")
        void validRows_allSucceed() throws IOException {
            byte[] excelBytes = buildTestExcel(new String[][]{
                    {"2024001", "张三", "男", "CS", "2020", "2024", "在校生", "ACM金牌"},
                    {"2024002", "李四", "女", "CS", "2020", "2024", "校友", "优秀毕业生"},
            });
            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

            when(collegeMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(college("CS", 1L)));
            when(alumniMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            AlumniService.ImportResult result = alumniService.importExcel(file);
            assertEquals(2, result.total());
            assertEquals(2, result.success());
            assertEquals(0, result.fail());
        }

        @Test
        @DisplayName("学号重复所在行记录失败")
        void duplicateStudentNo_partialFail() throws IOException {
            byte[] excelBytes = buildTestExcel(new String[][]{
                    {"2024001", "张三", "男", "CS", "", "", "", ""},
            });
            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

            when(collegeMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(college("CS", 1L)));
            when(alumniMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            AlumniService.ImportResult result = alumniService.importExcel(file);
            assertEquals(1, result.total());
            assertEquals(0, result.success());
            assertEquals(1, result.fail());
        }

        @Test
        @DisplayName("未知学院编码记录失败")
        void unknownCollegeCode_partialFail() throws IOException {
            byte[] excelBytes = buildTestExcel(new String[][]{
                    {"2024001", "张三", "男", "UNKNOWN", "", "", "", ""},
            });
            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

            when(collegeMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(college("CS", 1L)));
            when(alumniMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            AlumniService.ImportResult result = alumniService.importExcel(file);
            assertEquals(1, result.total());
            assertEquals(0, result.success());
            assertEquals(1, result.fail());
        }

        @Test
        @DisplayName("空行被跳过")
        void emptyRowsSkipped() throws IOException {
            byte[] excelBytes = buildTestExcel(new String[][]{
                    {"", "", "", "", "", "", "", ""},
                    {"2024001", "张三", "男", "CS", "", "", "", ""},
            });
            MockMultipartFile file = new MockMultipartFile("file", "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

            when(collegeMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(college("CS", 1L)));
            when(alumniMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            AlumniService.ImportResult result = alumniService.importExcel(file);
            assertEquals(1, result.total());
            assertEquals(1, result.success());
        }

        private byte[] buildTestExcel(String[][] rows) throws IOException {
            try (Workbook wb = new XSSFWorkbook()) {
                Sheet sheet = wb.createSheet();
                // 表头
                Row header = sheet.createRow(0);
                String[] titles = {"学号*", "姓名*", "性别(男/女)", "学院编码*", "入学年份", "毕业年份", "身份(在校生/校友)", "简介"};
                for (int i = 0; i < titles.length; i++) {
                    header.createCell(i).setCellValue(titles[i]);
                }
                // 数据行
                for (int r = 0; r < rows.length; r++) {
                    Row row = sheet.createRow(r + 1);
                    for (int c = 0; c < rows[r].length; c++) {
                        row.createCell(c).setCellValue(rows[r][c]);
                    }
                }
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                wb.write(bos);
                return bos.toByteArray();
            }
        }

        private SysCollege college(String code, Long id) {
            SysCollege c = new SysCollege();
            c.setCode(code);
            c.setId(id);
            c.setName("测试学院");
            c.setStatus(1);
            return c;
        }
    }
}
