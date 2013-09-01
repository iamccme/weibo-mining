package cn.bupt.bnrc.mining.weibo.search;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
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

import cn.bupt.bnrc.mining.weibo.db.StatusesDao;

@Service
public class ContentIndexer {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private StatusesDao statusesDao;
	
	public static int PAGE_SIZE = 100000;
	private static boolean create = true;
	
	public static Directory dir = null;
	public static Analyzer analyzer = null;
	public static IndexWriterConfig iwc = null;
	public static IndexWriter indexWriter = null;
	
	//private Set<Integer> indexedString = new HashSet<Integer>(30000000);
	
	public ContentIndexer() {
		try{
			dir = FSDirectory.open(new File(Constants.INDEX));
			analyzer = new StandardAnalyzer(Version.LUCENE_40);
			iwc = new IndexWriterConfig(Version.LUCENE_40, analyzer);
			if (create) {
				// Create a new index in the directory, removing any previously indexed documents:
				iwc.setOpenMode(OpenMode.CREATE);
			} else {
				// Add new documents to an existing index:
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			}
			indexWriter = new IndexWriter(dir, iwc);
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public ContentIndexer(String indexDir) {
		try{
			dir = FSDirectory.open(new File(indexDir));
			analyzer = new StandardAnalyzer(Version.LUCENE_40);
			iwc = new IndexWriterConfig(Version.LUCENE_40, analyzer);
			if (create) {
				// Create a new index in the directory, removing any previously indexed documents:
				iwc.setOpenMode(OpenMode.CREATE);
			} else {
				// Add new documents to an existing index:
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			}
			indexWriter = new IndexWriter(dir, iwc);
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	/** Index all text files under a directory. */
	public static void main(String[] args) {
		new ContentIndexer().indexStatus(PAGE_SIZE);
	}
	
	public void indexStatus(int nums){
		Date start = new Date();
		if (nums == -1){
			//index all statuses!
		}else{
			int pageNo = nums / PAGE_SIZE;
			int firstPageSize = nums % PAGE_SIZE;
			if (firstPageSize == 0){
				if (pageNo == 0){
					return;
				}
				firstPageSize = PAGE_SIZE;
				pageNo--;
			}
			Map<String, Object> params = new HashMap<String, Object>();
			
			logger.info("add status index: firstPageSize: {}", firstPageSize);
			params.put("firstPageSize", firstPageSize);
			List<Map<String, Object>> statuses = statusesDao.getFirstPageStatuses(params);
			this.indexStringList(indexWriter, statuses);

			for (int i = 0; i < pageNo; i++){
				logger.info("add status index: page_size: {}, page_offset: {}", PAGE_SIZE, firstPageSize + i * PAGE_SIZE);
				params.put("page_offset", firstPageSize + i * PAGE_SIZE);
				params.put("page_size", ContentIndexer.PAGE_SIZE);
				this.indexStringList(indexWriter, statusesDao.getNextPageStatuses(params));
			}
			try {
				indexWriter.commit();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Date end = new Date();
		logger.info("indexStatus -- time: {} total milliseconds", end.getTime() - start.getTime());
	}
	
	public void indexStringList(List<Map<String, Object>> statuses){
		this.indexStringList(indexWriter, statuses);
		try {
			indexWriter.commit();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void indexStringList(IndexWriter writer, List<Map<String, Object>> statuses){
		if (statuses == null){
			logger.info("indexStringList: statuses is empty!");
			return;
		}
		logger.info("start processing to index statuses -- size: {}", statuses.size());
		for (Map<String, Object> status : statuses){
			String statusContent = (String)status.get("content");
			this.indexString(indexWriter, statusContent);
		}
	}
	
	public void indexString(IndexWriter writer, String content){
		try{
			Document doc = new Document();
			content = doFilter(content);
			if (content != null){
				Field contentField = new VecTextField(Constants.FILED_NAME, content, Field.Store.YES);
				doc.add(contentField);
				writer.addDocument(doc);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public String doFilter(String content){
		content = content.replaceAll("[^a-zA-Z0-9\u4E00-\u9FA5]", " ").replaceAll(" +", " ").trim();
		/*
		if (indexedString.add(content.hashCode())){
			return content;
		}else{
			return null;
		}
		*/
		return content;
	}
}