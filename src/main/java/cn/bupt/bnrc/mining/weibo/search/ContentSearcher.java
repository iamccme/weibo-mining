package cn.bupt.bnrc.mining.weibo.search;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.sandbox.queries.regex.RegexQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cn.bupt.bnrc.mining.weibo.util.Utils;

@Service
public class ContentSearcher {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	//related to the number of record in the database.
	public static int MAX_SIZE = 1000000;
	
	private DirectoryReader reader;
	private IndexSearcher searcher;
	private Analyzer analyzer;
	private QueryParser parser;

	public ContentSearcher() {
		try {
			reader = DirectoryReader.open(FSDirectory.open(new File(Constants.INDEX)));
			searcher = new IndexSearcher(reader);
			analyzer = new StandardAnalyzer(Version.LUCENE_40);
			parser = new QueryParser(Version.LUCENE_40, Constants.FILED_NAME, analyzer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public ContentSearcher(String indexDir) {
		try {
			reader = DirectoryReader.open(FSDirectory.open(new File(indexDir)));
			searcher = new IndexSearcher(reader);
			analyzer = new StandardAnalyzer(Version.LUCENE_40);
			parser = new QueryParser(Version.LUCENE_40, Constants.FILED_NAME, analyzer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public IndexSearcher getSearcher(){
		return searcher;
	}
	
	public int getTotalDocumentsCount(){
		return this.reader.numDocs();
	}
	
	public TopDocs search2Words(String word1, String word2, int n){
		Query query1 = this.word2Query(word1);
		Query query2 = this.word2Query(word2);
		BooleanQuery booleanQuery = new BooleanQuery();
		booleanQuery.add(query1, BooleanClause.Occur.MUST);
		booleanQuery.add(query2, BooleanClause.Occur.MUST);
		
		try{
			return searcher.search(booleanQuery, n);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * TODO
	 * when the word is 'nb', this should regard as one search word.
	 * @param word
	 * @return
	 */
	public Query word2Query(String word){
		try {
			return parser.parse(Constants.FILED_NAME+":\"" + word + "\"");
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public TopDocs testSearchWord(String word, int n){
		try {
			Query query = parser.parse("content:\""+word+"\"");
			return searcher.search(query, n);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public TopDocs searchWord(String word, int n){
		Query query = this.word2Query(word);
		try {
			return searcher.search(query, n);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * but this not work! 
	 * TODO need to recify
	 * @param context
	 * @param n
	 * @return
	 */
	public TopDocs searchContext2(String context, int n){
		Term term = new Term("content", Utils.contextToRegex(context));
		RegexQuery query = new RegexQuery(term);
		System.out.println(query.toString());
		try{
			return searcher.search(query, n);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public TopDocs searchContext(String context, int n){
		PhraseQuery query = new PhraseQuery();
		for (int i = 0; i < context.length(); i++){
			if (context.charAt(i) != ' '){		//avoid the space.
				Term term = new Term("content", context.charAt(i)+"");
				query.add(term);
			}
		}
		query.setSlop(4);	//A x x x x B is searched
		try{
			return searcher.search(query, n);
		}catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	@Deprecated
	public TopDocs search(String words, int n){
		parser.setDefaultOperator(QueryParser.AND_OPERATOR);
		try{
			TopDocs results = searcher.search(parser.parse(words), n);
			return results;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public String getDocContent(int docID){
		String content = null;
		try {
			content = searcher.doc(docID).get("content");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	}

	// according to FieldTermStack class -- lucene-highlighter-vectorhighlighter
	public HashSet<Integer> getOneKeyPositionList(int docID, String fieldName, String searchTerm) throws IOException {
		HashSet<Integer> positions = new HashSet<Integer>();
		final Terms vector = reader.getTermVector(docID, fieldName);
		if (vector == null) {
			return positions;
		}

		final TermsEnum termsEnum = vector.iterator(null);
		DocsAndPositionsEnum dpEnum = null;
		BytesRef text;

		while ((text = termsEnum.next()) != null) {
			final String term = text.utf8ToString();
			if ( !this.isSatisfied(term, searchTerm) )
				continue;
			dpEnum = termsEnum.docsAndPositions(null, dpEnum);
			if (dpEnum == null) {
				return positions;
			}

			dpEnum.nextDoc();
			final int freq = dpEnum.freq();
			
			for (int i = 0; i < freq; i++) {
				int pos = dpEnum.nextPosition();
				positions.add(pos);
			}
		}
		return positions;
	}
	
	public boolean isSatisfied(String str1, String str2){
		if (str1.contains(str2)) return true;
		return false;
	}
	
	public int getCooccurrence(String key1, String key2){
		Date start = new Date();
		parser.setDefaultOperator(QueryParser.AND_OPERATOR);
		String searchQuery = key1 + " " + key2;
		int cooccurrence = 0;
		try {
			TopDocs results = searcher.search(parser.parse(searchQuery), MAX_SIZE);
			cooccurrence = results.totalHits;
		}catch(Exception e){
			e.printStackTrace();
		}
		Date end = new Date();
		logger.info("getCoocurrence of two words -- key1: {}, key2: {}, time: {} total milliseconds",
				new Object[]{key1, key2, end.getTime()-start.getTime()});
		return cooccurrence;
	}

	/**
	 * need to optimize.
	 * @param key1 first word
	 * @param key2 second word
	 * @param distance the distance between these two words despite the order of the two words
	 * @return
	 */
	public int getCooccurrence(String key1, String key2, int distance) {
		Date start = new Date();
		int cooccurrence = 0;
		parser.setDefaultOperator(QueryParser.AND_OPERATOR);
		String searchQuery = key1 + " " + key2;
		try {
			TopDocs results = searcher.search(parser.parse(searchQuery), MAX_SIZE);
			for (int i = 0; i < results.totalHits; i++) {
				int docId = results.scoreDocs[i].doc;
				HashSet<Integer> key1Position = this.getOneKeyPositionList(docId, Constants.FILED_NAME, key1);
				HashSet<Integer> key2Position = this.getOneKeyPositionList(docId, Constants.FILED_NAME, key2);
				for (Iterator<Integer> it = key1Position.iterator(); it.hasNext();){
					int pos = it.next();
					if (key2Position.contains(pos+distance) || key2Position.contains(pos-distance)){
						cooccurrence++;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Date end = new Date();
		logger.info("getCoocurrence of two words -- key1: {}, key2: {}, distance: {}, time: {} total milliseconds",
				new Object[]{key1, key2, distance, end.getTime()-start.getTime()});
		
		return cooccurrence;
	}

	public static void main(String[] args) throws Exception {
		ContentSearcher searcher = new ContentSearcher("index");
		System.out.println(searcher.getTotalDocumentsCount());
		int num = 10;

		try {
			TopDocs docs = searcher.searchWord("近很能吃", num);
			
			System.out.println(docs.totalHits);
			for (int i = 0; i< Math.min(num, docs.totalHits); i++){
				int docId = docs.scoreDocs[i].doc;
				String content = searcher.searcher.doc(docId).get("content");
				System.out.println(content);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
