# 接口契约 · 人脸识别链路（API Contract）

> 适用范围：face-service(Python/FastAPI/ArcSoft 虹软 SDK) ↔ Spring Boot ↔ screen-web。
> 约定：所有接口 Content-Type=application/json（文件上传除外）；
> 时间 ISO8601；特征为 base64 编码的二进制 blob（ArcSoft 原始特征字节）；
> 相似度 score 取值 0~1（非百分数），推荐阈值生活照 0.80 / 证件照 0.82。
> 字段名一经定义，两端必须完全一致，禁止自行改名。

---

## 第一部分：face-service 内部接口（Python，端口 8000）

### A. POST /extract  提取人脸特征

**用途**：给一张图，返回特征向量（用于录入入库，或识别时提取待比对特征）。

**请求**
```json
{
  "image": "string，必填，图像 base64（不含 data:image 前缀）",
  "needQuality": true
}
```

**响应（成功）**
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "faceFound": true,
    "faceCount": 1,
    "feature": "AQIDBAUG...base64编码的二进制特征...",
    "dim": 22020,
    "quality": 0.92,
    "modelVer": "arcsoft-v3.0"
  }
}
```

**响应（无人脸/多张脸）**
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "faceFound": false,
    "faceCount": 0,
    "feature": null,
    "dim": 0,
    "quality": null,
    "modelVer": "arcsoft-v3.0"
  }
}
```

**字段说明**

| 字段 | 类型 | 含义 |
|---|---|---|
| faceFound | bool | 是否检测到可用人脸 |
| faceCount | int | 检测到的人脸数；>1 时取最大/最清晰一张，由服务决定 |
| feature | string \| null | base64 编码的二进制特征 blob，无脸为 null |
| dim | int | 特征字节数（ArcSoft SDK 通常 ~22KB），便于校验 |
| quality | float \| null | 质量分 0~1，可用于录入门槛 |
| modelVer | string | 模型版本，存库用于后续模型升级比对 |

---

### B. POST /match  特征比对（1:N）

**用途**：识别时用。传入待识别图像 + 候选底库，返回最相似者。

**请求**
```json
{
  "image": "string，必填，待识别图像 base64",
  "threshold": 0.45,
  "candidates": [
    { "alumniId": 1001, "feature": [0.011, -0.022, "...512维..."] },
    { "alumniId": 1002, "feature": [0.033, -0.044, "...512维..."] }
  ]
}
```

> candidates 由 Spring Boot 从 face_feature 表取出后传入（服务无状态，不自管底库）。

**响应（命中）**
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "faceFound": true,
    "hit": true,
    "alumniId": 1002,
    "score": 0.87,
    "threshold": 0.45,
    "modelVer": "arcface-r100-v1"
  }
}
```

**响应（未命中：有脸但低于阈值）**
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "faceFound": true,
    "hit": false,
    "alumniId": null,
    "score": 0.31,
    "threshold": 0.45,
    "modelVer": "arcface-r100-v1"
  }
}
```

