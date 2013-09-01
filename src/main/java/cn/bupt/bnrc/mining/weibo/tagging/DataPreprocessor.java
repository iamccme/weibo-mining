package cn.bupt.bnrc.mining.weibo.tagging;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.bupt.bnrc.mining.weibo.db.TaggingStatusDao;

import com.google.common.io.Files;

@Service
public class DataPreprocessor {
	private Logger logger = LoggerFactory.getLogger(getClass());

	public void addTopicRelatedStatusesFromFile(String fileName){
		File file = new File(fileName);
		if (!file.exists()){
			logger.info(String.format("File: %s is not existed!", fileName));
			return;
		}
		try {
			String str = Files.toString(file, Charset.forName("GBK"));
			String another = new String(str.getBytes(),"utf-8");
			String[] statuses = another.split("\\$\\$\\$\\$\\$");
			
			for (String status : statuses){
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("content", status);
				params.put("tagging_flag", 0);
				taggingStatusDao.addUntaggedStatus(params);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		String filePath = DataPreprocessor.class.getClassLoader().getResource("").getPath()
				+"/data/jinzhengri.txt".substring(1);
		DataPreprocessor p = new DataPreprocessor();
		p.addTopicRelatedStatusesFromFile(filePath);
	}
	
	@Autowired private TaggingStatusDao taggingStatusDao;
}
