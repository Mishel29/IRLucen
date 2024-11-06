package ie.tcd.dalyc24;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class CreateIndex {

    public static void main(String[] args) throws Exception {
        String indexP = "cran.all.1400";
        String output_directory = "output";

        // Analyzer name and object
        String[] analyzzerName_list = {"standard", "simple", "english"};
        Analyzer[] analyzzer_list = {new StandardAnalyzer(), new SimpleAnalyzer(), new EnglishAnalyzer()};

        for (int i = 0; i < analyzzerName_list.length; i++) {
            String analyzerName = analyzzerName_list[i];
            Analyzer analyzer = analyzzer_list[i];

            String outputIdx_loc = Paths.get(output_directory, analyzerName + "_index").toString();
            Directory outputDirectory_idx = FSDirectory.open(Paths.get(outputIdx_loc));
            IndexWriterConfig idxConfig = new IndexWriterConfig(analyzer);
            idxConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            IndexWriter i_write = new IndexWriter(outputDirectory_idx, idxConfig);

            BufferedReader bRead = Files.newBufferedReader(Paths.get(indexP));
            String q_line = bRead.readLine();
            while (q_line != null) {
                Document doc = new Document();
                if (q_line.matches("(\\.I)( )(\\d)*")) {
                    StringBuilder stringB;
                    doc.add(new StringField("Id", q_line.substring(3), Field.Store.YES));
                    q_line = bRead.readLine();
                    while (q_line != null && !q_line.matches("(\\.I)( )(\\d)*")) {
                        if (q_line.matches("\\.T")) {
                            stringB = new StringBuilder();
                            q_line = bRead.readLine();
                            while (q_line != null && !q_line.matches(("\\.A"))) {
                                stringB.append(q_line).append(" ");
                                q_line = bRead.readLine();
                            }
                            doc.add(new TextField("Title", stringB.toString(), Field.Store.YES));
                        } else if (q_line.matches("\\.A")) {
                            stringB = new StringBuilder();
                            q_line = bRead.readLine();
                            while (q_line != null && !q_line.matches(("\\.B"))) {
                                stringB.append(q_line).append(" ");
                                q_line = bRead.readLine();
                            }
                            doc.add(new TextField("Author", stringB.toString(), Field.Store.YES));
                        } else if (q_line.matches("\\.B")) {
                            stringB = new StringBuilder();
                            q_line = bRead.readLine();
                            while (q_line != null && !q_line.matches(("\\.W"))) {
                                stringB.append(q_line).append(" ");
                                q_line = bRead.readLine();
                            }
                            doc.add(new TextField("Biblo", stringB.toString(), Field.Store.YES));
                        } else if (q_line.matches("\\.W")) {
                            stringB = new StringBuilder();
                            q_line = bRead.readLine();
                            while (q_line != null && !q_line.matches(("(\\.I)( )(\\d)*"))) {
                                stringB.append(q_line).append(" ");
                                q_line = bRead.readLine();
                            }
                            doc.add(new TextField("Content", stringB.toString(), Field.Store.YES));
                        }
                    }
                }
                i_write.addDocument(doc);
            }
            i_write.close();
            bRead.close();

            String queryFP =  "cran.qry";
            String output_queryFP = Paths.get(output_directory, analyzerName + "output.txt").toString();
            // Query the docs
            Directory dir = FSDirectory.open(Paths.get(outputIdx_loc));
            DirectoryReader idxRead = DirectoryReader.open(dir);
            IndexSearcher idxSearch = new IndexSearcher(idxRead);

            Map<String, Float> scr_map = new HashMap<>();
            scr_map.put("Title", 0.5f);
            scr_map.put("Author", 0.2f);
            scr_map.put("Biblo", 0.3f);
            scr_map.put("Content", 0.4f);
            MultiFieldQueryParser qParser = new MultiFieldQueryParser(
                    new String[]{"Title", "Author", "Biblo", "Content"}, analyzer, scr_map
            );

            bRead = Files.newBufferedReader(Paths.get(queryFP));
            FileWriter writerQuery = new FileWriter(output_queryFP);
            q_line = bRead.readLine();
            int q_Id = 1;
            String query_Id, query_content;
            while (q_line != null) {
                if (q_line.matches("(\\.I)( )(\\d)*")) {
                    StringBuilder stringB;
                    q_line = bRead.readLine();
                    query_Id = String.valueOf(q_Id);
                    while (q_line != null && !q_line.matches("(\\.I)( )(\\d)*")) {
                        if (q_line.matches("(\\.W)")) {
                            stringB = new StringBuilder();
                            q_line = bRead.readLine();
                            while (q_line != null && !q_line.matches("(\\.I)( )(\\d)*")) {
                                stringB.append(q_line).append(" ");
                                q_line = bRead.readLine();
                            }
                            query_content = stringB.toString();
                            String escapedQ_content = QueryParser.escape(query_content.trim());
                            // Creating query and search
                            Query query = qParser.parse(escapedQ_content);
                            TopDocs marks = idxSearch.search(query, 50);
                            for (ScoreDoc score_D : marks.scoreDocs) {
                                Document resultDoc = idxSearch.doc(score_D.doc);
                                writerQuery.write(query_Id + " q0 " + resultDoc.get("Id") + " " + "mm" + " " + score_D.score + " " + "runID" + "\n");
                            }
                        }
                       
                    }
                q_Id++;
                    
                }
            }
            writerQuery.close();
            bRead.close();
            idxRead.close();
        }
    }
}