**响应（无脸）**
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "faceFound": false,
    "hit": false,
    "alumniId": null,
    "score": null,
    "threshold": 0.45,
    "modelVer": "arcface-r100-v1"
  }
}
```

| 字段 | 类型 | 含义 |
|---|---|---|
| hit | bool | score≥threshold 且有脸 才为 true |
| alumniId | long \| null | 命中者；未命中为 null |
| score | float \| null | 最高相似度 0~1 |
| threshold | float | 本次使用的阈值（回显，便于排障） |

**face-service 错误码**

| code | 含义 |
|---|---|
| 0 | 成功（业务结果看 data） |
| 4001 | 图像解码失败 / base64 非法 |
| 4002 | candidates 为空或维度不匹配 |
| 5000 | 推理内部异常 |

---

## 第二部分：Spring Boot 对外接口（Java，端口 8080）

> 统一返回体 R<T> { code, message, data }；code=200 成功。
> /api/face/enroll 需 admin 鉴权；/api/face/recognize 为大屏匿名只读。

### C. POST /api/face/enroll  录入/更新校友人脸

**鉴权**：Bearer Token（admin 角色）
**Content-Type**：multipart/form-data

**请求参数**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| alumniId | long | 是 | 目标校友 |
| file | file | 是 | 人脸图片（jpg/png，单张清晰正脸） |

**处理流程**：转 base64 → 调 face-service `/extract` → 校验 faceFound 且 quality≥配置门槛 → 特征加密后写 face_feature → 更新 alumni.face_status=1。

**响应（成功）**
```json
{
  "code": 200,
  "message": "录入成功",
  "data": {
    "alumniId": 1002,
    "quality": 0.92,
    "modelVer": "arcface-r100-v1",
    "faceStatus": 1
  }
}
```

**响应（失败示例）**
```json
{ "code": 5101, "message": "未检测到清晰人脸，请重新采集", "data": null }
```

| code | 含义 |
|---|---|
| 200 | 录入成功 |
| 5101 | 未检测到人脸 |
| 5102 | 人脸质量不达标（低于门槛） |
| 5103 | 检测到多张人脸，请单人采集 |
| 5104 | face-service 不可用 |

---

### D. POST /api/face/recognize  大屏识别并定位

**鉴权**：匿名（大屏一体机）
**Content-Type**：application/json

**请求**
```json
{
  "image": "string，必填，摄像头抓拍帧 base64",
  "device": "hall-01"
}
```

**处理流程**：从 face_feature 取全部有效特征作为 candidates → 调 face-service `/match` → 写 face_recog_log → 命中则附带该校友专属轮播数据。

**响应（命中）**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "status": "HIT",
    "alumniId": 1002,
    "score": 0.87,
    "alumni": {
      "id": 1002,
      "name": "张三",
      "avatar": "https://.../avatar.jpg",
      "collegeName": "计算机学院",
      "gradYear": 2018,
      "summary": "ACM 全国金牌"
    },
    "timeline": [
      {
        "archiveId": 3001,
        "categoryName": "荣誉",
        "title": "全国大学生程序设计竞赛金奖",
        "eventDate": "2017-11-20",
        "content": "……",
        "media": [
          { "type": 1, "url": "https://.../a.jpg", "thumbnail": "https://.../a_thumb.jpg" },
          { "type": 2, "url": "https://.../b.mp4", "thumbnail": "https://.../b_cover.jpg", "duration": 86 }
        ]
      }
    ]
  }
}
```

**响应（未命中 / 无脸 → 降级）**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "status": "NO_MATCH",
    "alumniId": null,
    "score": 0.31,
    "alumni": null,
    "timeline": []
  }
}
```

**响应（人脸服务不可用 → 降级，仍返回 200 不让大屏报错）**
```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "status": "DEGRADED",
    "alumniId": null,
    "score": null,
    "alumni": null,
    "timeline": []
  }
}
```

**status 枚举（大屏据此切换行为）**

| status | 大屏行为 |
|---|---|
| HIT | 播"欢迎，XX"过场 → 切入 timeline 专属轮播 |
| NO_MATCH | 温和提示，数秒后回默认轮播 |
| DEGRADED | 静默回默认轮播（不打扰观众） |

**media.type 取值**：1=图片 2=视频 3=文档（与数据库 archive_media.type 对齐）

---

## 第三部分：字段对齐速查（防止跑偏）

| 概念 | face-service | Spring Boot | 数据库 |
|---|---|---|---|
| 特征向量 | feature | feature(byte[]加密) | face_feature.feature |
| 相似度 | score (0~1) | score | face_recog_log.score |
| 命中标记 | hit | status=HIT | face_recog_log.hit |
| 校友ID | alumniId | alumniId | alumni.id |
| 模型版本 | modelVer | modelVer | face_feature.model_ver |
| 媒体类型 | - | type(1/2/3) | archive_media.type |

---

## 第四部分：联调测试用例（验收依据）

1. 正脸清晰 → enroll 成功，face_feature 有加密记录，face_status=1。
2. 用同人另一张照片 recognize → status=HIT 且 alumniId 一致。
3. 用陌生人照片 recognize → status=NO_MATCH。
4. 传一张无人脸图 → /extract faceFound=false；recognize 返回 NO_MATCH。
5. 停掉 face-service 再 recognize → status=DEGRADED，大屏不报错、回默认轮播。
6. score 在 0~1 之间（确认两端没有按百分数处理）。
7. timeline 按 eventDate 升序，且只含 published 档案。