package edu.stanford.protege.search.lucene.tab.engine;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.standard.ClassicFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.analysis.util.WordlistLoader;

/**
 * Filters {@link WhitespaceTokenizer} with {@link ClassicFilter}, {@link
 * LowerCaseFilter} and {@link StopFilter}, using a list of
 * English stop words.
 *
 * This analyzer is a modified version of {@link ClassicAnalyzer}
 */
public class ClassicWhitespaceAnalyzer extends StopwordAnalyzerBase {

    /**
     * An unmodifiable set containing some common English words that are usually
     * not useful for searching.
     */
    public static final CharArraySet STOP_WORDS_SET = StopAnalyzer.ENGLISH_STOP_WORDS_SET;

    /** Builds an analyzer with the given stop words.
     * @param stopWords stop words */
    public ClassicWhitespaceAnalyzer(CharArraySet stopWords) {
      super(stopWords);
    }

    /** Builds an analyzer with the default stop words ({@link
     * #STOP_WORDS_SET}).
     */
    public ClassicWhitespaceAnalyzer() {
      this(STOP_WORDS_SET);
    }

    /** Builds an analyzer with the stop words from the given reader.
     * @see WordlistLoader#getWordSet(Reader)
     * @param stopwords Reader to read stop words from */
    public ClassicWhitespaceAnalyzer(Reader stopwords) throws IOException {
      this(loadStopwordSet(stopwords));
    }

    @Override
    protected TokenStreamComponents createComponents(final String fieldName) {
        final WhitespaceTokenizer src = new WhitespaceTokenizer();
        TokenStream tok = new ClassicFilter(src);
        tok = new LowerCaseFilter(tok);
        tok = new StopFilter(tok, stopwords);
        return new TokenStreamComponents(src, tok);
    }
}
