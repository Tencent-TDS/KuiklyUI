<template>
  <view class="home-container">
    <view class="header">
      <view class="welcome">欢迎回来！</view>
      <view class="user-info">用户中心</view>
    </view>
    
    <view class="menu-grid">
      <view 
        v-for="item in menuList" 
        :key="item.id"
        class="menu-item"
        @click="navigateTo(item.path)"
      >
        <view class="menu-icon" :style="{ background: item.color }">
          <text class="icon-text">{{ item.icon }}</text>
        </view>
        <view class="menu-name">{{ item.name }}</view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface MenuItem {
  id: number
  name: string
  icon: string
  path: string
  color: string
}

const menuList = ref<MenuItem[]>([
  { id: 1, name: '列表页', icon: '📋', path: '/pages/list/list', color: '#667eea' },
  { id: 2, name: '我的收藏', icon: '⭐', path: '', color: '#f093fb' },
  { id: 3, name: '消息中心', icon: '💬', path: '', color: '#4facfe' },
  { id: 4, name: '设置', icon: '⚙️', path: '/pages/settings/index', color: '#43e97b' },
  { id: 4, name: '路由', icon: '⚙️', path: '/pages/router/index', color: '#43e97b' },
  { id: 4, name: '按钮', icon: '⚙️', path: '/pages/button-demo/button-demo', color: '#43e97b' },
])

const navigateTo = (path: string) => {
  if (!path) {
    uni.showToast({
      title: '功能开发中',
      icon: 'none'
    })
    return
  }
  uni.navigateTo({
    url: path
  })
}
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 40rpx;
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20rpx;
  padding: 40rpx;
  margin-bottom: 40rpx;
  color: #ffffff;
}

.welcome {
  font-size: 36rpx;
  font-weight: bold;
  margin-bottom: 10rpx;
}

.user-info {
  font-size: 28rpx;
  opacity: 0.9;
}

.menu-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 30rpx;
}

.menu-item {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.menu-icon {
  width: 100rpx;
  height: 100rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20rpx;
}

.icon-text {
  font-size: 50rpx;
}

.menu-name {
  font-size: 28rpx;
  color: #333;
  font-weight: 500;
}
</style>
