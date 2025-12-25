<template>
  <view class="list-container">
    <view class="search-bar">
      <input 
        class="search-input" 
        placeholder="搜索内容"
        placeholder-class="placeholder"
      />
    </view>
    
    <view class="list-content">
      <view 
        v-for="item in dataList" 
        :key="item.id"
        class="list-item"
        @click="goToDetail(item.id)"
      >
        <view class="item-left">
          <view class="item-avatar" :style="{ background: item.color }">
            {{ item.name.charAt(0) }}
          </view>
        </view>
        <view class="item-content">
          <view class="item-title">{{ item.name }}</view>
          <view class="item-desc">{{ item.desc }}</view>
          <view class="item-time">{{ item.time }}</view>
        </view>
        <view class="item-arrow">›</view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface ListItem {
  id: number
  name: string
  desc: string
  time: string
  color: string
}

const dataList = ref<ListItem[]>([
  { id: 1, name: '项目A', desc: '这是项目A的简要描述信息', time: '2小时前', color: '#667eea' },
  { id: 2, name: '项目B', desc: '这是项目B的简要描述信息', time: '5小时前', color: '#f093fb' },
  { id: 3, name: '项目C', desc: '这是项目C的简要描述信息', time: '1天前', color: '#4facfe' },
  { id: 4, name: '项目D', desc: '这是项目D的简要描述信息', time: '2天前', color: '#43e97b' },
  { id: 5, name: '项目E', desc: '这是项目E的简要描述信息', time: '3天前', color: '#fa709a' },
])

const goToDetail = (id: number) => {
  uni.navigateTo({
    url: `/pages/detail/detail?id=${id}`
  })
}
</script>

<style scoped>
.list-container {
  min-height: 100vh;
  background: #f5f5f5;
}

.search-bar {
  background: #ffffff;
  padding: 20rpx 30rpx;
  margin-bottom: 20rpx;
}

.search-input {
  height: 70rpx;
  background: #f5f5f5;
  border-radius: 35rpx;
  padding: 0 30rpx;
  font-size: 28rpx;
}

.placeholder {
  color: #999;
}

.list-content {
  padding: 0 30rpx;
}

.list-item {
  background: #ffffff;
  border-radius: 15rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  display: flex;
  align-items: center;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.item-left {
  margin-right: 20rpx;
}

.item-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 32rpx;
  font-weight: bold;
}

.item-content {
  flex: 1;
}

.item-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 10rpx;
}

.item-desc {
  font-size: 26rpx;
  color: #666;
  margin-bottom: 10rpx;
}

.item-time {
  font-size: 24rpx;
  color: #999;
}

.item-arrow {
  font-size: 50rpx;
  color: #ccc;
  margin-left: 20rpx;
}
</style>
