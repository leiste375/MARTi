/*
 * Author: Richard M. Leggett
 * © Copyright 2021 Earlham Institute
 */
package uk.ac.earlham.marti.classify;

import uk.ac.earlham.lcaparse.*;
import uk.ac.earlham.marti.core.*;
import uk.ac.earlham.marti.blast.*;
import uk.ac.earlham.marti.schedule.*;
import uk.ac.earlham.marti.amr.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

/**
 * Classify reads based on alignment results, using Lowest Common Ancestor algorithm.
 * 
 * @author Richard M. Leggett
 */
public class ReadClassifier {
    private Taxonomy taxonomy;
    private LCAParseOptions lcaParseOptions = new LCAParseOptions();
    private MARTiEngineOptions options;
    private Hashtable<Integer, ReadClassifierItem> files = new Hashtable<Integer, ReadClassifierItem>();
    private ArrayList<String> summaryFiles = new ArrayList<String>();
    private Hashtable<String, Integer> barcodes = new Hashtable<String, Integer>();
    //private Hashtable<Integer, Integer> ntVfdbPair = new Hashtable<Integer, Integer>();
    private Hashtable<Integer, BlastDependencies> blastDependencies = new Hashtable<Integer, BlastDependencies>();
    private MARTiPendingTaskList pendingAnalysisTasks = null;
    private int filesProcessed = 0;
    private int fileCount = 0;
    
    public ReadClassifier(MARTiEngineOptions o) {
        options = o;
    }
 
    public synchronized void initialise() {                    
        lcaParseOptions = new LCAParseOptions(options.getTaxonomyDirectory(), options.getAccessionMap(), "nanook", true, options.getLCAMaxHits(), options.getLCAScorePercent(), options.getLCAMinIdentity(), options.getLCAMinQueryCoverage(), options.getLCAMinCombinedScore(), options.getLCAMinLength());
        taxonomy = new Taxonomy(options, lcaParseOptions, options.getTaxonomyDirectory() + "/nodes.dmp", options.getTaxonomyDirectory() + "/names.dmp"); 
    }
    
    public synchronized void addFile(String blastProcessName, int i, String queryFilename, String blastFilename, String logFilename, String classifyPrefix) {
        boolean ignoreThis = false;
        
        if (options.runBlastCommand() == false) {
            File f = new File(blastFilename);
            if (!f.exists()) {
                options.getLog().println("dontrunblast - file "+blastFilename+" doesn't exist, so ignoring");
                ignoreThis = true;
            }
        }

        if (ignoreThis == false) {
            files.put(i, new ReadClassifierItem(blastProcessName, i, queryFilename, blastFilename, logFilename, classifyPrefix));
            fileCount++;

            if (classifyPrefix.contains("barcode")) {
                String bcString = classifyPrefix.substring(classifyPrefix.indexOf("barcode"), classifyPrefix.indexOf("barcode")+9);
                if (!barcodes.containsKey(bcString)) {
                    barcodes.put(bcString, 1);
                }
            }
        }
    }
    
    public boolean checkBLASTCompleted(ReadClassifierItem f, int exitValue) {
        String blastLogFilename = f.getLogFile();
        
        options.getLog().println("Checking BLAST log for errors "+blastLogFilename);
        options.getLog().println("  Exit value was "+exitValue);
        
        if (exitValue != 0) {
            return false;
        }

        return true;
    }
    
//    /**
//    * Add a parsed BLAST chunk to the results list. 
//    *
//    * @param  bc   barcode index (or 0 if not barcoded)
//    * @param  pfp  LCAFileParser object of the parsed file
//    */
//    private boolean checkForVfdbResults(SimpleJobScheduler js, int id, ReadClassifierItem f) {
//        boolean rc = true;
//        
//        if (f.getBlastProcessName().equalsIgnoreCase("nt")) {        
//            if (options.getVFDBBlastProcess() != null) {
//                if (ntVfdbPair.containsKey(id)) {
//                    int vfdbId = ntVfdbPair.get(id);
//                    if (!js.checkJobCompleted(vfdbId)) {
//                        rc = false;
//                        options.getLog().println("Waiting for VFDB on jobs "+id+" "+vfdbId);
//                    }
//                } else {
//                    System.out.println("Error: no Vfdb pair for "+id);
//                    System.exit(1);
//                }
//            }
//        }
//        
//        return rc;
//    }    
    
