# 分布式高并发商品秒杀系统设计

[TOC]

# 一、介绍

本项目为之前写的一个单体高并发商品秒杀的分布式改进版本。

商品秒杀与其它业务最大的区别在于：

- 低廉价格
- 大幅推广
- 瞬时售空
- 一般是定时上架
- 时间短、瞬时并发量高、网络的流量也会瞬间变大

除了具有以上特点，秒杀商品还需要完成正常的电子商务逻辑。

本项目引入诸多优化手段，使系统可以从容应对秒杀场景下的业务处理。

本项目是对单体项目进行了分布式改造，对职责进行了划分，降低了单体应用的业务耦合性。

# 二、快速启动

- 开发工具

  |        🏖         |       🌁       |      🏖      |        🎯         |        🦄         |              |
  | :--------------: | :-----------: | :---------: | :--------------: | :--------------: | :----------: |
  |     Java 1.8     | Mysql 5.7.20  | Redis 2.8.9 | Zookeeper 3.4.12 |  Rabbitmq 3.7.7  |              |
  |        🧐         |       🥇       |      🌈      |        🍻         |        📮         |      🚏       |
  | SpringBoot 2.1.5 | MyBatis 2.0.1 | Dubbo 2.7.1 |  Nginx   1.20.1  | Thymeleaf 3.0.11 | jmeter 5.4.1 |

  安装好上述构建工具和开发环境。

**第一步**：完成数据库的初始化

使用`./dis-seckill-common/schema/seckill.sql`初始化数据库。

**第二步**： 将项目导入IDE中进行构建，按照以下的顺序启动服务

- 启动缓存服务
- 启动用户服务
- 启动订单服务
- 启动商品服务
- 启动消息队列服务
- 启动网关服务

> 注： 启动服务时最好按上面的顺序启动

**第三步：**访问项目入口地址

```
http://localhost:8082
```

初始用户手机号码：13428317233，密码: 00000

# 三、系统架构图

![image-20210630213816233](https://gitee.com/wu_hc/note_images/raw/master/project/seckill/20210630220403.png)

- 注册中心使用ZooKeeper
- 缓存采用redis
- 消息队列采用RabbitMQ
- 用户请求全部交由Gateway模块处理
- Gateway模块使用RPC的方式调用其它模块提供的服务完成业务处理

# 四、项目入门

## 1、模块介绍

![image-20210630203152357](https://gitee.com/wu_hc/note_images/raw/master/project/seckill/20210630220421.png)

- `dis-seckill-common`: 通用模块

- `dis-seckill-user`: 用户模块

- `dis-seckill-goods`: 商品模块

- `dis-seckill-order`: 订单模块

- `dis-seckill-gateway`: 网关模块

- `dis-seckill-cache`: 缓存模块

- `dis-seckill-mq`: 消息队列模块

   用户请求全部交由Gateway模块处理，Gateway模块使用RPC（远程过程调用）的方式调用其它模块提供的服务完全业务处理

## 2、秒杀流程图

![image-20210630214404615](https://gitee.com/wu_hc/note_images/raw/master/project/seckill/20210630220416.png)

## 3、秒杀方案介绍

![image-20210630215406470](https://gitee.com/wu_hc/note_images/raw/master/project/seckill/20210630220422.png)

# 五、TODO

- [x] [项目基本技术点]();
- [x] **[秒杀流程优化]()**；
- [x] **[接口安全优化]()**；
- [x] **[系统限流与降级服务]()**;
- [ ] **[`Nginx`水平扩展网关模块与限流配置]()**;

# 六、Q&A

| Q&A                                                          |
| ------------------------------------------------------------ |
| [前后端交互接口定义](https://github.com/TheWhc/dis-seckill/blob/master/doc/%E5%89%8D%E5%90%8E%E7%AB%AF%E4%BA%A4%E4%BA%92%E6%8E%A5%E5%8F%A3%E5%AE%9A%E4%B9%89.md) |
| [前后端交互接口逻辑实现](https://github.com/TheWhc/dis-seckill/blob/master/doc/%E5%89%8D%E5%90%8E%E7%AB%AF%E4%BA%A4%E4%BA%92%E6%8E%A5%E5%8F%A3%E9%80%BB%E8%BE%91%E5%AE%9E%E7%8E%B0.md)                                   |
| [`Redis`中存储的数据](https://github.com/TheWhc/dis-seckill/blob/master/doc/Redis%E4%B8%AD%E5%AD%98%E5%82%A8%E7%9A%84%E6%95%B0%E6%8D%AE.md)                                      |
| [使用分布式锁解决恶意用户重复注册问题](https://github.com/TheWhc/dis-seckill/blob/master/doc/%E4%BD%BF%E7%94%A8%E5%88%86%E5%B8%83%E5%BC%8F%E9%94%81%E8%A7%A3%E5%86%B3%E6%81%B6%E6%84%8F%E7%94%A8%E6%88%B7%E9%87%8D%E5%A4%8D%E6%B3%A8%E5%86%8C%E9%97%AE%E9%A2%98.md)                     |
| [拦截器`HandlerInterceptor`的使用]()                         |
| **[`Rabbitmq`如何保证消息的可靠投递（）]()**                 |
| **[`redis`和`mysql`如何实现双删一致性（）]()**               |
| **[限流的的原理和项目中使用(近期更新)]()**                   |

