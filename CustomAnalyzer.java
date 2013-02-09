/*
 * CustomAnalzyer for use in indexing and searching.
 * Basically a modified version of the EnglishAnalzyer from the Lucene library.
 * Modifications will mainly be in the TokenStreamComponents library.
 * 
 * 
 * 
 */

import java.io.Reader;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.KeywordMarkerFilter;
import org.apache.lucene.analysis.pattern.PatternReplaceFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;
import org.apache.lucene.util.Version;


public final class CustomAnalyzer extends StopwordAnalyzerBase 
{
	
	private final CharArraySet stemExclusionSet;

	/**
	 * Returns an unmodifiable instance of the default stop words set.
	 * @return default stop words set.
	 */
	public static CharArraySet getDefaultStopSet()
	{
		return DefaultSetHolder.DEFAULT_STOP_SET;
	}

	/**
	 * Atomically loads the DEFAULT_STOP_SET in a lazy fashion once the outer class 
	 * accesses the static final set the first time.;
	 */
	private static class DefaultSetHolder 
	{
		static final CharArraySet DEFAULT_STOP_SET = StandardAnalyzer.STOP_WORDS_SET;
	}

	/**
	 * Builds an analyzer with the default stop words: {@link #getDefaultStopSet}.
	 */
	public CustomAnalyzer(Version matchVersion) 
	{
		this(matchVersion, DefaultSetHolder.DEFAULT_STOP_SET);
	}

	/**
	 * Builds an analyzer with the given stop words.
	 * 
	 * @param matchVersion lucene compatibility version
	 * @param stopwords a stopword set
	 */
	public CustomAnalyzer(Version matchVersion, CharArraySet stopwords) 
	{
		this(matchVersion, stopwords, CharArraySet.EMPTY_SET);
	}

	/**
	 * Builds an analyzer with the given stop words. If a non-empty stem exclusion set is
	 * provided this analyzer will add a {@link KeywordMarkerFilter} before
	 * stemming.
	 * 
	 * @param matchVersion lucene compatibility version
	 * @param stopwords a stopword set
	 * @param stemExclusionSet a set of terms not to be stemmed
	 */
	public CustomAnalyzer(Version matchVersion, CharArraySet stopwords, CharArraySet stemExclusionSet)
	{
		super(matchVersion, stopwords);
		this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(
				matchVersion, stemExclusionSet));
	}

	/**
	 * 
	 */
	@Override
	protected TokenStreamComponents createComponents(String fieldName, Reader reader) 
	{
		//final Tokenizer source = new StandardTokenizer(matchVersion, reader);
		
		//****
		//Modification: use the WikipediaTokenizer instead of the StandardTokenizer. It
		//should remove the Wikipedia markup.
		final Tokenizer source = new WikipediaTokenizer(reader);
		//****
		
		TokenStream result = new StandardFilter(matchVersion, source);
		// prior to this we get the classic behavior, standardfilter does it for us.
		if (matchVersion.onOrAfter(Version.LUCENE_31))
			result = new EnglishPossessiveFilter(matchVersion, result);
		result = new LowerCaseFilter(matchVersion, result);
		result = new StopFilter(matchVersion, result, stopwords);
		if(!stemExclusionSet.isEmpty())
			result = new KeywordMarkerFilter(result, stemExclusionSet);
		result = new PorterStemFilter(result);
		
		//****
		//Additon: use the PatterReplaceFilter. It should replace all tokens that match the 
		//regular expression with a given string (in this case, a blank).
		//
		result = new PatternReplaceFilter(result,Pattern.compile("^.*\\.(com|org|jpg|png)$"),null,true);
		//****
		
		// Filter out terms with non-alphanumeric chars
		result = new PatternReplaceFilter(result,Pattern.compile("^.*[^a-zA-Z0-9]+.*$"),null,true);
		
		return new TokenStreamComponents(source, result);
	}
}
