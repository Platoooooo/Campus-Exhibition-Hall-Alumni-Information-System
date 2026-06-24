"""
校园展览馆 · 人脸识别服务（ArcSoft 虹软 SDK）
端口 8000

接口：
  POST /extract       — 提取人脸特征（返回 float 数组，与 Java DTO 对齐）
  POST /match         — 特征比对（1:N）
  GET  /health        — 健康检查

依赖：libarcsoft_face.dll（Windows x64），通过 ctypes 调用
"""

import base64
import ctypes
import os
import struct
from ctypes import (
    c_ubyte, c_int32, c_long, c_float, c_char_p,
    c_void_p, POINTER, byref, Structure, sizeof, pointer, cast
)
from typing import List, Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

# ============================================================
# SDK 配置（环境变量）
# ============================================================
APP_ID = os.getenv("ARCSOFT_APP_ID", "")
SDK_KEY = os.getenv("ARCSOFT_SDK_KEY", "")
DLL_PATH = os.getenv("ARCSOFT_DLL_PATH", r".\x64\Realease\libarcsoft_face_engine.dll")
# 推荐阈值：生活照 0.80，证件照 0.82
DEFAULT_THRESHOLD = float(os.getenv("FACE_THRESHOLD", "0.80"))

# -- 将 DLL 所在目录加入 Windows DLL 搜索路径，确保依赖 DLL（OpenCV 等）可被找到 --
_DLL_DIR = os.path.abspath(os.path.dirname(DLL_PATH))
if os.path.isdir(_DLL_DIR) and hasattr(os, "add_dll_directory"):
    os.add_dll_directory(_DLL_DIR)
# 同时加到 PATH（兼容旧版 Python / 子进程）
os.environ["PATH"] = _DLL_DIR + os.pathsep + os.environ.get("PATH", "")

# ============================================================
# ctypes 类型映射（参考 amcomdef.h）
# ============================================================
MRESULT = c_long
MHandle = c_void_p
MInt32 = c_int32
MFloat = c_float
MByte = c_ubyte
MPChar = c_char_p
MOK = 0  # MERR_NONE

# 颜色格式 ASVL_PAF_RGB24_B8G8R8 = 0x201
ASVL_PAF_RGB24_B8G8R8 = 0x201

# 检测模式
ASF_DETECT_MODE_IMAGE = 0xFFFFFFFF
# 人脸识别功能掩码：人脸检测 + 特征提取
ASF_FACE_DETECT = 0x00000001
ASF_FACERECOGNITION = 0x00000004
COMBINED_MASK = ASF_FACE_DETECT | ASF_FACERECOGNITION
# 优先级：全角度
ASF_OP_ALL_OUT = 0x5
# 比对模型：生活照
ASF_LIFE_PHOTO = 0x1

# ============================================================
# C 结构体定义
# ============================================================

class MRECT(Structure):
    _fields_ = [
        ("left",   c_int32),
        ("top",    c_int32),
        ("right",  c_int32),
        ("bottom", c_int32),
    ]


class ASF_SingleFaceInfo(Structure):
    _fields_ = [
        ("faceRect",   MRECT),
        ("faceOrient", c_int32),
    ]


class ASF_FaceFeature(Structure):
    _fields_ = [
        ("feature",     POINTER(c_ubyte)),
        ("featureSize", c_int32),
    ]


class ASF_MultiFaceInfo(Structure):
    _fields_ = [
        ("faceRect",   POINTER(MRECT)),
        ("faceOrient", POINTER(c_int32)),
        ("faceNum",    c_int32),
        ("faceID",     POINTER(c_int32)),
    ]


# ============================================================
# 加载 DLL
# ============================================================
_arc_dll = None
_h_engine = None


