/*
 * Author: Richard M. Leggett
 * © Copyright 2021 Earlham Institute
 */
package uk.ac.earlham.marti.core;

import uk.ac.earlham.lcaparse.SimplifiedRank;
import uk.ac.earlham.marti.amr.AMRResults;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import uk.ac.earlham.lcaparse.LCAFileParser;
import uk.ac.earlham.lcaparse.LCAHitSet;
import uk.ac.earlham.lcaparse.Taxonomy;
import uk.ac.earlham.lcaparse.TaxonomyNode;
import javax.json.*;
import javax.json.stream.JsonGenerator;

/**
 * Represent overall results (essentially, taxonomic classifications) for all barcodes.
 * 
 * @author Richard M. Leggett
 */
public class MARTiResults {
    private MARTiEngineOptions options = null;
    //private Hashtable<Integer, MARTiResultsSample> sampleResults = new Hashtable<Integer, MARTiResultsSample>();
    private Hashtable<Integer, Integer> chunkCount = new Hashtable<Integer, Integer>();
    private Taxonomy taxonomy = null;
    private Hashtable<Integer, AMRResults> amrResults = new Hashtable<Integer, AMRResults>();
    private Hashtable<Integer, ArrayList<String>> fileOrder = new Hashtable<Integer, ArrayList<String>>();
    private Hashtable<Integer, TaxaAccumulation> taxaAccumulation = new Hashtable<Integer, TaxaAccumulation>();
    private SimplifiedRank mr = new SimplifiedRank();
    
    /**
    * Class constructor.
    * 
    * @param  o  global MARTiEngineOptions object
    */
    public MARTiResults(MARTiEngineOptions o) {
        options = o;
    }

    /**
    * Add a parsed BLAST chunk to the results list. 
    *
    * @param  bc   barcode index (or 0 if not barcoded)
    * @param  pfp  LCAFileParser object of the parsed file
    * @return chunk number 
    */
    public int addChunk(int bc, LCAFileParser pfp) {
        int fileCount = 0;
        
        options.getLog().println("MARTiResults received file for barcode "+bc);
        //MARTiResultsSample sample;
        taxonomy = options.getReadClassifier().getTaxonomy();

        //if (sampleResults.containsKey(bc)) {
        //    sample = sampleResults.get(bc);
        //} else {
        //    sample = new MARTiResultsSample(bc, options);
        //    sampleResults.put(bc, sample);
        //}
        
        //sample.addFile(pfp);    
        
        Hashtable<String, LCAHitSet> hitsByQuery = pfp.getHitsByQuery();
        Set<String> keys = hitsByQuery.keySet();        
        for (String queryName : keys) {
            LCAHitSet hs = hitsByQuery.get(queryName);      
            long taxon = hs.getAssignedTaxon();            
            taxonomy.countRead(bc, taxon);            
        }
        
        if (chunkCount.containsKey(bc)) {
            fileCount = chunkCount.get(bc);
        }        
        fileCount++;        
        chunkCount.put(bc, fileCount);

        ArrayList<String> l;
        if (fileOrder.containsKey(bc)) {
            l = fileOrder.get(bc);        
        } else {
            l = new ArrayList<String>();
            fileOrder.put(bc, l);
        }
        l.add(pfp.getLastParsedFilename());
        
        return fileCount;
    }
        
