<template>
  <view class="login-container">
    <view class="login-box">
      <image class="logo" src="/static/logo.png" />
      <view class="title">欢迎登录</view>
      
      <view class="form">
        <view class="input-group">
          <input 
            v-model="username" 
            class="input" 
            placeholder="请输入用户名"
            placeholder-class="placeholder"
          />
        </view>
        <view class="input-group">
          <input 
            v-model="password" 
            class="input" 
            type="password"
            placeholder="请输入密码"
            placeholder-class="placeholder"
          />
        </view>
      </view>
      
      <button class="login-btn" @click="handleLogin">登录</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'

// 自动填入演示账号密码
const username = ref('demo')
const password = ref('123456')

const handleLogin = () => {
  if (!username.value || !password.value) {
    uni.showToast({
      title: '请输入用户名和密码',
      icon: 'none'
    })
    return
  }
  
  // 模拟登录
  uni.showToast({
    title: '登录成功',
    icon: 'success'
  })
  
  setTimeout(() => {
    uni.redirectTo({
      url: '/pages/index/index'
    })
  }, 1000)
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
}

.login-box {
  width: 100%;
  background: #ffffff;
  border-radius: 20rpx;
  padding: 60rpx 40rpx;
  box-shadow: 0 10rpx 40rpx rgba(0, 0, 0, 0.1);
}

.logo {
  width: 120rpx;
  height: 120rpx;
  display: block;
  margin: 0 auto 30rpx;
}

.title {
  text-align: center;
  font-size: 40rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 60rpx;
}

.form {
  margin-bottom: 40rpx;
}

.input-group {
  margin-bottom: 30rpx;
}

.input {
  width: 100%;
  height: 88rpx;
  background: #f5f5f5;
  border-radius: 10rpx;
  padding: 0 30rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

.placeholder {
  color: #999;
}

.login-btn {
  width: 100%;
  height: 88rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  border: none;
  border-radius: 10rpx;
  font-size: 32rpx;
  font-weight: bold;
  line-height: 88rpx;
}
</style>
