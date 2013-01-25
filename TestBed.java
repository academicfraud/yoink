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
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.*;
import org.apache.lucene.util.Version;
import org.apache.lucene.queryparser.*;
import org.apache.lucene.queryparser.classic.QueryParser;




public class TestBed {

	
	public static void main(String args[])
	{
		
		try {
			
			File dir = new File(System.getProperty("user.dir") + "\\en");
			System.out.println(dir.toString());


			File index = new File("index");
			index.createNewFile();

			File stopWords = new File ("StopWords.sdx");
			System.out.println(stopWords.canRead());

			FileReader stopWordReader = new FileReader(stopWords);


			
			//Create the StandardAnalyzer that will also use stop words.
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_41,stopWordReader);

			// To store an index on disk, use this instead:
			Directory directory = FSDirectory.open(index);
			IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_41, analyzer);
			IndexWriter iwriter = new IndexWriter(directory, config);

			traverseAndIndex(dir,iwriter);

			iwriter.close();

			System.out.println("Done");
			
			
			
			
			
			
			
			
			
			
			
			// Now search the index:
		    DirectoryReader ireader = DirectoryReader.open(directory);
		    IndexSearcher isearcher = new IndexSearcher(ireader);
		    // Parse a simple query that searches for "text":
		    QueryParser parser = new QueryParser(Version.LUCENE_41, "contents", analyzer);
		    Query query = parser.parse("Which authors were born in and write about the Bohemian Forest?");
		    ScoreDoc[] hits = isearcher.search(query, null, 1000).scoreDocs;
		    
		    System.out.println("Number of hits " + hits.length);
		    // Iterate through the results:
		    for (int i = 0; i < hits.length; i++) {
		    	org.apache.lucene.document.Document hitDoc = isearcher.doc(hits[i].doc);
		    	System.out.println(hitDoc.toString());
		    }
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
				//System.out.println("Indexing" + fileList[i].getAbsolutePath());

				try
				{
					//
					DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					org.w3c.dom.Document doc = dBuilder.parse(fileList[i]); //Parse a file into a DOM tree.
					NodeList nList = doc.getChildNodes();                   //Grab the first node in the DOM tree.
					String fileContents = nList.item(0).getTextContent();   //Grab text of this node and of all it's descendant nodes.



					org.apache.lucene.document.Document idoc = new org.apache.lucene.document.Document();
					//Index, tokenize, don't store in index document
					idoc.add(new TextField("contents", fileContents, Field.Store.NO));
					//Indexed but not tokenize the path to the file, store in index document
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