    /**
    * Output a taxon node to the JSON. 
    *
    * @param  bc    barcode index (or 0 if not barcoded)
    * @param  n     the node to output
    * @param  jf    the JSON file we're writing to
    * @param  comma true if there should be a comma after this element
    */
    private void outputNode(int bc, TaxonomyNode n, JsonObjectBuilder treeBuilder, PrintWriter pwAssignments, boolean useLCA) {                
        if (n != null) {
            ArrayList<TaxonomyNode> children = n.getChildren();
            String ncbiRankString = n.getRankString();
            int summedCount;
            int assignedCount;
            int rank = 0;

            if (useLCA) {
                summedCount = n.getLCASummed(bc);
                assignedCount = n.getLCAAssigned(bc);
            } else {
                summedCount = n.getSummed(bc);
                assignedCount = n.getAssigned(bc);
            }
            
            // If root node, need to add unclassified count
            if (n.getId() == 1) {
                summedCount += options.getSampleMetaData(bc).getReadsUnclassified();
            }            
                     
            rank = mr.getRankFromString(ncbiRankString);
                        
            treeBuilder.add("name", taxonomy.getNameFromTaxonId(n.getId()));
            treeBuilder.add("rank", rank);
            treeBuilder.add("ncbiRank", ncbiRankString);
            treeBuilder.add("ncbiID", n.getId());
            treeBuilder.add("value", assignedCount);
            treeBuilder.add("summedValue", summedCount);
            
            pwAssignments.print(taxonomy.getNameFromTaxonId(n.getId()) + ",");
            pwAssignments.println(n.getId() + "," + assignedCount + "," + summedCount);
            
            // Now output children as array
            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for (int i=0; i<children.size(); i++) {
                TaxonomyNode c = children.get(i);
                int childSummarisedCount;
                
                if (useLCA) {
                    childSummarisedCount = c.getLCASummed(bc);
                } else {
                    childSummarisedCount = c.getSummed(bc);
                }
                
                if (childSummarisedCount > 0) {
                    JsonObjectBuilder childBuilder = Json.createObjectBuilder();
                    outputNode(bc, c, childBuilder, pwAssignments, useLCA);
                    arrayBuilder.add(childBuilder);                  
                }                
                
            }        

            // Add unclassified to root node
            if (n.getId() == 1) {
                JsonObjectBuilder unclassifiedBuilder = Json.createObjectBuilder();
                JsonArrayBuilder unclassifiedArrayBuilder = Json.createArrayBuilder();
                int unclassifiedCount = options.getSampleMetaData(bc).getReadsUnclassified();
                unclassifiedBuilder.add("name", "unclassified");
                unclassifiedBuilder.add("rank", 0);
                unclassifiedBuilder.add("ncbiRank", "no rank");
                unclassifiedBuilder.add("ncbiID", 0);
                unclassifiedBuilder.add("value", unclassifiedCount);
                unclassifiedBuilder.add("summedValue", unclassifiedCount);
                unclassifiedBuilder.add("children", unclassifiedArrayBuilder);
                arrayBuilder.add(unclassifiedBuilder);                  
            }                                    

            treeBuilder.add("children", arrayBuilder);
        } else {
            System.out.println("Error: null node passed to outputNode!");
        }
    }    
    
    /**
    * Write JSON file. 
    *
    * @param  bc    barcode index (or 0 if not barcoded)
    */
    public synchronized void writeTree(int bc, double minSupport) {
        int fileCount = 0;
        String jsonFilename;
        String jsonFilenameFinal;
        String assignmentsFilename;
        String assignmentsFilenameFinal;
        PrintWriter pwAssignments = null;
                
        if (chunkCount.containsKey(bc)) {
            fileCount = chunkCount.get(bc);
        } else {
            System.out.println("Error: no chunk count found in writeJSONFile");
            System.exit(1);
        }
        
        if (bc > 0) {
            jsonFilename = options.getLCAParseDirectory() + File.separator + "tree_barcode" + bc + "_ch" + fileCount + "_ms" + minSupport + ".json";
            assignmentsFilename = options.getLCAParseDirectory() + File.separator + "assignments_barcode" + bc + "_ch" + fileCount + "_ms" + minSupport + ".csv";
        } else {
            jsonFilename = options.getLCAParseDirectory() + File.separator + "tree_ch" + fileCount + "_ms" + minSupport + ".json";
            assignmentsFilename = options.getLCAParseDirectory() + File.separator + "assignments_ch" + fileCount + "_ms" + minSupport + ".csv";
        }

        jsonFilenameFinal = options.getMARTiJSONDirectory(bc) + File.separator + "tree_ms" + minSupport + ".json";
        assignmentsFilenameFinal = options.getMARTiJSONDirectory(bc) + File.separator + "assignments_ms" + minSupport + ".csv";
        
        // Adjust for min support
        long startTime = System.nanoTime();
        // minSuppport of 100 is special case to output the non-LCA counts
        if (minSupport < 100) {
            taxonomy.adjustForMinSupport(bc, minSupport);
        }
        long timeDiff = (System.nanoTime() - startTime) / 1000000;
        options.getLog().println("Timing: Min support refactoring for barcode "+bc+" minSupport "+minSupport+" completed in "+timeDiff+" ms");


        // Open assignments file
        try {
            pwAssignments = new PrintWriter(new FileWriter(assignmentsFilename, false));
        } catch (Exception e) {
            System.out.println("Error in openPutativePathogenReadFile");
            e.printStackTrace();
            System.exit(1);
        }        
        
        options.getLog().printlnLogAndScreen("Writing MARTi tree JSON to "+jsonFilename);
        options.getLog().println("Writing assignments to "+assignmentsFilename);
        ArrayList<String> fo;
        if (fileOrder.containsKey(bc)) {
            fo = fileOrder.get(bc);
            for (int i=0; i<fo.size(); i++) {
                options.getLog().println("File "+i+": "+fo.get(i));
            }
        } else {
            System.out.println("Error: couldn't find file order for barcode "+bc);
        }

        // Create tree object
        JsonObjectBuilder treeBuilder = Json.createObjectBuilder();
        
        TaxonomyNode n = taxonomy.getNodeFromTaxonId(1L);

        // minSuppport of 100 is special case to output the non-LCA counts
        if (minSupport == 100) {
            outputNode(bc, n, treeBuilder, pwAssignments, false);
        } else {
            outputNode(bc, n, treeBuilder, pwAssignments, true);
        }
        
        // Build meta data
        JsonObjectBuilder metaBuilder = Json.createObjectBuilder();
        metaBuilder.add("martiVersion", MARTiEngine.VERSION_STRING);
        LocalDateTime date = LocalDateTime.now();
        String dateTimeString = date.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).toString();
        metaBuilder.add("fileWritten", dateTimeString);
                        
