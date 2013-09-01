package cn.bupt.bnrc.mining.weibo.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.map.ObjectMapper;

public class StringBuilderUtil {

	public static ObjectMapper objectMapper = new ObjectMapper();
	
	public static String buildJsonString(Object obj){
		String result = null;
		try {
			result = objectMapper.writeValueAsString(obj);
		} catch(Exception e){
			e.printStackTrace();
		}
		return result;
	}
	
	public static String buildJsonString(Map<String,Object> map){
		String result = null;
		try {
			result = objectMapper.writeValueAsString(map);
		} catch(Exception e){
			e.printStackTrace();
		}
		return result;
	}
	
	public static String buildRequestBodyString(HttpServletRequest request){
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
			String line = null;
			StringBuilder sb = new StringBuilder();
			while ((line = reader.readLine()) != null) sb.append(line);
			return sb.toString();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return "";
	}
}
