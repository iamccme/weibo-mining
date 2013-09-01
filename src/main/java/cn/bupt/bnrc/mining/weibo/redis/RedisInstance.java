package cn.bupt.bnrc.mining.weibo.redis;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import redis.clients.jedis.Jedis;
import cn.bupt.bnrc.mining.weibo.util.Utils;
import cn.bupt.bnrc.mining.weibo.weka.Constants;

public class RedisInstance {

	private static RedisInstance redisInstance = null;
	private static Jedis jedis = null;
	private RedisInstance(){}
	
	public static RedisInstance getRedisInstance(){
		if (redisInstance == null){
			jedis = new Jedis("59.64.156.84", 6379);
			jedis.select(4);
			redisInstance = new RedisInstance();
		}
		
		return redisInstance;
	}
	
	public void getAllKeysWithDatabase(){
		Set<String> keys = jedis.keys("*");
		for (String key : keys){
			jedis.hlen(key);
			System.out.println(key);
		}
	}
	
	public void getKeys() throws IOException{
		String key = Constants.wordCountInTotalFileName;
		Set<String> fields = jedis.hkeys(key);
		HashMap<String, Double> result = new HashMap<String, Double>();
		for (String field : fields){
			result.put(field, new Double(jedis.hget(key, field)));
		}
		File file = new File(key);
		file.createNewFile();
		Utils.hashMapToDisk(result, file);
	}
	
	public static void release(){
		jedis.disconnect();
	}
	
	public static void main(String[] args) throws Exception{
		RedisInstance instance = RedisInstance.getRedisInstance();
		instance.getKeys();
		RedisInstance.release();
	}
}
