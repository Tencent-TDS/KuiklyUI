<template>
  <view class="button-demo-container">
    <view class="page-title">Button 组件演示</view>
    
    <!-- 按钮类型 -->
    <view class="section">
      <view class="section-title">按钮类型 (type)</view>
      <view class="button-group">
        <button type="primary">主要按钮 (primary)</button>
        <button type="default">默认按钮 (default)</button>
        <button type="warn">警告按钮 (warn)</button>
      </view>
    </view>

    <!-- 按钮大小 -->
    <view class="section">
      <view class="section-title">按钮大小 (size)</view>
      <view class="button-group">
        <button type="primary" size="default">默认大小</button>
        <button type="primary" size="mini">迷你按钮</button>
      </view>
    </view>

    <!-- 镂空按钮 -->
    <view class="section">
      <view class="section-title">镂空按钮 (plain)</view>
      <view class="button-group">
        <button type="primary" plain>镂空主要按钮</button>
        <button type="default" plain>镂空默认按钮</button>
        <button type="warn" plain>镂空警告按钮</button>
      </view>
    </view>

    <!-- 禁用状态 -->
    <view class="section">
      <view class="section-title">禁用状态 (disabled)</view>
      <view class="button-group">
        <button type="primary" disabled>禁用主要按钮</button>
        <button type="default" disabled>禁用默认按钮</button>
        <button type="warn" disabled>禁用警告按钮</button>
      </view>
    </view>

    <!-- 加载状态 -->
    <view class="section">
      <view class="section-title">加载状态 (loading)</view>
      <view class="button-group">
        <button type="primary" :loading="isLoading" @click="toggleLoading">
          {{ isLoading ? '加载中...' : '点击加载' }}
        </button>
        <button type="default" loading>持续加载</button>
      </view>
    </view>

    <!-- 自定义样式 -->
    <view class="section">
      <view class="section-title">自定义样式</view>
      <view class="button-group">
        <button class="custom-button-1">渐变按钮</button>
        <button class="custom-button-2">圆角按钮</button>
        <button class="custom-button-3">阴影按钮</button>
      </view>
    </view>

    <!-- 按钮组合 -->
    <view class="section">
      <view class="section-title">按钮组合</view>
      <view class="button-row">
        <button type="primary" size="mini">确定</button>
        <button type="default" size="mini">取消</button>
      </view>
    </view>

    <!-- 开放能力 -->
    <view class="section">
      <view class="section-title">开放能力 (open-type)</view>
      <view class="button-group">
        <button type="primary" open-type="share">分享给好友</button>
        <button type="primary" open-type="getUserInfo" @getuserinfo="onGetUserInfo">
          获取用户信息
        </button>
        <button type="primary" open-type="contact">联系客服</button>
        <button type="primary" open-type="getPhoneNumber" @getphonenumber="onGetPhoneNumber">
          获取手机号
        </button>
      </view>
    </view>

    <!-- 表单按钮 -->
    <view class="section">
      <view class="section-title">表单按钮 (form-type)</view>
      <form @submit="onFormSubmit" @reset="onFormReset">
        <view class="form-group">
          <input 
            class="form-input" 
            v-model="formData.username" 
            placeholder="请输入用户名"
          />
          <input 
            class="form-input" 
            v-model="formData.password" 
            type="password"
            placeholder="请输入密码"
          />
        </view>
        <view class="button-group">
          <button type="primary" form-type="submit">提交表单</button>
          <button type="default" form-type="reset">重置表单</button>
        </view>
      </form>
    </view>

    <!-- 事件处理 -->
    <view class="section">
      <view class="section-title">事件处理</view>
      <view class="button-group">
        <button type="primary" @click="handleClick">点击事件</button>
        <button type="primary" @longpress="handleLongPress">长按事件</button>
        <button type="primary" @touchstart="handleTouchStart" @touchend="handleTouchEnd">
          触摸事件
        </button>
      </view>
      <view class="event-log" v-if="eventLog">
        <text>{{ eventLog }}</text>
      </view>
    </view>

    <!-- 计数器示例 -->
    <view class="section">
      <view class="section-title">实际应用示例 - 计数器</view>
      <view class="counter-container">
        <button 
          type="default" 
          size="mini" 
          @click="decreaseCount"
          :disabled="count <= 0"
        >
          -
        </button>
        <view class="counter-value">{{ count }}</view>
        <button 
          type="default" 
          size="mini" 
          @click="increaseCount"
          :disabled="count >= 10"
        >
          +
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'

