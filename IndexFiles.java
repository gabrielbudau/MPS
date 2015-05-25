

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.WordlistLoader;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.apache.poi.xwpf.converter.core.FileURIResolver;
import org.apache.poi.xwpf.converter.xhtml.XHTMLOptions;
import org.apache.poi.xwpf.converter.xhtml.XHTMLConverter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;


public class IndexFiles {

	private static final String codification = "UTF-8";

	private IndexFiles() {
	}


	public static void main(String[] args) throws FileNotFoundException,
			IOException {
		String indexPath = args[0];
		String docsPath = args[1];
		final File docDir = new File(docsPath);
		Directory dir = FSDirectory.open(new File(indexPath));
		File stopWordsFile = new File("resources/stop.txt");
		CharArraySet stopWordsCharArraySet = WordlistLoader.getWordSet(
				new FileReader(stopWordsFile), Version.LUCENE_47);
		Analyzer analyzer = new RomanianAnalyzerUsingAnotherConstructorForStopwordAnalyzer(
				Version.LUCENE_47, stopWordsCharArraySet);
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_47,
				analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		IndexWriter writer = new IndexWriter(dir, iwc);
		FileInputStream fis;

		String[] files = docDir.list();
		for (String fileString : files) {
			File file = new File(docsPath + "/" + fileString);
			try {
				fis = new FileInputStream(file);
			} catch (FileNotFoundException fnfe) {
				fnfe.printStackTrace();
				return;
			}

			String fileName = file.getName();
			int index = fileName.lastIndexOf('.');
			String extension = "";
			if (index != -1) {
				extension = fileName.substring(index);
			}

			Document doc = new Document();

			Field pathField = new StringField("path", file.getPath(),
					Field.Store.YES);
			doc.add(pathField);
			switch (extension) {
			case ".html":
				String html = "";
				int content;
				InputStreamReader fiss = new InputStreamReader(fis,
						codification);
				while ((content = fiss.read()) != -1) {
					html += (char) content;
				}
				String plainText = Jsoup.parse(html).text();
				doc.add(new TextField("contents", new BufferedReader(
						new StringReader(plainText))));
				break;
			case ".pdf":
				doc.add(new TextField("contents", new StringReader(
						PDFTextParser.pdftoText(file.getAbsolutePath()))));
				break;
			case ".docx":
				XWPFDocument document = new XWPFDocument(fis);
				XHTMLOptions options = XHTMLOptions.create().URIResolver(new FileURIResolver(new File("word/media")));
				OutputStream out = new ByteArrayOutputStream();
				XHTMLConverter.getInstance().convert(document, out, options);
				String converted_text = Jsoup.parse( out.toString()).text();
				doc.add(new TextField("contents", new BufferedReader(
						new StringReader(converted_text))));
				break;

			default:
				doc.add(new TextField("contents", new BufferedReader(
						new InputStreamReader(fis, codification))));
				break;
			}
			System.out.println("adding " + file);
			writer.addDocument(doc);
			fis.close();
		}
		writer.close();

	}
}