    public int getChunkNumber(String filename) {
        int chunkNumber = -1;
        File f = new File(filename);
        String leafName = f.getName();
        if (leafName.lastIndexOf('.') != -1) {
            String filePrefix = leafName.substring(0, leafName.lastIndexOf('.'));
            int lastUnderscore = filePrefix.lastIndexOf('_');
            
            if ((lastUnderscore != -1) || (lastUnderscore < (filePrefix.length() - 1))) {
                chunkNumber = Integer.parseInt(filePrefix.substring(lastUnderscore+1));
            }
        }        
        
        return chunkNumber;
    }
    
    public synchronized void checkForFilesToClassify() {
        JobScheduler js = options.getJobScheduler();
        Set<Integer> asSet = files.keySet();
        Integer[] ids = asSet.toArray(new Integer[asSet.size()]);        

        options.getLog().println(MARTiLog.LOGLEVEL_CHECKFORFILESTOCLASSIFY, "In checkForFilesToClassify - size "+ids.length);
        
        for (int i=0; i<ids.length; i++) {
            int thisId = ids[i];
            ReadClassifierItem f = files.get(thisId);
            
            // Check if job completed
            if (js.checkJobCompleted(thisId)) {
                // Check if BLAST completed ok
                if (checkBLASTCompleted(f, js.getExitValue(thisId))) {                        
                    // Different BLAST types need handling differently...
                    if (f.getBlastProcessName().equalsIgnoreCase("VFDB")) {
                        // If VFDB file, can parse it
                        options.getLog().println("Parsing VFDB output " + f.getBlastFile());
                        
                        // Remove from list
                        filesProcessed++;
                        files.remove(thisId);
                        options.getProgressReport().incrementChunksParsedCount();
                    } else if (f.getBlastProcessName().equalsIgnoreCase("card")) {
                        // If CARD file, we'll process later...
                        options.getLog().println("Got CARD output " + f.getBlastFile() + " but will process later.");
                        // Remove from list
                        filesProcessed++;
                        files.remove(thisId);
                        options.getProgressReport().incrementChunksParsedCount();
                    } else if (f.getBlastProcessName().equalsIgnoreCase(options.getClassifyingBlastName())) {
                        // If nt file, can only parse it if dependencies have been met
                        if (blastDependencies.containsKey(thisId)) {
                            BlastDependencies bd = blastDependencies.get(thisId);
                            
                            if (bd.dependenciesMet()) {
                                options.getLog().println("Running parse on " + f.getBlastFile());
                                options.getLog().println("              to " + f.getClassifierPrefix());

                                long startTime = System.nanoTime();
                                long timeDiff;
                                int barcode = options.getBarcodeFromPath(f.getBlastFile());
                                SampleMetaData md = options.getSampleMetaData(barcode);

                                options.getLog().println("Got sample metadata");
                                LCAFileParser pfp = new LCAFileParser(taxonomy, lcaParseOptions, null, options.runningCARD());
                                
                                String summaryFilename = f.getClassifierPrefix() + "_summary.txt";
                                String perReadFilename = f.getClassifierPrefix() + "_perread.txt";
                                options.getLog().println("Got LCAFileParse instance, now parsing");
                                int readsWithHits = pfp.parseFile(f.getBlastFile());
                                options.getLog().println("Parsed... now removing poor alignments");
                                int readsRemoved = pfp.removePoorAlignments();

                                options.getLog().println("Adding to reads classified");
                                md.addToReadsClassified(readsWithHits);
                                options.getLog().println("Marking poor alignments");
                                md.markPoorAlignments(readsRemoved);
                                options.getLog().println("Registering chunks");
                                md.registerChunkAnalysed(f.getQueryFile());

                                timeDiff = (System.nanoTime() - startTime) / 1000000;
                                options.getLog().println("Timing: LCA parse on " + f.getBlastFile() + " completed in " + timeDiff + " ms");
                                
                                startTime = System.nanoTime();
                                pfp.writeResults(summaryFilename, perReadFilename);
                                timeDiff = (System.nanoTime() - startTime) / 1000000;
                                options.getLog().println("Written " + summaryFilename);
                                options.getLog().println("Written " + perReadFilename);
                                options.getLog().println("Timing: Results file for " + f.getBlastFile() + " written in " + timeDiff + " ms");
                                
                                summaryFiles.add(summaryFilename);

                                int fastaChunkNumber = getChunkNumber(f.getBlastFile());
                                int chunkNumberByOrderCompleted = options.getResults().addChunk(barcode, pfp);
                                
                                // Write files for min support 0, 0.1, 1 and 2
                                startTime = System.nanoTime();
                                options.getResults().writeTree(barcode, 0);
                                options.getResults().storeAccumulationData(barcode, fastaChunkNumber, chunkNumberByOrderCompleted, md.getReadsAnalysed(), md.getLastChunkAnalysedTime(), 0);
                                options.getResults().writeAccumulationJson(barcode, 0);                                

                                options.getResults().writeTree(barcode, 0.1);
                                options.getResults().storeAccumulationData(barcode, fastaChunkNumber, chunkNumberByOrderCompleted, md.getReadsAnalysed(), md.getLastChunkAnalysedTime(), 0.1);
                                options.getResults().writeAccumulationJson(barcode, 0.1);                                

                                options.getResults().writeTree(barcode, 1);
                                options.getResults().storeAccumulationData(barcode, fastaChunkNumber, chunkNumberByOrderCompleted, md.getReadsAnalysed(), md.getLastChunkAnalysedTime(), 1);
                                options.getResults().writeAccumulationJson(barcode, 1);                                

                                options.getResults().writeTree(barcode, 2);
                                options.getResults().storeAccumulationData(barcode, fastaChunkNumber, chunkNumberByOrderCompleted, md.getReadsAnalysed(), md.getLastChunkAnalysedTime(), 2);
                                options.getResults().writeAccumulationJson(barcode, 2);                                

                                timeDiff = (System.nanoTime() - startTime) / 1000000;
                                options.getLog().println("Timing: LCA tree and accumulation " + f.getBlastFile() + " completed in " + timeDiff + " ms");
                                
                                // Remove from list only if we were able to process it
                                filesProcessed++;
                                files.remove(thisId);
                                options.getProgressReport().incrementChunksParsedCount();
                                
                                // Handle AMR?
                                if (options.runningCARD()) {
                                    options.getLog().println("Time to run parse CARD and do walkout");
                                    String cardFilename = bd.getDependencyFile("card");
                                    if (cardFilename != null) {
                                        options.getLog().println("CARD filename: " + cardFilename);
                                        options.getLog().println("  nt filename: " + f.getBlastFile());

                                        AMRAnalysisTask mat = new AMRAnalysisTask(barcode, fastaChunkNumber, chunkNumberByOrderCompleted, bd.getDependencyFile("card"), f.getBlastFile(), f.getQueryFile());
                                        options.getProgressReport().incrementAnalysisSubmitted();
                                        pendingAnalysisTasks.addPendingTask(mat);
                                    } else {
                                        System.out.println("Error: couldn't get CARD filename\n");
                                        System.exit(1);
                                    }
                                }
                                
                                md.writeSampleJSON(false);
                                
                                // Handle accumulation
                            } else {
                                options.getLog().println("Not got dependency results yet for " + f.getBlastFile());
                            }
                        } else {
                            options.getLog().println("Id not found in blastDependencies: "+thisId);
                            System.out.println("Id not found in blastDependencies: "+thisId);
                        }
                    } else {                
                        System.out.println("Warning: Unexpected blast process " + f.getBlastProcessName());
                        options.getLog().println("Warning: Unexpected blast process " + f.getBlastProcessName());
                    }
                } else {
                    System.out.println("Error: Failed BLAST "+f.getBlastFile());
                    options.getLog().println("Error: Failed BLAST "+f.getBlastFile());
                }
            } else {    
                options.getLog().println(MARTiLog.LOGLEVEL_NOTCOMPLETED, "Not completed " + f.blastProcessName + " - " + f.blastFile + " - " + f.getJobId());
            }
        }        
    }
    
