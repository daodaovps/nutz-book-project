package net.wendal.nutzbook.service.yvr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.wendal.nutzbook.bean.yvr.Topic;
import net.wendal.nutzbook.lucene.LuceneIndex;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.dao.sql.Sql;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.lang.Files;
import org.nutz.log.Log;
import org.nutz.log.Logs;
import org.wltea.analyzer.lucene.IKAnalyzer;

@IocBean(create = "init", depose = "close")
public class TopicSearchService {

	@Inject
	protected Dao dao;
	
	
	private static Log log = Logs.get();

	@Inject("java:$conf.get('topic.lucene.dir')")
	private String indexDir;

	protected LuceneIndex luceneIndex;
	
	public void add(Topic topic) {
		if (topic == null)
			return; // 虽然不太可能,还是预防一下吧
		// 暂时不索引评论
		//dao.fetchLinks(topic, "replies");
		Document document;
		document = new Document();
		Field field;
		FieldType fieldType;

		// 先加入id
		fieldType = new FieldType();
		fieldType.setIndexed(true);// 索引
		fieldType.setStored(true);// 存储
		fieldType.setStoreTermVectors(true);
		fieldType.setTokenized(true);
		fieldType.setStoreTermVectorPositions(true);// 存储位置
		fieldType.setStoreTermVectorOffsets(true);// 存储偏移量
		field = new Field("id", topic.getId(), fieldType);
		document.add(field);

		// 加入标题
		fieldType = new FieldType();
		fieldType.setIndexed(true);// 索引
		fieldType.setStored(true);// 存储
		fieldType.setStoreTermVectors(true);
		fieldType.setTokenized(true);
		fieldType.setStoreTermVectorPositions(true);// 存储位置
		fieldType.setStoreTermVectorOffsets(true);// 存储偏移量
		field = new Field("title", topic.getTitle(), fieldType);
		document.add(field);

		// 加入文章内容
		fieldType = new FieldType();
		fieldType.setIndexed(true);// 索引
		fieldType.setStored(true);// 存储
		fieldType.setStoreTermVectors(true);
		fieldType.setTokenized(true);
		fieldType.setStoreTermVectorPositions(true);// 存储位置
		fieldType.setStoreTermVectorOffsets(true);// 存储偏移量
		field = new Field("content", topic.getContent(), fieldType);
		document.add(field);

		try {
			luceneIndex.writer.addDocument(document);
		} catch (IOException e) {
			log.debug("add to index fail : id=" + topic.getId());
		}
	}

	//@RequiresPermissions("topic:index:rebuild")
	public void rebuild() throws IOException {
		Sql sql = Sqls.queryString("select id from t_topic");
		dao.execute(sql);
		luceneIndex.writer.deleteAll();
		String[] topicIds = sql.getObject(String[].class);
		for (String topicId : topicIds) {
			Topic topic = dao.fetch(Topic.class, topicId);
			add(topic);
		}
		luceneIndex.writer.commit();
	}
	
	public List<String> search(String keyword) throws IOException, ParseException {
		IndexReader reader = luceneIndex.reader();
		try {
			IndexSearcher searcher = new IndexSearcher(reader);
			Analyzer analyzer = new IKAnalyzer();
			MultiFieldQueryParser parser = new MultiFieldQueryParser(Version.LUCENE_4_9, new String[]{"title", "content"}, analyzer);
			// 将关键字包装成Query对象
			Query query = parser.parse(keyword);
			TopDocs results = searcher.search(query, 30);
			List<String> ids = new ArrayList<String>();
			for (ScoreDoc doc : results.scoreDocs) {
				ids.add(searcher.doc(doc.doc).get("id"));
			}
			return ids;
		} finally {
			reader.close();
		}
	}

	public void init() throws IOException {
		Files.createDirIfNoExists(indexDir);
		luceneIndex = new LuceneIndex(indexDir, OpenMode.CREATE_OR_APPEND);
	}

	public void close() throws IOException {
		if (luceneIndex != null)
			luceneIndex.close();
	}
}
