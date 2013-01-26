import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.io.*;

import org.w3c.dom.Document;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.*;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.queryparser.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.tartarus.snowball.ext.PorterStemmer;

public class WikipediaIR {
    
    public static final boolean VERBOSE = false;
    
    public static void main(String args[]) {
        try {
            // Wikipedia files should be located in the same directory, in a folder called "en".
            // Files downloaded from http://www.site.uottawa.ca/~diana/csi4107/gikiCLEF/en.zip
            File dir = new File("en");
            if (!dir.isDirectory()) {
                System.out.println("Error: Wikipedia files should be located in a folder called " +
                                   "en under the project root.");
                return;
            }

            // Make sure we can access StopWords.txt
            File stopWordsFile = new File ("StopWords.txt");
            if (!stopWordsFile.canRead()) {
                System.out.println("Error: Cannot read StopWords.txt");
                return;
            }

            // Initialize CharArraySet for holding stopWords; required for the EnglishAnalyzer
            CharArraySet stopWordSet = new CharArraySet(Version.LUCENE_41, 700, true);
            
            // Use bufferedReader to build stopWordsSet
            FileReader stopWordReader = new FileReader(stopWordsFile);
            BufferedReader bufferedReader = new BufferedReader(stopWordReader);
            String line = null;
            
            while ((line = bufferedReader.readLine()) != null) {
                stopWordSet.add(line);
            }
            
            bufferedReader.close();
            
            // Initialize analyzer with our stopWords
            EnglishAnalyzer analyzer = new EnglishAnalyzer(Version.LUCENE_41,stopWordSet);
            
            File index = new File("index");
            Directory directory;
            
            // Check for a folder called index under application root. If it exists, use its
            // contents as the index, otherwise create the folder and build the index
            if (index.isDirectory()) {
                System.out.println("Using existing Index");
                directory = FSDirectory.open(index);
            } else {
                // Create a folder called index under application root
                boolean indexCreated = index.mkdir();
                if (indexCreated) System.out.println("Index directory created");
    
                // Initialize Lucene IndexWriter
                directory = FSDirectory.open(index);
                IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_41, analyzer);
                IndexWriter iwriter = new IndexWriter(directory, config);
    
                // Traverse files and index
                System.out.println("Begin indexing");
                traverseAndIndex(dir,iwriter);
                System.out.println("Indexing complete");
                iwriter.close();
            }

            // Now search the index:
            DirectoryReader ireader = DirectoryReader.open(directory);
            IndexSearcher isearcher = new IndexSearcher(ireader);
            // Parse a simple query that searches for "text":
            QueryParser parser = new QueryParser(Version.LUCENE_41, "contents", analyzer);
            Query query = parser.parse("Which authors were born in and write about the Bohemian Forest?");
            ScoreDoc[] hits = isearcher.search(query, null, 1000).scoreDocs;
            
            // Iterate through the results:
            for (int i = 0; i < hits.length; i++) {
                org.apache.lucene.document.Document hitDoc = isearcher.doc(hits[i].doc);
                if (VERBOSE) System.out.println(hitDoc.toString());
            }
            
            ////
            //****************************************************************************************
            // This code will create a text file "out.txt" containing all the terms in the index.
            // References:
            // http://lucene.apache.org/core/4_1_0/MIGRATE.html
            // http://stackoverflow.com/questions/14211974/how-can-i-read-and-print-lucene-index-4-0
            //****************************************************************************************
            AtomicReader ar = SlowCompositeReaderWrapper.wrap(ireader);
            Terms terms = ar.terms("contents");
            
            TermsEnum termsEnum= terms.iterator(null);
            BytesRef text;
            FileWriter fstream = new FileWriter("out.txt");
            BufferedWriter out = new BufferedWriter(fstream);
            while((text = termsEnum.next()) != null) {
                
                out.write("field=" + "contents" + "; text=" + text.utf8ToString() + "\n");  
                //System.out.println("field=" + "contents" + "; text=" + text.utf8ToString());
            }
            out.close();
            //****************************************************************************************
            ////
            
            System.out.println("Number of hits " + hits.length);
            ireader.close();
            directory.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void traverseAndIndex(File file, IndexWriter iwriter)
    {
        File[] fileList = file.listFiles();

        for (int i = 0; i < fileList.length; i++)
        {
            if (fileList[i].isFile())
            {
                if (VERBOSE) System.out.println("Indexing" + fileList[i].getAbsolutePath());
                try
                {
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    // Parse a file into a DOM tree.
                    org.w3c.dom.Document doc = dBuilder.parse(fileList[i]);
                    // Grab the first node in the DOM tree.
                    NodeList nList = doc.getChildNodes();
                    // Grab text of this node and of all it's descendant nodes.
                    String fileContents = nList.item(0).getTextContent();

                    org.apache.lucene.document.Document idoc = new org.apache.lucene.document.Document();
                    // Index, tokenize, don't store in index document
                    idoc.add(new TextField("contents", fileContents, Field.Store.NO));
                    // Indexed but not tokenize the path to the file, store in index document
                    idoc.add(new StringField("path", fileList[i].getAbsolutePath(), Field.Store.YES));
                    iwriter.addDocument(idoc);
                }
                catch (Exception e)
                {

                }
            }
            else if (fileList[i].isDirectory())
            {
                traverseAndIndex(fileList[i],iwriter);
            }
        }
    }

}