// 加载状态
const isLoading = ref(false)

// 表单数据
const formData = ref({
  username: '',
  password: ''
})

// 事件日志
const eventLog = ref('')

// 计数器
const count = ref(0)

// 切换加载状态
const toggleLoading = () => {
  isLoading.value = true
  setTimeout(() => {
    isLoading.value = false
    uni.showToast({
      title: '加载完成',
      icon: 'success'
    })
  }, 2000)
}

// 获取用户信息
const onGetUserInfo = (e: any) => {
  console.log('获取用户信息：', e)
  if (e.detail.userInfo) {
    uni.showToast({
      title: '获取成功',
      icon: 'success'
    })
  } else {
    uni.showToast({
      title: '用户拒绝授权',
      icon: 'none'
    })
  }
}

// 获取手机号
const onGetPhoneNumber = (e: any) => {
  console.log('获取手机号：', e)
  if (e.detail.code) {
    uni.showToast({
      title: '获取成功',
      icon: 'success'
    })
  } else {
    uni.showToast({
      title: '用户拒绝授权',
      icon: 'none'
    })
  }
}

// 表单提交
const onFormSubmit = () => {
  if (!formData.value.username || !formData.value.password) {
    uni.showToast({
      title: '请填写完整信息',
      icon: 'none'
    })
    return
  }
  uni.showToast({
    title: '提交成功',
    icon: 'success'
  })
  console.log('表单数据：', formData.value)
}

// 表单重置
const onFormReset = () => {
  formData.value = {
    username: '',
    password: ''
  }
  uni.showToast({
    title: '表单已重置',
    icon: 'none'
  })
}

// 点击事件
const handleClick = () => {
  eventLog.value = '触发了点击事件 ' + new Date().toLocaleTimeString()
}

// 长按事件
const handleLongPress = () => {
  eventLog.value = '触发了长按事件 ' + new Date().toLocaleTimeString()
  uni.showToast({
    title: '长按事件触发',
    icon: 'none'
  })
}

// 触摸开始
const handleTouchStart = () => {
  eventLog.value = '触摸开始 ' + new Date().toLocaleTimeString()
}

// 触摸结束
const handleTouchEnd = () => {
  eventLog.value = '触摸结束 ' + new Date().toLocaleTimeString()
}

// 增加计数
const increaseCount = () => {
  if (count.value < 10) {
    count.value++
  }
}

// 减少计数
const decreaseCount = () => {
  if (count.value > 0) {
    count.value--
  }
}
</script>

<style scoped>
.button-demo-container {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 30rpx;
}

.page-title {
  font-size: 40rpx;
  font-weight: bold;
  color: #333;
  text-align: center;
  margin-bottom: 40rpx;
  padding: 30rpx 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 20rpx;
}

.section {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  margin-bottom: 30rpx;
  padding-bottom: 20rpx;
  border-bottom: 2rpx solid #f0f0f0;
}

.button-group {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.button-group button {
  margin: 0;
}

.button-row {
  display: flex;
  gap: 20rpx;
  justify-content: center;
}

.button-row button {
  flex: 1;
  margin: 0;
}

/* 自定义按钮样式 */
.custom-button-1 {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 50rpx;
}

.custom-button-2 {
  background: #4facfe;
  color: #fff;
  border: none;
  border-radius: 100rpx;
}

.custom-button-3 {
  background: #43e97b;
  color: #fff;
  border: none;
  box-shadow: 0 8rpx 20rpx rgba(67, 233, 123, 0.4);
}

/* 表单样式 */
.form-group {
  margin-bottom: 30rpx;
}

.form-input {
  width: 100%;
  height: 80rpx;
  padding: 0 20rpx;
  margin-bottom: 20rpx;
  border: 2rpx solid #e0e0e0;
  border-radius: 10rpx;
  font-size: 28rpx;
  box-sizing: border-box;
}

/* 事件日志 */
.event-log {
  margin-top: 20rpx;
  padding: 20rpx;
  background: #f0f0f0;
  border-radius: 10rpx;
  font-size: 24rpx;
  color: #666;
}

/* 计数器样式 */
.counter-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 30rpx;
}

.counter-value {
  font-size: 48rpx;
  font-weight: bold;
  color: #667eea;
  min-width: 100rpx;
  text-align: center;
}

.counter-container button {
  width: 80rpx;
  height: 80rpx;
  line-height: 80rpx;
  padding: 0;
  margin: 0;
  font-size: 36rpx;
  font-weight: bold;
}
</style>