        JsonObjectBuilder fileBuilder = Json.createObjectBuilder();
        ArrayList<String> files = fileOrder.get(bc);
        for (int i=0; i<files.size(); i++) {
            fileBuilder.add(Integer.toString(i), files.get(i));
        }
        metaBuilder.add("blastFiles", fileBuilder);
        
        // Build top-level object
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("meta", metaBuilder);
        objectBuilder.add("tree", treeBuilder);
        JsonObject jsonObject = objectBuilder.build();        

        // Print it with pretty printing (pacing etc.)
        Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory writerFactory = Json.createWriterFactory(config);
        
        try {
            Writer writer = new StringWriter();
            writerFactory.createWriter(writer).write(jsonObject);
            String jsonString = writer.toString();
            
            PrintWriter pw = new PrintWriter(new FileWriter(jsonFilename));
            pw.write(jsonString);
            pw.close();  
            pwAssignments.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }        

        options.getLog().println("Finished MARTi tree JSON");
        options.copyFile(jsonFilename, jsonFilenameFinal);
        options.copyFile(assignmentsFilename, assignmentsFilenameFinal);        
    }    
    
    /**
    * Get AMR results object. 
    *
    * @param   bc  barcode for results
    * @return  AMR results object
    */
    public AMRResults getAMRResults(int bc) {
        AMRResults results = null;
        
        // Create AMRResults object for this barcode if it doesn't exist
        if (amrResults.containsKey(bc)) {
            results = amrResults.get(bc);
        } else {
            results = new AMRResults(options, bc);
            amrResults.put(bc, results);
        }
        
        return results;
    }    
    
    public void storeAccumulationData(int bc, int fastaChunkNumber, int chunkNumberByOrderCompleted, int nReadsAnalysed, int minsSinceStart, double minSupport) {
        TaxaAccumulation ta;

        if (taxaAccumulation.containsKey(bc)) {
            ta = taxaAccumulation.get(bc);
        } else {
            ta = new TaxaAccumulation(options, this, bc);
            taxaAccumulation.put(bc, ta);
        }
        
        ta.storeAccumulation(fastaChunkNumber, chunkNumberByOrderCompleted, nReadsAnalysed, minsSinceStart, minSupport);
    }
    
    public void writeAccumulationJson(int bc, double minSupport) {
        TaxaAccumulation ta;

        if (taxaAccumulation.containsKey(bc)) {
            ta = taxaAccumulation.get(bc);
            ta.writeJSON(minSupport);
        } else {
            options.getLog().printlnLogAndScreen("Error: can't find accumulation data for barcode "+bc);
        }
    }
}