    public int getPendingClassificationCount() {
        options.getLog().println("Pending classification: "+files.size());
        return files.size();
    }

    public int getChunksProcessed() {
        return filesProcessed;
    }
    
    public void writeSummaries() {
        System.out.println("");
        
        if (barcodes.size() == 0) {
            writeSummary(null);
        } else {
            Set<String> bcodes = barcodes.keySet();
            for (String bc: bcodes) {
                writeSummary(bc);
            }
        }
    }
    
    public void writeSummary(String barcodeString) {
        Hashtable<Long, ShortTaxon> taxa = new Hashtable<Long, ShortTaxon>();
        int totalReadCount = 0;

        if (barcodeString == null) {
            System.out.println("Producing summary...");
        } else {
            System.out.println("Producing summary for "+barcodeString);
        }
        
        for (int i=0; i<summaryFiles.size(); i++) {
            String filename = summaryFiles.get(i);
            boolean processFile = false;
            
            if (barcodeString == null) {
                processFile = true;
            } else {
                if (filename.contains("barcode")) {
                    String bcString = filename.substring(filename.indexOf("barcode"), filename.indexOf("barcode")+9);
                    if (bcString.compareTo(barcodeString) == 0) {
                        processFile = true;
                    }
                }
            }
            
            if (processFile) {
                System.out.println("  " + filename);
                try {
                    BufferedReader br = new BufferedReader(new FileReader(filename));
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] tokens = line.split("\t");
                        int count = Integer.parseInt(tokens[0]);
                        long taxaId = Long.parseLong(tokens[2]);
                        String taxaPath = tokens[3];

                        if (taxaId >= 0) {
                            ShortTaxon ts;
                            if (taxa.containsKey(taxaId)) {
                                ts = taxa.get(taxaId);
                            } else {
                                ts = new ShortTaxon(taxaId, taxaPath, 0);
                            }
                            ts.increaseCount(count);
                            taxa.put(taxaId, ts);
                            totalReadCount += count;
                        }
                    }
                    br.close();                
                } catch (Exception e) {
                    System.out.println("Exception:");
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
        
        writeSummaryTextFile(taxa, barcodeString);        
    }
     
    public void writeSummaryTextFile(Hashtable<Long, ShortTaxon> taxa, String barcodeString) {            
        String summaryFilename;
        
        
        if (barcodeString == null) {        
            summaryFilename = options.getLCAParseDirectory() + File.separator + "classification_summary.txt";
        } else {
            summaryFilename = options.getLCAParseDirectory() + File.separator + "classification_summary_"+barcodeString+".txt";
        }
        
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(summaryFilename));
            Set<Long> taxaIds = taxa.keySet();
            for (Long i: taxaIds) {
                ShortTaxon ts = taxa.get(i);
                pw.println(ts.getCount() + "\t" + i + "\t" + ts.getTaxonPath());
            }
            pw.close();
        } catch (Exception e) {
            System.out.println("Exception:");
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Summary written to "+summaryFilename);
        
    }
    
