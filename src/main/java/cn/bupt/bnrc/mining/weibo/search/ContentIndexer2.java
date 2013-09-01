package cn.bupt.bnrc.mining.weibo.search;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.bupt.bnrc.mining.weibo.classify.Emoticons;
import cn.bupt.bnrc.mining.weibo.db.StatusesContentDao;
import cn.bupt.bnrc.mining.weibo.util.Utils;

@Service
public class ContentIndexer2 {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private StatusesContentDao statusesContentDao;

	private static boolean create = false;

	public static Directory dir = null;
	public static Analyzer analyzer = null;
	public static IndexWriterConfig iwc = null;
	public static IndexWriter indexWriter = null;

	public ContentIndexer2() {
		try {
			dir = FSDirectory.open(new File(Constants.INDEX));
			analyzer = new StandardAnalyzer(Version.LUCENE_40);
			iwc = new IndexWriterConfig(Version.LUCENE_40, analyzer);
			if (create) {
				// Create a new index in the directory, removing any previously
				// indexed documents:
				iwc.setOpenMode(OpenMode.CREATE);
			} else {
				// Add new documents to an existing index:
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			}
			indexWriter = new IndexWriter(dir, iwc);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** Index all text files under a directory. */
	public static void main(String[] args) {

	}

	public void indexStatus() {
		Date lastTime = new Date();

		List<Map<String, Object>> topNStatuses = null;
		Map<String, Object> topNParams = new HashMap<String, Object>();
		int pageSize = 100000;
		topNParams.put("pageSize", pageSize);
		do{
			logger.info("get indexing top {} statuses", pageSize);
			topNStatuses = statusesContentDao.getTopNStatusContent(topNParams);
			for (Map<String, Object> status : topNStatuses){
				String content = (String)status.get("content");
				this.indexString(indexWriter, content);
			}
			statusesContentDao.deleteTopNStatusContent(topNParams);
			logger.info("delete top {} statuses", pageSize);
			Date currentTime = new Date();
			logger.info("indexStatus -- time: {} total milliseconds, indexedStatus size: {}",
					currentTime.getTime() - lastTime.getTime(), topNStatuses.size());
			lastTime = currentTime;
		}while (topNStatuses.size() == pageSize);
		try {
			indexWriter.commit();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void indexStatusesContainEmoticons(){
		Date lastTime = new Date();

		List<Map<String, Object>> topNStatuses = null;
		Map<String, Object> topNParams = new HashMap<String, Object>();
		int pageSize = 100000;
		topNParams.put("pageSize", pageSize);
		do{
			logger.info("get indexing top {} statuses", pageSize);
			topNStatuses = statusesContentDao.getTopNStatusContent(topNParams);
			for (Map<String, Object> status : topNStatuses){
				String content = (String)status.get("content");
				this.indexStringContainEmoticons(indexWriter, content);
			}
			statusesContentDao.deleteTopNStatusContent(topNParams);
			logger.info("delete top {} statuses", pageSize);
			Date currentTime = new Date();
			logger.info("indexStatus -- time: {} total milliseconds, indexedStatus size: {}",
					currentTime.getTime() - lastTime.getTime(), topNStatuses.size());
			lastTime = currentTime;
		}while (topNStatuses.size() == pageSize);
		try {
			indexWriter.commit();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void statisticEmoticons(){
		Date lastTime = new Date();
		
		Pattern p = Pattern.compile(Utils.emoticonRegexStr);
		HashMap<String, Integer> emoticons = new HashMap<String, Integer>();

		List<Map<String, Object>> topNStatuses = null;
		HashMap<String, Object> topNParams = new HashMap<String, Object>();
		int pageSize = 100000;
		topNParams.put("pageSize", pageSize);
		do{
			logger.info("get indexing top {} statuses", pageSize);
			topNStatuses = statusesContentDao.getTopNStatusContent(topNParams);
			for (Map<String, Object> status : topNStatuses){
				String content = (String)status.get("content");
				
				Matcher m = p.matcher(content);			
				while (m.find()){
					String emoticon = m.group(1);
					Integer time = 1;
					if (emoticons.containsKey(emoticon)){
						time = emoticons.get(emoticon) + 1;
					}
					emoticons.put(emoticon, time);
					logger.info(content);
				}
			}
			statusesContentDao.deleteTopNStatusContent(topNParams);
			logger.info("delete top {} statuses", pageSize);
			Date currentTime = new Date();
			logger.info("indexStatus -- time: {} total milliseconds, indexedStatus size: {}",
					currentTime.getTime() - lastTime.getTime(), topNStatuses.size());
			lastTime = currentTime;
		}while (topNStatuses.size() == pageSize);
		try {
			indexWriter.commit();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Utils.hashMapToDisk(emoticons, "emoticons");
	}

	public void indexStringList(IndexWriter writer,
			List<Map<String, Object>> statuses) {
		if (statuses == null) {
			return;
		}
		for (Map<String, Object> status : statuses) {
			String statusContent = (String) status.get("content");
			this.indexString(indexWriter, statusContent);
		}
	}
	
	public void indexStringContainEmoticons(IndexWriter writer, String content){
		if (Utils.isContainEmoticons(content, Emoticons.emoticons)){
			try {
				Document doc = new Document();
				if (content != null) {
					Field contentField = new TextField(Constants.FILED_NAME,
							content, Field.Store.YES);
					doc.add(contentField);
					writer.addDocument(doc);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void indexString(IndexWriter writer, String content) {
		try {
			Document doc = new Document();
			content = doFilter(content);
			if (content != null) {
				Field contentField = new TextField(Constants.FILED_NAME,
						content, Field.Store.YES);
				doc.add(contentField);
				writer.addDocument(doc);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String doFilter(String content) {
		content = content.replaceAll("[^a-zA-Z0-9\u4E00-\u9FA5]", " ")
				.replaceAll(" +", " ").trim();
		return content;
	}
}
