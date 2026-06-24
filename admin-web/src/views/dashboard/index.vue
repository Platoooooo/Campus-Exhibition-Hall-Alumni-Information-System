<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDashboardStats } from '@/api/dashboard'

// ---- 数据状态 ----
const loading = ref(false)
const stats = ref({
  totalAlumni: 0,
  pendingReview: 0,
  published: 0,
  newThisMonth: 0,
  categoryDistribution: [],
  auditFlow: [],
  todoList: []
})

// ---- 图表容器 ----
const pieRef = ref(null)
const barRef = ref(null)
let pieChart = null
let barChart = null

// ---- 统计卡配置 ----
const statCards = [
  { key: 'totalAlumni', label: '校友总数', icon: 'User', color: '#2B5AED', bg: '#EDF1FD' },
  { key: 'pendingReview', label: '待我审核', icon: 'Clock', color: '#F5A623', bg: '#FEF7E8' },
  { key: 'published', label: '已上架', icon: 'CircleCheck', color: '#36C5A6', bg: '#E8FAF5' },
  { key: 'newThisMonth', label: '本月新增', icon: 'TrendCharts', color: '#E5484D', bg: '#FDECEE' }
]

// 状态标签映射
const statusLabels = {
  draft: '草稿',
  pending_college: '待学院审核',
  pending_academic: '待教务处审核',
  approved: '已入库',
  rejected: '已驳回',
  published: '已上架',
  unpublished: '已下架'
}

// ---- 加载数据 ----
async function loadStats() {
  loading.value = true
  try {
    stats.value = await getDashboardStats()
    await nextTick()
    renderPie()
    renderBar()
  } finally {
    loading.value = false
  }
}

// ---- ECharts 饼图：分类占比 ----
function renderPie() {
  if (!pieRef.value) return
  if (!pieChart) {
    pieChart = echarts.init(pieRef.value)
  }
  const data = stats.value.categoryDistribution || []
  pieChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      textStyle: { color: '#86909C', fontSize: 12 }
    },
    series: [{
      type: 'pie',
      radius: ['55%', '80%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 6,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold' }
      },
      data: data.map(d => ({ name: d.name, value: d.value }))
    }],
    color: ['#2B5AED', '#36C5A6', '#F5A623', '#E5484D', '#8B5CF6', '#06B6D4', '#F97316']
  })
}

// ---- ECharts 柱状图：审核流转（近12个月） ----
function renderBar() {
  if (!barRef.value) return
  if (!barChart) {
    barChart = echarts.init(barRef.value)
  }
  const flow = stats.value.auditFlow || []
  barChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      data: ['待学院审', '待教务处审', '已通过/上架', '已驳回'],
      top: 0,
      textStyle: { color: '#86909C', fontSize: 12 }
    },
    grid: { left: 10, right: 10, bottom: 0, top: 36, containLabel: true },
    xAxis: {
      type: 'category',
      data: flow.map(f => f.month.substring(5)), // 只显示 MM
      axisLine: { lineStyle: { color: '#E5E6EB' } },
      axisLabel: { color: '#86909C', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#F2F3F5' } },
      axisLabel: { color: '#86909C', fontSize: 11 }
    },
    series: [
      {
        name: '待学院审',
        type: 'bar',
        stack: 'audit',
        data: flow.map(f => f.pendingCollege),
        itemStyle: { color: '#F5A623', borderRadius: [0, 0, 0, 0] },
        barWidth: 20
      },
      {
        name: '待教务处审',
        type: 'bar',
        stack: 'audit',
        data: flow.map(f => f.pendingAcademic),
        itemStyle: { color: '#8B5CF6' }
      },
      {
        name: '已通过/上架',
        type: 'bar',
        stack: 'audit',
        data: flow.map(f => f.approved),
        itemStyle: { color: '#36C5A6' }
      },
      {
        name: '已驳回',
        type: 'bar',
        stack: 'audit',
        data: flow.map(f => f.rejected),
        itemStyle: { color: '#E5484D', borderRadius: [0, 0, 0, 0] }
      }
    ]
  })
}

// ---- 窗口大小变化时重绘 ----
function handleResize() {
  pieChart?.resize()
  barChart?.resize()
}

onMounted(() => {
  loadStats()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  pieChart?.dispose()
  barChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="dashboard">
    <h2 class="page-title">工作台</h2>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="card in statCards" :key="card.key">
        <div class="stat-card" v-loading="loading">
          <div class="stat-icon" :style="{ color: card.color, background: card.bg }">
            <el-icon :size="22"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-value">{{ (stats[card.key] ?? 0).toLocaleString() }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="10">
        <div class="chart-card">
          <h3 class="chart-title">资料分类占比</h3>
          <div ref="pieRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :span="14">
        <div class="chart-card">
          <h3 class="chart-title">审核流转（近12个月）</h3>
          <div ref="barRef" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 待办列表 -->
    <div class="chart-card" style="margin-top:16px">
      <h3 class="chart-title">待办列表</h3>
      <el-table :data="stats.todoList" v-loading="loading" empty-text="暂无待办" style="width:100%">
        <el-table-column prop="title" label="档案标题" min-width="180" show-overflow-tooltip sortable />
        <el-table-column prop="categoryName" label="分类" width="110" sortable />
        <el-table-column prop="collegeName" label="学院" width="130" sortable />
        <el-table-column prop="status" label="环节" width="130" sortable>
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'pending_academic' ? '' : 'warning'">
              {{ statusLabels[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="170" sortable />
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.dashboard { padding: 0; }

.page-title {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 20px;
  color: #1D2129;
  margin: 0 0 20px;
}

/* ---- 统计卡片 ---- */
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  transition: box-shadow 0.2s;
}

.stat-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
}

.stat-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  flex-shrink: 0;
}

.stat-value {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 28px;
  color: #1D2129;
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: #86909C;
  margin-top: 6px;
}

/* ---- 图表卡片 ---- */
.chart-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

.chart-title {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 15px;
  color: #1D2129;
  margin: 0 0 12px;
}

.chart-box {
  width: 100%;
  height: 280px;
}
</style>
