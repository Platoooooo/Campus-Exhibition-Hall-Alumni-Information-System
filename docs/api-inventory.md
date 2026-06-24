# API 接口清单

> 最后更新：2026-06-23
> 覆盖全部 Controller 接口，含鉴权、路径、说明。

## 1. 认证 (AuthController)

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| POST | /api/auth/login | 匿名 | 登录 {username, password} → {token, user} |

## 2. 工作台 (DashboardController)

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | /api/dashboard/stats | admin/academic/college | 聚合统计：4 卡片 + 分类饼图 + 审核柱图 + 待办 |

## 3. 校友管理 (AlumniController)

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | /api/alumni | admin/academic/college | 分页查询（name/studentNo/collegeId/gradYear/identity/status） |
| GET | /api/alumni/{id} | admin/academic/college | 详情 |
| POST | /api/alumni | admin/academic/college | 新增 |
| PUT | /api/alumni/{id} | admin/academic/college | 编辑 |
| DELETE | /api/alumni/{id} | admin | 删除 |
| GET | /api/alumni/template | admin/academic | Excel 模板下载 |
| POST | /api/alumni/import | admin/academic | Excel 批量导入 |

## 4. 资料档案 (ArchiveController)

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | /api/archive | admin/academic/college | 分页（title/categoryId/status/collegeId） |
| GET | /api/archive/{id} | admin/academic/college | 详情 + mediaList |
| POST | /api/archive | admin/academic/college | 新增 |
| PUT | /api/archive/{id} | admin/academic/college | 编辑 |
| DELETE | /api/archive/{id} | admin/academic | 删除 |
| POST | /api/archive/{id}/media | admin/academic/college | 上传媒体 |
| DELETE | /api/archive/{aid}/media/{mid} | admin/academic/college | 删除媒体 |
| PUT | /api/archive/{id}/media/sort | admin/academic/college | 媒体排序 |

## 5. 审核中心 (AuditController)

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| PUT | /api/audit/{id}/submit | admin/academic/college | 提交审核 |
| GET | /api/audit/college/todo | college | 学院待办 |
| GET | /api/audit/academic/todo | academic | 教务处待办 |
| PUT | /api/audit/{id}/college/approve | college | 学院通过 |
| PUT | /api/audit/{id}/college/reject | college | 学院驳回（opinion 必填） |
| PUT | /api/audit/{id}/academic/approve | academic | 教务处通过 |
| PUT | /api/audit/{id}/academic/reject | academic | 教务处驳回（opinion 必填） |
| GET | /api/audit/{id}/logs | admin/academic/college | 审核记录时间线 |

## 6. 资料库运营 (OperationController)

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| PUT | /api/operation/archive/{id}/publish | admin | 上架 |
| PUT | /api/operation/archive/{id}/unpublish | admin | 下架 |
| PUT | /api/operation/archive/{id}/top | admin | 置顶 {value:0\|1} |
| PUT | /api/operation/archive/{id}/recommend | admin | 推荐 {value:0\|1} |
| PUT | /api/operation/archive/{id}/sort | admin | 排序 {value} |
| GET | /api/operation/carousel | admin | 轮播方案列表 |
| POST | /api/operation/carousel | admin | 新建方案 |
| PUT | /api/operation/carousel/{id} | admin | 编辑方案 |
| DELETE | /api/operation/carousel/{id} | admin | 删除方案 |
| GET | /api/operation/carousel/{id} | admin | 方案详情 + items |
| POST | /api/operation/carousel/{id}/item | admin | 添加轮播项 |
| DELETE | /api/operation/carousel/{cid}/item/{iid} | admin | 移除轮播项 |
| PUT | /api/operation/carousel/{id}/items/sort | admin | 轮播项排序 |

## 7. 大屏公开 (ScreenController)

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | /api/screen/carousel | 匿名 | 默认轮播方案 |
| GET | /api/screen/wall | 匿名 | 校友墙分页 |
| GET | /api/screen/alumni/{id} | 匿名 | 成长轨迹 |
| GET | /api/screen/search | 匿名 | 搜索 |

## 8. 人脸识别 (FaceController)

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| POST | /api/face/enroll | admin | 录入人脸 (multipart) |
| POST | /api/face/recognize | 匿名 | 大屏识别 → HIT/NO_MATCH/DEGRADED |

## 9. 媒体 (MediaController)

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| POST | /api/media/upload | admin/academic/college | 上传文件 |
| GET | /api/media/{id} | admin/academic/college | 媒体详情 |
| GET | /api/public/media/{id} | 匿名 | 公开访问 |
| GET | /api/public/thumb/{id} | 匿名 | 缩略图 |

## 10. 字典 (College / Major / Category)

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | /api/college/all | admin/academic/college | 学院列表 |
| GET | /api/college | admin | 学院分页 |
| POST/PUT | /api/college[/{id}] | admin | 学院 CRUD |
| PUT | /api/college/{id}/toggle | admin | 启停 |
| GET | /api/major/by-college | admin/academic/college | 按学院查专业 |
| GET | /api/major | admin | 专业分页 |
| POST/PUT | /api/major[/{id}] | admin | 专业 CRUD |
| PUT | /api/major/{id}/toggle | admin | 启停 |
| GET | /api/category/all | admin/academic/college | 资料分类列表 |

## 11. 系统管理 (SysUser / Log)

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | /api/user | admin | 用户分页 |
| POST | /api/user | admin | 新增用户 |
| PUT | /api/user/{id} | admin | 编辑用户 |
| DELETE | /api/user/{id} | admin | 删除用户 |
| PUT | /api/user/{id}/reset-password | admin | 重置密码 |
| PUT | /api/user/{id}/toggle | admin | 启停用户 |
| GET | /api/log/oper | admin | 操作日志 |
| GET | /api/log/login | admin | 登录日志 |

## 角色缩写速查

| 角色 | 含义 | 数据范围 |
|---|---|---|
| admin | 校级管理员 | 全校 |
| academic | 教务处管理员 | 全校 |
| college | 学院管理员 | 仅本学院 |

## 统一返回体

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

| code | 含义 |
|---|---|
| 200 | 成功 |
| 400 | 参数/业务校验错误 |
| 401 | Token 无效/过期 |
| 403 | 越权 |
| 404 | 资源不存在 |
| 500 | 服务内部错误 |

## 状态机（资料档案 ArchiveStatus）

```
draft → pending_college → pending_academic → approved → published ⇄ unpublished
任意环节 → rejected → draft（退回修改）
```
