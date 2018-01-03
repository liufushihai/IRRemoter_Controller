# 基于FireBLE的空调遥控器(Android上位机)

介绍
---------------------

该空调遥控器是通过Android手机BLE模块发送相应空调格式与频率的红外编码数据给FireBLE单片机，再由FireBLE单片机上的红外发射模块将接收数据发送给空调的红外接收模块，从而来实现对空调的控制。

架构图如下: 
![architecture](https://github.com/liufushihai/IRRemoter_Controller/blob/master/IRRemoter_Controller_architecture/architecture-zh.png)

功能
---------------------
1. Android上位机App通过BLE与FireBLE硬件模块进行连接。
2. 实现空调的常规控制，包括温度、风速、制冷等模式的控制。
3. 兼容市面主流空调的控制。

实物
--------------------

![IRRemoter_Controller](http://oypvhzll7.bkt.clouddn.com/%E7%A9%BA%E8%B0%83%E9%81%A5%E6%8E%A7%E5%99%A82.jpg)