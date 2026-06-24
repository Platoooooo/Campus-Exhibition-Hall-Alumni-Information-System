package com.campus.exhibition.common;

import com.campus.exhibition.entity.SysOperLog;
import com.campus.exhibition.mapper.SysOperLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;

/**
 * 操作日志切面 —— 拦截 @OperLog 注解的方法，异步写入 sys_oper_log
 * 密码和人脸数据不记录
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private final SysOperLogMapper operLogMapper;
    private final ObjectMapper objectMapper;

    /** 需要过滤的参数名 */
    private static final Set<String> FILTER_KEYS = Set.of(
            "password", "newPassword", "oldPassword",
            "image", "file", "feature", "faceData", "base64"
    );

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();

        SysOperLog logEntry = new SysOperLog();
        logEntry.setDescription(operLog.value());
        logEntry.setStatus(0); // 默认成功

        // 请求信息
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                logEntry.setMethod(req.getMethod());
                logEntry.setUri(req.getRequestURI());
                logEntry.setIp(getClientIp(req));
            }
        } catch (Exception e) { /* 非 Web 上下文忽略 */ }

        // 当前用户
        try {
            logEntry.setUserId(UserContext.userId());
            logEntry.setUsername(
                    UserContext.currentUser() != null ? UserContext.currentUser().getUsername() : null);
        } catch (Exception e) { /* 未登录忽略 */ }

        // 请求参数（过滤敏感字段）
        try {
            Object[] args = joinPoint.getArgs();
            List<Object> safeArgs = new ArrayList<>();
            for (Object arg : args) {
                if (arg instanceof HttpServletRequest || arg instanceof jakarta.servlet.http.HttpServletResponse) {
                    continue;
                }
                if (arg instanceof org.springframework.web.multipart.MultipartFile) {
                    safeArgs.add("[文件上传]");
                    continue;
                }
                safeArgs.add(arg);
            }
            String paramsJson = objectMapper.writeValueAsString(safeArgs);
            // 截断过长参数
            if (paramsJson.length() > 2000) {
                paramsJson = paramsJson.substring(0, 2000) + "...(truncated)";
            }
            // 过滤敏感字段：用占位符替换
            for (String key : FILTER_KEYS) {
                paramsJson = paramsJson.replaceAll(
                        "\"" + key + "\"\\s*:\\s*\"[^\"]*\"",
                        "\"" + key + "\":\"***\"");
            }
            logEntry.setParams(paramsJson);
        } catch (Exception e) {
            logEntry.setParams("[序列化失败]");
        }

        // 执行目标方法
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            logEntry.setStatus(1);
            logEntry.setErrorMsg(t.getMessage() != null
                    ? t.getMessage().substring(0, Math.min(t.getMessage().length(), 500))
                    : t.getClass().getSimpleName());
            // 写入日志后重新抛出
            logEntry.setCostMs(System.currentTimeMillis() - start);
            operLogMapper.insert(logEntry);
            throw t;
        }

        logEntry.setCostMs(System.currentTimeMillis() - start);
        operLogMapper.insert(logEntry);
        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