def get_dll():
    global _arc_dll, _h_engine
    if _arc_dll is not None:
        return _arc_dll

    if not os.path.exists(DLL_PATH):
        raise RuntimeError(f"SDK DLL 未找到: {DLL_PATH}")

    # 先加载主 DLL（48MB，含 AI 模型数据），引擎 DLL 依赖它
    _main_dll_path = os.path.join(os.path.dirname(DLL_PATH), "libarcsoft_face.dll")
    if os.path.exists(_main_dll_path):
        ctypes.WinDLL(_main_dll_path)

    dll = ctypes.WinDLL(DLL_PATH)

    # ---- 激活 ----
    dll.ASFActivation.argtypes = [MPChar, MPChar]
    dll.ASFActivation.restype = MRESULT

    # ---- 初始化引擎 ----
    dll.ASFInitEngine.argtypes = [
        c_int32, c_int32, c_int32, c_int32, c_int32, POINTER(MHandle)]
    dll.ASFInitEngine.restype = MRESULT

    # ---- 人脸检测 ----
    dll.ASFDetectFaces.argtypes = [
        MHandle, c_int32, c_int32, c_int32, POINTER(c_ubyte),
        POINTER(ASF_MultiFaceInfo), c_int32]
    dll.ASFDetectFaces.restype = MRESULT

    # ---- 特征提取 ----
    dll.ASFFaceFeatureExtract.argtypes = [
        MHandle, c_int32, c_int32, c_int32, POINTER(c_ubyte),
        POINTER(ASF_SingleFaceInfo), POINTER(ASF_FaceFeature)]
    dll.ASFFaceFeatureExtract.restype = MRESULT

    # ---- 特征比对 ----
    dll.ASFFaceFeatureCompare.argtypes = [
        MHandle, POINTER(ASF_FaceFeature), POINTER(ASF_FaceFeature),
        POINTER(c_float), c_int32]
    dll.ASFFaceFeatureCompare.restype = MRESULT

    # ---- 反初始化 ----
    dll.ASFUninitEngine.argtypes = [MHandle]
    dll.ASFUninitEngine.restype = MRESULT

    # ---- 激活（v4.x 需要在线激活，APP_ID 需去掉 "AppId" 前缀） ----
    app_id_clean = APP_ID
    if app_id_clean.startswith("AppId"):
        app_id_clean = app_id_clean[5:]
    ret = dll.ASFOnlineActivation(app_id_clean.encode("utf-8"), SDK_KEY.encode("utf-8"))
    if ret not in (0, 0x16002):  # MOK or MERR_ASF_ALREADY_ACTIVATED
        raise RuntimeError(f"SDK 在线激活失败，错误码: {ret}（请检查网络、APP_ID、SDK_KEY）")

    # ---- 初始化引擎 ----
    engine = MHandle()
    ret = dll.ASFInitEngine(
        ASF_DETECT_MODE_IMAGE,  # 静态图模式
        ASF_OP_ALL_OUT,         # 全角度
        32,                     # detectFaceScaleVal（推荐 32）
        1,                      # maxFaceNum
        COMBINED_MASK,          # 人脸检测 + 特征提取
        byref(engine)
    )
    if ret != MOK:
        raise RuntimeError(f"SDK 引擎初始化失败，错误码: {ret}")

    _h_engine = engine
    _arc_dll = dll
    return dll


def get_engine() -> MHandle:
    if _h_engine is None:
        get_dll()
    return _h_engine


# ============================================================
# FastAPI
# ============================================================
app = FastAPI(
    title="Face Recognition Service",
    description="校园展览馆 · ArcSoft 虹软人脸识别服务",
    version="0.3.0",
)


# ============================================================
# 请求 / 响应模型（与 Java DTO 对齐：feature 为 List[float]）
# ============================================================

class ExtractRequest(BaseModel):
    image: str = Field(..., description="图像 base64（不含 data:image 前缀）")
    needQuality: bool = Field(True)


class CandidateItem(BaseModel):
    alumniId: int
    feature: List[float] = Field(..., description="float 数组格式的特征向量")


class MatchRequest(BaseModel):
    image: str = Field(..., description="待识别图像 base64")
    threshold: float = Field(DEFAULT_THRESHOLD, ge=0.0, le=1.0)
    candidates: List[CandidateItem] = Field(..., min_length=1)


class ExtractData(BaseModel):
    faceFound: bool
    faceCount: int
    feature: Optional[List[float]] = None   # float 数组，与 Java DTO List<Float> 对齐
    dim: int = 0                             # 特征向量维度（float 个数）
    quality: Optional[float] = None
    modelVer: str = "arcsoft-v3.0"


class ExtractResponse(BaseModel):
    code: int = 0
    message: str = "ok"
    data: ExtractData


class MatchData(BaseModel):
    faceFound: bool
    hit: bool
    alumniId: Optional[int] = None
    score: Optional[float] = None
    threshold: float
    modelVer: str = "arcsoft-v3.0"


class MatchResponse(BaseModel):
    code: int = 0
    message: str = "ok"
    data: MatchData


# ============================================================
# 特征序列化辅助（bytes ↔ List[float]，与 Java floatsToBytes/bytesToFloats 对齐）
# ============================================================

