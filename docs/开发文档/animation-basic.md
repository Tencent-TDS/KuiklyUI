# 动画基础

Kuikly动画有声明式和命令式两种使用方法，本章节简要介绍Kuikly动画的基础概念，后续章节将对动画的两种使用方式进行详细的介绍。

## 动画定义

Kuikly的动画，描述的是视图从一个状态到另外一个属性状态的过渡过程。也就是说，我们需要通过设置属性描述一个视图在状态A时的UI效果，以及状态B时的UI效果。这样，当状态A变成状态B时，Kuikly框架就自动把A到B之间的UI变化，用动画的过程表现出来。但是光有2个状态的UI效果是不够的，通常我们还希望能够控制动画的过程，比如说动画时长，时间曲线等。这些都是由`Animation`对象来控制的，所以说`Animation`是对动画过程的一个抽象。

## 动画类型

Animation类默认提供了以下动画曲线类型
  * Animation.linear: 线性曲线
  * Animation.easeIn: 先慢后快曲线
  * Animation.easeOut: 先快后慢曲线
  * Animation.easeInOut: 开始和结尾慢, 中间快曲线
  * Animation.springLinear: 弹簧式线性曲线
  * Animation.springEaseIn: 弹簧式先慢后快曲线
  * Animation.springEaseOut: 弹簧式先快后慢曲线
  * Animation.springEaseInOut: 弹簧式开始和结尾慢, 中间快曲线

## 可动画属性

在``Kuikly``支持对以下属性做动画

1. transform动画: 可以对组件进行**位移**、**旋转**和**缩放**动画
2. opacity动画: 可以对组件的**透明度**进行动画
3. backgroundColor动画: 可以对组件的**背景颜色**进行动画
4. frame动画: 可以对组件的**位置和大小**进行动画
