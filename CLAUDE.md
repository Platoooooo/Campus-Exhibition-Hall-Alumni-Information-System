# CLAUDE.md

校园展览馆校友资料系统。三端：管理后台、大屏展示端、人脸识别服务。

## 技术栈
- 后端：Spring Boot 3.x · JDK 17 · MyBatis-Plus · Spring Security(JWT) · MySQL 8.0
- 后台前端：Vue3 + Vite + Pinia + Vue Router + Element Plus + Axios
- 大屏前端：Vue3 + Vite + Pinia（独立工程，轻量，无重型组件库）
- 人脸服务：Python + FastAPI + Arcface（独立进程，HTTP 调用）

## 目录结构
```
/server          Spring Boot 后端
/admin-web       管理后台 (Vue3)
/screen-web      大屏展示端 (Vue3)
/face-service    Arcface 推理服务 (Python)
/docs            需求/页面/数据库设计文档
```

## 模块约定
- 后端分层：controller / service / mapper / entity / dto / vo / config / common
- 统一返回体 `R<T> { code, message, data }`；分页 `PageResult<T>`
- 异常用全局 `@RestControllerAdvice`；禁止裸吞异常
- 业务状态枚举集中在 `enums` 包，禁止魔法字符串

## 核心状态机（资料档案）
draft → pending_college → pending_academic → approved → published ⇄ unpublished
任意审核环节可 → rejected（必带意见，可退回修改）

## 数据权限
- 学院管理员：仅本学院数据
- 教务处/校级管理员：全校
- 大屏端 API：只读，仅返回 status=published

## 人脸识别约定
- Spring Boot 不直接做算法，调用 `/face-service`
- 仅存特征向量（加密），可配置不留原图
- 大屏识别失败一律降级为默认轮播，绝不空屏

## 字体（前端 assets/fonts）
- HarmonyOS_SansSC_Bold/Regular：中文标题/正文
- din-bold-2：大屏数字
- Rajdhani-SemiBold：大屏英文/标签

## 编码规范
- Java：构造注入，DTO/VO 与 Entity 分离，Service 写接口+impl
- Vue：组合式 API + `<script setup>`，请求统一封装 `api/` 层
- 命名：后端驼峰，数据库 snake_case，URL kebab-case
- 提交前确保编译通过；新增接口同步更新 `/docs`

## 启动
- server: `mvn spring-boot:run`（8080）
- admin-web: `npm run dev`（5173）
- screen-web: `npm run dev`（5174）
- face-service: `uvicorn main:app --port 8000`

## 不要做
- 不在 Controller 写业务逻辑
- 不在前端硬编码后端地址（走 .env）
- 不向大屏暴露未发布数据
- 不在日志打印人脸原始数据/密码