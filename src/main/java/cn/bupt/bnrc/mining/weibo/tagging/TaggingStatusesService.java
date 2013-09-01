package cn.bupt.bnrc.mining.weibo.tagging;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.bupt.bnrc.mining.weibo.db.TaggingStatusDao;

import com.google.common.io.Files;

@Service
public class TaggingStatusesService {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public static final int UNTAG_FLAG = 0;
	public static final int TAG_POSITIVE_FLAG = 1;
	public static final int TAG_NEGATIVE_FLAG = 2;
	public static final int TAG_NETURAL_FLAG = 3;
	
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
	
	public List<Map<String, Object>> getPagedStatuses(int pageNum){
		List<Map<String, Object>> statuses = null;
		if (pageNum == 0){
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("page_size", pageSize);
			params.put("tagging_flag", TaggingStatusesService.UNTAG_FLAG);
			statuses = taggingStatusDao.getFirstPageStatuses(params);
		}else{
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("page_offset", pageNum * pageSize);
			params.put("page_size", pageSize);
			params.put("tagging_flag", TaggingStatusesService.UNTAG_FLAG);
			statuses = taggingStatusDao.getNextPageStatuses(params);
		}
		
		return statuses;
	}
	
	public void updateStatusTagFlag(int statusId, int flag){
		if (statusId > 0){
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("id", statusId);
			params.put("tagging_flag", flag);
			taggingStatusDao.updateStatusFlag(params);
		}
	}
	
	public int getUntaggedStatusesPageCount(){
		int count = this.getUntaggedStatusesCount();
		if (count <= 0){
			return 0;
		}
		int page = (count-1)/pageSize + 1;
		
		return page;
	}
	
	public int getUntaggedStatusesCount(){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("tagging_flag", TaggingStatusesService.UNTAG_FLAG);
		return taggingStatusDao.getTotalCount(params);
	}

	private int pageSize = 100;
	
	@Autowired private TaggingStatusDao taggingStatusDao;
}
