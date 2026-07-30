<template>
  <view class="detail-container">
    <view class="detail-header">
      <view class="detail-avatar" :style="{ background: detailData.color }">
        {{ detailData.name.charAt(0) }}
      </view>
      <view class="detail-title">{{ detailData.name }}</view>
      <view class="detail-time">{{ detailData.time }}</view>
    </view>
    
    <view class="detail-content">
      <view class="section-title">详细信息</view>
      <view class="detail-text">{{ detailData.content }}</view>
      
      <view class="section-title">项目状态</view>
      <view class="status-tag" :class="detailData.status">
        {{ detailData.statusText }}
      </view>
      
      <view class="section-title">相关数据</view>
      <view class="data-grid">
        <view class="data-item">
          <view class="data-label">浏览量</view>
          <view class="data-value">{{ detailData.views }}</view>
        </view>
        <view class="data-item">
          <view class="data-label">点赞数</view>
          <view class="data-value">{{ detailData.likes }}</view>
        </view>
      </view>
    </view>
    
    <view class="detail-footer">
      <button class="action-btn secondary" @click="handleBack">返回</button>
      <button class="action-btn primary" @click="handleAction">操作</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

interface DetailData {
  id: number
  name: string
  time: string
  color: string
  content: string
  status: string
  statusText: string
  views: number
  likes: number
}

const detailData = ref<DetailData>({
  id: 1,
  name: '项目A',
  time: '2小时前',
  color: '#667eea',
  content: '这是项目的详细介绍内容。这里可以展示更多关于项目的详细信息，包括项目背景、目标、进展情况等。可以根据实际需求添加更多内容字段。',
  status: 'active',
  statusText: '进行中',
  views: 1234,
  likes: 89
})

onLoad((options: any) => {
  const id = options.id
  // 根据id加载对应数据（这里使用模拟数据）
  const dataMap: Record<string, Partial<DetailData>> = {
    '1': { name: '项目A', color: '#667eea', statusText: '进行中', status: 'active' },
    '2': { name: '项目B', color: '#f093fb', statusText: '已完成', status: 'completed' },
    '3': { name: '项目C', color: '#4facfe', statusText: '待审核', status: 'pending' },
    '4': { name: '项目D', color: '#43e97b', statusText: '进行中', status: 'active' },
    '5': { name: '项目E', color: '#fa709a', statusText: '已完成', status: 'completed' },
  }
  
  if (id && dataMap[id]) {
    detailData.value = { ...detailData.value, ...dataMap[id] }
  }
})

const handleBack = () => {
  uni.navigateBack()
}

const handleAction = () => {
  uni.showToast({
    title: '操作成功',
    icon: 'success'
  })
}
</script>

<style scoped>
.detail-container {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 120rpx;
}

.detail-header {
  background: #ffffff;
  padding: 40rpx;
  text-align: center;
  margin-bottom: 20rpx;
}

.detail-avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  margin: 0 auto 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 50rpx;
  font-weight: bold;
}

.detail-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 10rpx;
}

.detail-time {
  font-size: 26rpx;
  color: #999;
}

.detail-content {
  background: #ffffff;
  padding: 40rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 20rpx;
  margin-top: 30rpx;
}

.section-title:first-child {
  margin-top: 0;
}

.detail-text {
  font-size: 28rpx;
  color: #666;
  line-height: 1.6;
}

.status-tag {
  display: inline-block;
  padding: 10rpx 30rpx;
  border-radius: 30rpx;
  font-size: 26rpx;
}

.status-tag.active {
  background: #e8f5e9;
  color: #43e97b;
}

.status-tag.completed {
  background: #e3f2fd;
  color: #4facfe;
}

.status-tag.pending {
  background: #fff3e0;
  color: #ff9800;
}

.data-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
}

.data-item {
  background: #f5f5f5;
  border-radius: 15rpx;
  padding: 30rpx;
  text-align: center;
}

.data-label {
  font-size: 26rpx;
  color: #999;
  margin-bottom: 10rpx;
}

.data-value {
  font-size: 36rpx;
  font-weight: bold;
  color: #333;
}

.detail-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #ffffff;
  padding: 20rpx 30rpx;
  display: flex;
  gap: 20rpx;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.action-btn {
  flex: 1;
  height: 80rpx;
  border-radius: 10rpx;
  font-size: 30rpx;
  border: none;
  line-height: 80rpx;
}

.action-btn.secondary {
  background: #f5f5f5;
  color: #666;
}

.action-btn.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
}
</style>
