


import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.RomanianStemmer;

/**
 * {@link Analyzer} for Romanian.
 */
public final class RomanianAnalyzerUsingAnotherConstructorForStopwordAnalyzer extends StopwordAnalyzerBase {
  private final CharArraySet stemExclusionSet;
  
  /** File containing default Romanian stopwords. */
  public final static String DEFAULT_STOPWORD_FILE = "stopwords.txt";

  
  
  /**
   * Atomically loads the DEFAULT_STOP_SET in a lazy fashion once the outer class 
   * accesses the static final set the first time.;
   */
  private static class DefaultSetHolder {
    static final CharArraySet DEFAULT_STOP_SET;

    static {
      try {
        DEFAULT_STOP_SET = loadStopwordSet(false, RomanianAnalyzerUsingAnotherConstructorForStopwordAnalyzer.class, 
            DEFAULT_STOPWORD_FILE, STOPWORDS_COMMENT);
      } catch (IOException ex) {
        // default set should always be present as it is part of the
        // distribution (JAR)
        throw new RuntimeException("Unable to load default stopword set");
      }
    }
  }

  /**
   * Builds an analyzer with the default stop words: {@link #DEFAULT_STOPWORD_FILE}.
   */
  public RomanianAnalyzerUsingAnotherConstructorForStopwordAnalyzer(Version matchVersion) {
    this(matchVersion, DefaultSetHolder.DEFAULT_STOP_SET);
  }
  
  /**
   * Builds an analyzer with the given stop words.
   * 
   * @param matchVersion lucene compatibility version
   * @param stopwords a stopword set
   */
  public RomanianAnalyzerUsingAnotherConstructorForStopwordAnalyzer(Version matchVersion, CharArraySet stopwords) {
    this(matchVersion, stopwords, CharArraySet.EMPTY_SET);
  }

  /**
   * Builds an analyzer with the given stop words. If a non-empty stem exclusion set is
   * provided this analyzer will add a {@link SetKeywordMarkerFilter} before
   * stemming.
   * 
   * @param matchVersion lucene compatibility version
   * @param stopwords a stopword set
   * @param stemExclusionSet a set of terms not to be stemmed
   */
  public RomanianAnalyzerUsingAnotherConstructorForStopwordAnalyzer(Version matchVersion, CharArraySet stopwords, CharArraySet stemExclusionSet) {
    super(matchVersion, stopwords);
    this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(
        matchVersion, stemExclusionSet));
  }

  /**
   * Creates a
   * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
   * which tokenizes all the text in the provided {@link Reader}.
   * 
   * @return A
   *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
   *         built from an {@link StandardTokenizer} filtered with
   *         {@link StandardFilter}, {@link LowerCaseFilter}, {@link StopFilter}
   *         , {@link SetKeywordMarkerFilter} if a stem exclusion set is
   *         provided and {@link SnowballFilter}.
   */
  @Override
  protected TokenStreamComponents createComponents(String fieldName,
      Reader reader) {
    final Tokenizer source = new StandardTokenizer(matchVersion, reader);
    TokenStream result = new ASCIIFoldingFilter(source);
    result = new LowerCaseFilter(matchVersion, result);
    result = new StopFilter(matchVersion, result, stopwords);
    if(!stemExclusionSet.isEmpty())
      result = new SetKeywordMarkerFilter(result, stemExclusionSet);
    result = new SnowballFilter(result, new RomanianStemmer());
    return new TokenStreamComponents(source, result);
  }
}
