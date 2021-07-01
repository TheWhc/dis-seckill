package com.seckill.dis.mq.receiver;

import java.io.IOException;

import com.rabbitmq.client.Channel;
import com.seckill.dis.common.api.cache.RedisServiceApi;
import com.seckill.dis.common.api.cache.vo.GoodsKeyPrefix;
import com.seckill.dis.common.api.cache.vo.OrderKeyPrefix;
import com.seckill.dis.common.api.goods.GoodsServiceApi;
import com.seckill.dis.common.api.goods.vo.GoodsVo;
import com.seckill.dis.common.api.mq.vo.SkMessage;
import com.seckill.dis.common.api.order.OrderServiceApi;
import com.seckill.dis.common.api.seckill.SeckillServiceApi;
import com.seckill.dis.common.api.user.vo.UserVo;
import com.seckill.dis.common.domain.SeckillOrder;
import com.seckill.dis.mq.config.MQConfig;
import org.apache.dubbo.config.annotation.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * MQ消息接收者, 消费者
 * 消费者绑定在队列监听，既可以接收到队列中的消息
 *
 * @author mata
 */
@Service
public class MqConsumer {

    private static Logger logger = LoggerFactory.getLogger(MqConsumer.class);

    @Reference(interfaceClass = GoodsServiceApi.class)
    GoodsServiceApi goodsService;

    @Reference(interfaceClass = SeckillServiceApi.class)
    SeckillServiceApi seckillService;

    @Reference(interfaceClass = RedisServiceApi.class)
    RedisServiceApi redisService;

    @Reference(interfaceClass = OrderServiceApi.class)
    OrderServiceApi orderService;

    /**
     * 处理收到的秒杀成功信息（核心业务实现）
     *
     * @param message
     */
    @RabbitListener(queues = MQConfig.SECKILL_QUEUE)
    @RabbitHandler
    public void receiveSkInfo(SkMessage message, Channel channel, Message mes) throws IOException {
        logger.info("MQ receive a message: " + message);
        // 获取秒杀用户信息与商品id
        Long userId = message.getUserID();
        long goodsId = message.getGoodsId();

        // 获取商品的库存
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        Integer stockCount = goods.getStockCount();
        if(stockCount <= 0) {
            return;
        }

        // 判断是否已经秒杀到了(保证秒杀接口幂等性)
        SeckillOrder order = this.getSkOrderByUserIdAndGoodsId(userId, goodsId);
        if (order != null) {
            return;
        }

        try {
            // 1.减库存 2.写入订单 3.写入秒杀订单
            seckillService.seckill(userId, goodsId);
            channel.basicAck(mes.getMessageProperties().getDeliveryTag(), false);
        } 
        catch (Exception e) {
            if(redisService.get(OrderKeyPrefix.SK_ORDER, ":" + userId + "_" + goodsId, SeckillOrder.class)!=null){
                logger.debug("消息已重复处理,拒绝再次接收...");
                channel.basicReject(mes.getMessageProperties().getDeliveryTag(), false); // 拒绝消息
            } 
            else{
                if (mes.getMessageProperties().getRedelivered()) {
                    channel.basicReject(mes.getMessageProperties().getDeliveryTag(), false);
                } else{
                    logger.error("消息即将再次返回队列处理...");
                    channel.basicNack(mes.getMessageProperties().getDeliveryTag(), false, true); 
                }
            } 
        }

    }


    /**
     * 通过用户id与商品id从订单列表中获取订单信息，这个地方用了唯一索引（unique index!!!!!）
     * <p>
     * 优化，不同每次都去数据库中读取秒杀订单信息，而是在第一次生成秒杀订单成功后，
     * 将订单存储在redis中，再次读取订单信息的时候就直接从redis中读取
     *
     * @param userId
     * @param goodsId
     * @return 秒杀订单信息
     */
    private SeckillOrder getSkOrderByUserIdAndGoodsId(Long userId, long goodsId) {

        // 从redis中取缓存，减少数据库的访问
        SeckillOrder seckillOrder = redisService.get(OrderKeyPrefix.SK_ORDER, ":" + userId + "_" + goodsId, SeckillOrder.class);
        if (seckillOrder != null) {
            return seckillOrder;
        }
        return orderService.getSeckillOrderByUserIdAndGoodsId(userId, goodsId);
    }
}

