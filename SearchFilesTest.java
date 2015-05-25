
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.WordlistLoader;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class SearchFilesTest {

	private static final String codification = "UTF-8";

	private SearchFilesTest() {
	}

	/** Simple command-line based search demo. */
	public static void main(String[] args) throws Exception {
		String index = args[0];
		String field = "contents";
		String queries = "resources/query.txt";
		String queryString = null;
		int hitsPerPage = 10;

		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(
				index)));
		IndexSearcher searcher = new IndexSearcher(reader);
		File stopWordsFile = new File("resources/stop.txt");
		CharArraySet stopWordsCharArraySet = WordlistLoader.getWordSet(
				new FileReader(stopWordsFile), Version.LUCENE_47);
		Analyzer analyzer = new RomanianAnalyzerUsingAnotherConstructorForStopwordAnalyzer(
				Version.LUCENE_47, stopWordsCharArraySet);

		BufferedReader in = null;
		in = new BufferedReader(new InputStreamReader(new FileInputStream(
				queries), codification));
		QueryParser parser = new QueryParser(Version.LUCENE_47, field, analyzer);
		while (true) {
			String line = in.readLine();
			if (line == null || line.length() == -1) {
				break;
			}
			line = line.trim();
			if (line.length() == 0) {
				break;
			}

			Query query = parser.parse(line);
			System.out.println("Looking for: " + query.toString(field));
			doPagingSearch(in, searcher, query);

			if (queryString != null) {
				break;
			}
		}
		reader.close();
	}

	public static void doPagingSearch(BufferedReader in,
			IndexSearcher searcher, Query query)
			throws IOException {

		TopDocs results = searcher.search(query, 100);
		ScoreDoc[] hits = results.scoreDocs;

		int numTotalHits = results.totalHits;
		System.out.println(numTotalHits + " total matching documents");

		for (int i = 0; i < hits.length; i++) {
			Document doc = searcher.doc(hits[i].doc);
			String path = doc.get("path");
			if (path != null) {
				System.out.println((i + 1) + ". " + path + " score = "
						+ hits[i].score);
				String title = doc.get("title");
				if (title != null) {
					System.out.println("   Title: " + doc.get("title"));
				}
				
			} else {
				System.out
						.println((i + 1) + ". " + "No path for this document");
			}
		}
	}
}