    public void createBlastDependency(String primaryDb, String dbFilename, int jobId) {
        if (blastDependencies.containsKey(jobId)) {
            options.getLog().println("Duplicate jobId in createBlastDependency - shouldn't be here!");
            System.out.println("Duplicate jobId in createBlastDependency - shouldn't be here!");
            System.exit(1);
        } else {
            BlastDependencies bd = new BlastDependencies(options, primaryDb, dbFilename, jobId);
            blastDependencies.put(jobId, bd);
        }
    }
    
    public void addBlastDependency(int nt, String dependencyDb, String dbFilename, int dependencyId) {
        BlastDependencies bd;
        
        options.getLog().println("Adding BLAST dependency: nt="+nt+" DB="+dependencyDb+" id="+dependencyId);
        
        if (blastDependencies.containsKey(nt)) {
            bd = blastDependencies.get(nt);
            bd.addDependency(dependencyDb, dbFilename, dependencyId);
        } else {
            options.getLog().println("Error: no dependency set up for id "+nt);
            System.out.println("Error: no dependency set up for id "+nt);
            System.exit(1);
        }
    }
    
//    public void linkNTToVFDBResults(int nt, int vfdb) {
//        ntVfdbPair.put(nt, vfdb);
//    }    
    
    public Taxonomy getTaxonomy() {
        return taxonomy;
    }
    
    public void setPendingTaskList(MARTiPendingTaskList ptl) {
        options.getLog().println("Got pending task list");
        pendingAnalysisTasks = ptl;
    }

    public LCAParseOptions getLCAParseOptions() {
        return lcaParseOptions;
    }
}