def _bytes_to_floats(data: bytes) -> List[float]:
    """将 ArcSoft 原始特征字节 → float 列表（大端序，4 字节/float）"""
    count = len(data) // 4
    return list(struct.unpack(f">{count}f", data))


def _floats_to_bytes(floats: List[float]) -> bytes:
    """float 列表 → 原始字节（大端序，与 Java floatsToBytes 一致）"""
    return struct.pack(f">{len(floats)}f", *floats)


# ============================================================
# 辅助函数
# ============================================================

def _decode_image(b64: str) -> bytes:
    """解码 base64 → 图像字节"""
    try:
        data = base64.b64decode(b64)
        if len(data) < 100:
            raise ValueError("图像数据过小")
        return data
    except Exception as e:
        raise HTTPException(status_code=400, detail={
            "code": 4001, "message": f"图像解码失败: {e}"})


def _jpg_to_bgr_raw(image_bytes: bytes):
    """
    用 Pillow 将 JPEG/PNG 解码为 BGR 裸像素数据
    - 自动纠正 EXIF 旋转方向（手机照片常见问题）
    - 大图自动缩放至 1920px（SDK 推荐尺寸，避免检测窗口不匹配）
    - 宽度填充至 4 的倍数（SDK 要求行对齐，补黑边不裁剪）
    返回 (width, height, bgr_bytes)
    """
    from PIL import Image, ImageOps
    import numpy as np
    import io

    img = Image.open(io.BytesIO(image_bytes))

    # 关键：应用 EXIF 方向（修复手机照片旋转后检测不到人脸的问题）
    img = ImageOps.exif_transpose(img)

    # 大图缩放：ArcSoft SDK 推荐长边不超过 1920px
    max_dim = 1920
    w_orig, h_orig = img.size
    if max(w_orig, h_orig) > max_dim:
        ratio = max_dim / max(w_orig, h_orig)
        new_size = (int(w_orig * ratio), int(h_orig * ratio))
        img = img.resize(new_size, Image.LANCZOS)

    if img.mode != 'RGB':
        img = img.convert('RGB')

    # RGB → BGR（ArcSoft SDK 期望 B8G8R8 格式）
    arr = np.array(img)
    bgr = arr[:, :, ::-1]
    h, w = bgr.shape[:2]

    # SDK 要求每行 4 字节对齐：填充右边而非裁剪（避免切掉人脸）
    if w % 4 != 0:
        new_w = ((w + 3) // 4) * 4  # 向上取整到 4 的倍数
        padded = np.zeros((h, new_w, 3), dtype=bgr.dtype)
        padded[:, :w, :] = bgr
        bgr = padded
        w = new_w

    return w, h, bgr.tobytes()


def _detect_faces(image_bytes: bytes):
    """检测人脸，返回 (face_count, multi_face_info)"""
    dll = get_dll()
    engine = get_engine()

    w, h, bgr_data = _jpg_to_bgr_raw(image_bytes)

    face_info = ASF_MultiFaceInfo()
    # 预分配检测结果缓冲区
    rects = (MRECT * 1)()
    orients = (c_int32 * 1)()
    face_ids = (c_int32 * 1)()
    face_info.faceRect = cast(rects, POINTER(MRECT))
    face_info.faceOrient = cast(orients, POINTER(c_int32))
    face_info.faceID = cast(face_ids, POINTER(c_int32))
    face_info.faceNum = 0

    ret = dll.ASFDetectFaces(
        engine,
        w, h,
        ASVL_PAF_RGB24_B8G8R8,
        (c_ubyte * len(bgr_data)).from_buffer_copy(bgr_data),
        byref(face_info),
        0x1  # ASF_DETECT_MODEL_RGB
    )

    if ret != MOK:
        return 0, None

    return face_info.faceNum, face_info


def _extract_feature_raw(image_bytes: bytes) -> Optional[bytes]:
    """提取第一个人脸的特征 → 返回原始二进制"""
    face_count, face_info = _detect_faces(image_bytes)
    if face_count == 0 or face_info is None:
        return None

    dll = get_dll()
    engine = get_engine()

    w, h, bgr_data = _jpg_to_bgr_raw(image_bytes)

    # 构造单人脸信息
    single = ASF_SingleFaceInfo()
    single.faceRect = face_info.faceRect[0]
    single.faceOrient = face_info.faceOrient[0]

    feature = ASF_FaceFeature()
    ret = dll.ASFFaceFeatureExtract(
        engine,
        w, h,
        ASVL_PAF_RGB24_B8G8R8,
        (c_ubyte * len(bgr_data)).from_buffer_copy(bgr_data),
        byref(single),
        byref(feature)
    )

    if ret != MOK or feature.featureSize == 0:
        return None

    # 拷贝出特征数据
    feat_bytes = ctypes.string_at(feature.feature, feature.featureSize)
    return feat_bytes


def _extract_feature(image_bytes: bytes) -> Optional[List[float]]:
    """提取特征 → 返回 float 列表（对齐 Java DTO）"""
    raw = _extract_feature_raw(image_bytes)
    if raw is None:
        return None
    return _bytes_to_floats(raw)


def _compare_features(feat1: List[float], feat2: List[float]) -> float:
    """比对两个 float 列表特征 → 返回置信度 0~1"""
    dll = get_dll()
    engine = get_engine()

    raw1 = _floats_to_bytes(feat1)
    raw2 = _floats_to_bytes(feat2)

    f1 = ASF_FaceFeature()
    f1.feature = cast(
        (c_ubyte * len(raw1)).from_buffer_copy(raw1), POINTER(c_ubyte))
    f1.featureSize = len(raw1)

    f2 = ASF_FaceFeature()
    f2.feature = cast(
        (c_ubyte * len(raw2)).from_buffer_copy(raw2), POINTER(c_ubyte))
    f2.featureSize = len(raw2)

    confidence = c_float(0.0)
    ret = dll.ASFFaceFeatureCompare(
        engine, byref(f1), byref(f2), byref(confidence), ASF_LIFE_PHOTO)

    if ret != MOK:
        return 0.0

    return float(confidence.value)


# ============================================================
# 接口
# ============================================================

@app.get("/health")
async def health():
    try:
        get_dll()
        sdk_ok = True
    except Exception as e:
        sdk_ok = False

    return {
        "status": "ok" if sdk_ok else "degraded",
        "service": "face-service",
        "sdk": "arcsoft" if sdk_ok else "not_loaded",
        "modelVer": "arcsoft-v3.0",
    }


@app.post("/extract", response_model=ExtractResponse)
async def extract(req: ExtractRequest):
    image_bytes = _decode_image(req.image)
    import sys
    print(f"[extract] decoded {len(image_bytes)} bytes ({len(req.image)} b64 chars)", flush=True)

    face_count, face_info = _detect_faces(image_bytes)
    print(f"[extract] detect result: faceCount={face_count}", flush=True)

    if face_count == 0:
        return ExtractResponse(data=ExtractData(
            faceFound=False, faceCount=0, feature=None, dim=0, quality=None))

    features = _extract_feature(image_bytes)
    if features is None:
        print(f"[extract] feature extraction failed (face detected but extract returned None)", flush=True)
        return ExtractResponse(data=ExtractData(
            faceFound=False, faceCount=face_count, feature=None, dim=0,
            quality=None))

    print(f"[extract] OK: dim={len(features)}", flush=True)
    return ExtractResponse(data=ExtractData(
        faceFound=True,
        faceCount=face_count,
        feature=features,
        dim=len(features),
        quality=None,  # ArcSoft SDK free version 不返回质量分
    ))


@app.post("/match", response_model=MatchResponse)
async def match(req: MatchRequest):
    if not req.candidates:
        raise HTTPException(status_code=400, detail={
            "code": 4002, "message": "candidates 为空"})

    image_bytes = _decode_image(req.image)
    query_feat = _extract_feature(image_bytes)

    if query_feat is None:
        return MatchResponse(data=MatchData(
            faceFound=False, hit=False, alumniId=None,
            score=None, threshold=req.threshold))

    best_id = None
    best_score = 0.0
    for c in req.candidates:
        if not c.feature:
            continue
        score = _compare_features(query_feat, c.feature)
        if score > best_score:
            best_score = score
            best_id = c.alumniId

    hit = best_score >= req.threshold
    return MatchResponse(data=MatchData(
        faceFound=True,
        hit=hit,
        alumniId=best_id if hit else None,
        score=round(best_score, 6),
        threshold=req.threshold,
    ))


# ============================================================
# 直接运行
# ============================================================
if __name__ == "__main__":
    import uvicorn
    print(f"DLL: {DLL_PATH}  (exists: {os.path.exists(DLL_PATH)})")
    print(f"DLL dir added to search path: {_DLL_DIR}")
    print(f"APP_ID: {'***' + APP_ID[-4:] if APP_ID else 'NOT SET'}")
    print(f"SDK_KEY: {'***' + SDK_KEY[-4:] if SDK_KEY else 'NOT SET'}")
    uvicorn.run(app, host="0.0.0.0", port=8000)
