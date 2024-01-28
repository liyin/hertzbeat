package org.dromara.hertzbeat.manager.scheduler.netty;

import com.alibaba.fastjson.JSON;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hertzbeat.alert.dao.AlertDao;
import org.dromara.hertzbeat.common.entity.alerter.Alert;
import org.dromara.hertzbeat.common.entity.manager.Monitor;
import org.dromara.hertzbeat.common.entity.push.PushMetricsDto;
import org.dromara.hertzbeat.common.entity.warehouse.History;
import org.dromara.hertzbeat.common.queue.CommonDataQueue;
import org.dromara.hertzbeat.common.util.JsonUtil;
import org.dromara.hertzbeat.manager.dao.MonitorDao;
import org.dromara.hertzbeat.warehouse.config.WarehouseProperties;
import org.dromara.hertzbeat.warehouse.dao.HistoryDao;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;

import javax.persistence.Tuple;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhaoqiaobo
 * @date 2022/6/22 17:56
 */
@Slf4j
@Component
public class BootNettyUdpSimpleChannelInboundHandler
		extends SimpleChannelInboundHandler<DatagramPacket> {

	private final HistoryDao historyDao;
	private final AlertDao alertDao;
	private final MonitorDao monitorDao;
	private final CommonDataQueue dataQueue;

	private static final int STRING_MAX_LENGTH = 1024;
	private static final String TABLE = "table";

	private Map<Long, Long> lastAlert;  //key: alertid value: time

	public BootNettyUdpSimpleChannelInboundHandler(HistoryDao historyDao,AlertDao alertDao,MonitorDao monitorDao, CommonDataQueue dataQueue) {
		this.historyDao = historyDao;
		this.alertDao = alertDao;
		this.monitorDao = monitorDao;
		this.dataQueue = dataQueue;
		lastAlert = new ConcurrentHashMap<>(100);

		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				try{
					Long currentTime = System.currentTimeMillis();
					lastAlert.keySet().removeIf(key -> {
						Long keyTime = lastAlert.get(key);

						if (currentTime >= keyTime.longValue() + 2000) {
							lastAlert.remove(key);
							return true;
						}
						return false;
					});
				}catch (Exception e) {
					log.error("periodical deletion failed. {}", e.getMessage());
				}
			}
		}, 1000, 5000);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
		//传两次，拿一次缓存，时间一样就不存
		try {
			String strdata = msg.content().toString(CharsetUtil.UTF_8);
			//打印收到的消息
			log.info("---------------------receive data--------------------------");
			log.info("报文：{}", msg.toString());
			log.info(strdata);
			//解析出来存入

			Map<String, Object> payload = toMap(strdata);
			Map source = (Map) payload.get("source");
			if (Objects.nonNull(source.get(TABLE))) {
				String table = JSON.parseObject(JSON.toJSONString(source.get(TABLE)), String.class);
				switch (table){
					case "hzb_history":
						hzb_history(payload);
						break;
					case "hzb_alert":
						hzb_alert(payload);
						break;
					case "hzb_monitor":
						hzb_monitor(payload);
						break;
					default:
						break;
				}
			}
			log.info("---------------------receive data--------------------------");
			//收到udp消息后，可通过此方式原路返回的方式返回消息，例如返回时间戳
			ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer("ok", CharsetUtil.UTF_8), msg.sender()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Map<String, Object> toMap(String payload){
		TypeReference<Map<String, Object>> type = new TypeReference<Map<String, Object>>(){};
		Map<String, Object> jsonMap = JsonUtil.fromJson(payload, type);
		return jsonMap;
	}

	public static Map<String, Map> toMap2(String payload){
		TypeReference<Map<String, Map>> type = new TypeReference<Map<String, Map>>(){};
		Map<String, Map> jsonMap = JsonUtil.fromJson(payload, type);
		return jsonMap;
	}

	/**
	 *服务端发两次过来，但hibernate做完一次后，发现有，就不做了。hibernate缓存机制
	 */
	public boolean hzb_history(Map<String, Object> payload) {
		String op = JSON.parseObject(JSON.toJSONString(payload.get("op")), String.class);
		if (Objects.nonNull(op)) {
			switch (op) {
				case "r": //全量
					return true;
				case "c":
					return historyDao.save(JSON.parseObject(JSON.toJSONString(payload.get("after")), History.class))!=null;
				case "u":
					return historyDao.save(JSON.parseObject(JSON.toJSONString(payload.get("after")), History.class))!=null;
				case "d":
					//同步删除的数据对于性能而言无意义
					//historyDao.delete(JSON.parseObject(JSON.toJSONString(payload.get("before")), History.class));
					return true;
				default:
					return false;
			}
		} else {
			return false;
		}
	}

	public boolean hzb_alert(Map<String, Object> payload) throws JsonProcessingException {
		String op = JSON.parseObject(JSON.toJSONString(payload.get("op")), String.class);
		Map after = JSON.parseObject(JSON.toJSONString(payload.get("after")), Map.class);
		Alert parse = null;
		if (Objects.nonNull(op)) {
			switch (op) {
				case "r": //全量
					return true;
				case "c":
					parse = parse(after);
					if(parse!=null) dataQueue.sendAlertsData(parse);
					return true;
				case "u":
					parse = parse(after);
					if(parse!=null) dataQueue.sendAlertsData(parse);
					return true;
				case "d":
					alertDao.delete(JSON.parseObject(JSON.toJSONString(payload.get("before")), Alert.class));
					return true;
				default:
					return false;
			}
		} else {
			return false;
		}
	}

	public boolean hzb_monitor(Map<String, Object> payload) {
		String op = JSON.parseObject(JSON.toJSONString(payload.get("op")), String.class);
		if (Objects.nonNull(op)) {
			switch (op) {
				case "r": //全量
					return true;
				case "c":
					return monitorDao.save(JSON.parseObject(JSON.toJSONString(payload.get("after")), Monitor.class))!=null;
				case "u":
					return monitorDao.save(JSON.parseObject(JSON.toJSONString(payload.get("after")), Monitor.class))!=null;
				case "d":
					//同步删除的数据对于性能而言无意义
					monitorDao.delete(JSON.parseObject(JSON.toJSONString(payload.get("before")), Monitor.class));
					return true;
				default:
					return false;
			}
		} else {
			return false;
		}
	}

	private Alert parse(Map after){
		String tags =(String) after.remove("tags");
		log.warn(JSON.toJSONString(after));
		Alert alert = JSON.parseObject(JSON.toJSONString(after), Alert.class);
		Map tagsMap = JSON.parseObject(tags,Map.class);
		alert.setTags(tagsMap);
		if (!lastAlert.containsKey(alert.getId()) ||  System.currentTimeMillis() > lastAlert.get(alert.getId()) + 1000L) {
			lastAlert.put(alert.getId(), System.currentTimeMillis());
			return alert;
		}
		return  null;
	}
}