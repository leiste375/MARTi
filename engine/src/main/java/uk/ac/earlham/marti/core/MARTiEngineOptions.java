/*
 * Author: Richard M. Leggett
 * © Copyright 2021 Earlham Institute
 */
package uk.ac.earlham.marti.core;

import uk.ac.earlham.marti.classify.*;
import uk.ac.earlham.marti.watcher.*;
import uk.ac.earlham.marti.blast.*;
import uk.ac.earlham.marti.schedule.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Representation of program options and some global constants.
 * 
 * @author Richard M. Leggett
 */
public class MARTiEngineOptions implements Serializable {
    public static final boolean DEBUG_DONT_SUBMIT_JOB = false;
    private static final long serialVersionUID = MARTiEngine.SERIAL_VERSION;
    public final static int MAX_BARCODES = 100;
    public final static int MAX_KMER = 20000;
    public final static int MAX_READ_LENGTH = 1000000;
    public final static int MAX_READS = 1000000;
    public final static int FASTA = 1;
    public final static int FASTQ = 2;
    public final static int TYPE_TEMPLATE = 0;
    public final static int TYPE_COMPLEMENT = 1;
    public final static int TYPE_2D = 2;
    public final static int TYPE_ALL = -1;
    public final static int TYPE_INSERTION = 0;
    public final static int TYPE_DELETION = 1;
    public final static int TYPE_SUBSTITUTION = 2;
    public final static int READTYPE_COMBINED = 0;
    public final static int READTYPE_PASS = 1;
    public final static int READTYPE_FAIL = 2;
    public final static int MIN_ALIGNMENTS = 10;
    public final static int PROGRESS_WIDTH = 50;
    private MARTiProgress progressReport = new MARTiProgress(this);
    private String sampleDirectory = null;
    private String sampleName = null;
    private String schedulerName="local";
    private String configFile = null;
    private RawDataDirectory rawDataDir = null;
    private boolean processPassReads = true;
    private boolean processFailReads = true;
    private int maxReads = 0;
    private boolean process2DReads = true;
    private boolean processTemplateReads = true;
    private boolean processComplementReads = true;
    private boolean fixRandom = false;
    private long randomSeed = 0;
    private boolean extractingReads = false;
    private boolean convertingFastQ = false;
    private boolean aligningReads = false;
    private boolean parsingReads = false;
    private boolean blastingReads = false;
    private boolean classifyingReads = true;
    private boolean mergeFastaFiles = false;
    private boolean force = false;
    private boolean testMode = false;
    private double minQForPass = -1;
    private int maxSchedulerJobs = 4;
    private int runMode = 0;
    private int readFormat = FASTA;
    private int numThreads = 8; // For ThreadPoolExecutor
    private int fileWatcherTimeout = 10;
    private int readsPerMultiFastq = 1;
    private String jobQueue = "";
    private MARTiLog logFile = new MARTiLog();
    private String readsDir = "fast5";
    private String fastQConvertDir = "fastq_pass";
    private int returnValue = 0;
    private int readsPerBlast = 4000;
    private boolean clearLogsOnStart = true;
    private JobScheduler jobScheduler = null;
    private transient WatcherLog watcherReadLog = new WatcherLog(this);
    private transient WatcherLog watcherCardFileLog = new WatcherLog(this);
    private transient WatcherLog watcherntFileLog = new WatcherLog(this);
    private transient WatcherLog watcherCardCommandLog = new WatcherLog(this);
    private transient WatcherLog watcherntCommandLog = new WatcherLog(this);
    private transient ThreadPoolExecutor executor;
    private BlastHandler blastHandler = new BlastHandler(this);
    private ReadClassifier readClassifier = new ReadClassifier(this);
    private ArrayList<BlastProcess> blastProcesses = new ArrayList<BlastProcess>();
    private boolean isMac = false;
    private String meganCmdLine = "MEGAN";
    private String meganLicense = "MEGAN5-academic-license.txt";
    //private String meganCmdLine="source MEGAN-5.11.3 ; /tgac/software/testing/MEGAN/5.11.3/x86_64/xvfb-run -d MEGAN";
    //private String meganLicense="/tgac/software/testing/MEGAN/5.11.3/x86_64/megan/MEGAN5-academic-license.txt";
    private BarcodesList barcodesList = null;
    private int localSchedulerMaxJobs = 4;
    private boolean doingMeganMinSupport = false;
    private boolean doingMeganMinSupportPercent = true;
    private int meganMinSupport = 1;
    private double meganMinSupportPercent = 0.1;
    private boolean useXvfb = false;
    private String meganPropertiesFile = null;
    private int stopProcessingAfter = 0;
    private int timeLimit = 0;
    private boolean stopFlag = false;
    private boolean runBlastCommand = true;
    private boolean dontRunNt = false;
    private String taxonomyDir = null;
    private String accessionMapFile = "0";
    private int lcaMaxHits = 20;
    private double lcaScorePercent = 90;
    private int lcaMinIdentity = 60;
    private int lcaMinQueryCoverage = 0;
    private int lcaMinCombinedScore = 0;
    private int lcaMinLength = 100;
    private int readFilterMinQ = 8;
    private int readFilterMinLength = 500;
    private MARTiResults martiResults = new MARTiResults(this);
    private String resultsFile = null;
    private boolean initMode = false;
    private boolean writeConfigMode = false;
    private String initDir = null;
    private boolean haveReachedReadOrTimeLimit = false;
    private long startTime = System.nanoTime();
    private double walkoutMaxE = 0.001;
    private double walkoutMinID = 80.0;
    private int walkoutMinLength = 100;   
    private boolean runningCARD = false;
    private boolean autodeleteBlastFiles = false;
    private boolean autodeleteFastaChunks = false;
    private boolean autodeleteFastqChunks = false;
    private boolean autodeleteMetaMapsFiles = false;
    private String blastProcessNames = null;
    private String cardDBPath = null;
    private Hashtable<Integer, String> barcodeIDs = new Hashtable<Integer, String>();
    private Hashtable<Integer, SampleMetaData> metaData = new Hashtable<Integer, SampleMetaData>();
    private BlastProcess vfdbBlastProcess = null;    
    private HashMap<Integer, String> sampleIdByBarcode = new HashMap<Integer,String>();
    private MARTiEngineOptionsFile engineOptionsFile = new MARTiEngineOptionsFile(this);
    private String classifyingBlastName = null;
    
    public MARTiEngineOptions() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("mac os x")) { 
            isMac = true;
            System.out.println("Mac OS X detected");
            meganCmdLine="/Applications/MEGAN5.11.3/MEGAN.app/Contents/MacOS/JavaApplicationStub";
            meganLicense="/Applications/MEGAN5/MEGAN5-academic-license.txt";
        }        
                        
        engineOptionsFile.readOptionsFile();
    }
        
    public boolean isMac() {
        return isMac;
    }
    
    public String getMeganCmdLine() {
        return meganCmdLine;
    }
    
    public String getMeganLicense() {
        return meganLicense;
    }
        
    public void setReturnValue(int r) {
        returnValue = r;
    }
    
    public int getReturnValue() {
        return returnValue;
    }
    
    /**
     * Parse command line arguments.
     * @param args array of command line arguments
     */
    public void parseArgs(String[] args) {
        int i=0;
        
        if (args.length <= 1) {
            System.out.println("To run a MARTi analysis:");
            System.out.println("");
            System.out.println("    marti -config <file> [options]");
            System.out.println("");
            System.out.println("Options:");
            System.out.println("-init to enter initialisation mode and output version information.");
            //System.out.println("-t|-numthreads <number> specifies the number of threads to use for external processes (default 5)");
            //System.out.println("-force to force ignore warnings");
            System.out.println("-loglevel <int> to set the level of logging to logs/engine.txt from 0 (none) to " + MARTiLog.LOGLEVEL_MAX +" (maximum) (default "+MARTiLog.LOGLEVEL_DEFAULT+")");           
            //System.out.println("-timeout to set the number of seconds before giving up waiting for new reads (default 2)");
            System.out.println("-fixrandom <long> to fix the random number seed used for debugging");
            System.out.println("");
            System.out.println("Or to generate a new config file");
            System.out.println("");
            System.out.println("    marti -writeconfig <file> [options]");
            System.out.println("");
            System.exit(0);
        }
                                                
        while (i < (args.length)) {
            if (args[i].equalsIgnoreCase("-config")) {
                configFile = args[i+1];
                readProcessFile();
                i+=2;
            } else if (args[i].equalsIgnoreCase("-writeconfig")) {
                configFile = args[i+1];
                writeConfigMode = true;
                i+=2;
            } else if (args[i].equalsIgnoreCase("-test")) {
                testMode = true;
                i++;
            } else if (args[i].equalsIgnoreCase("-force")) {
                force = true;
                i++;
            } else if (args[i].equalsIgnoreCase("-maxreads")) {
                maxReads = Integer.parseInt(args[i+1]);
                i+=2;
            } else if (args[i].equalsIgnoreCase("-nofail") || args[i].equalsIgnoreCase("-passonly")) {
                processPassReads = true;
                processFailReads = false;  
                i++;
            } else if (args[i].equalsIgnoreCase("-nopass") || args[i].equalsIgnoreCase("-failonly")) {
                processPassReads = false;
                processFailReads = true;
                i++;
            } else if (args[i].equalsIgnoreCase("-fasta") || args[i].equalsIgnoreCase("-a")) {
                    readFormat = FASTA;
                i++;
            } else if (args[i].equalsIgnoreCase("-fastq") || args[i].equalsIgnoreCase("-q")) {
                    readFormat = FASTQ;
                i++;
            } else if (args[i].equalsIgnoreCase("-2donly")) {
                process2DReads = true;
                processTemplateReads = false;
                processComplementReads = false;
                i++;
            } else if ((args[i].equalsIgnoreCase("-1d")) || 
                       (args[i].equalsIgnoreCase("-templateonly")) ) {
                process2DReads = false;
                processTemplateReads = true;
                processComplementReads = false;
                i++;
            } else if (args[i].equalsIgnoreCase("-complementonly")) {
                process2DReads = false;
                processTemplateReads = false;
                processComplementReads = true;
                i++;                
            } else if (args[i].equalsIgnoreCase("-fixrandom")) {
                fixRandom = true;
                randomSeed = Long.parseLong(args[i+1]);
                i+=2;                
            } else if (args[i].equalsIgnoreCase("-numthreads") || args[i].equalsIgnoreCase("-t")) {
                System.out.println("WARNING: -numthreads is deprecated and shouldn't be used.");
                numThreads = 3 + Integer.parseInt(args[i+1]);
                i+=2;
            } else if (args[i].equalsIgnoreCase("-keeplogs")) {
                clearLogsOnStart = false;
                i++;
            } else if (args[i].equalsIgnoreCase("-mergereads")) {
                mergeFastaFiles = true;
                i++;
            } else if (args[i].equalsIgnoreCase("-minquality")) {
                minQForPass = Double.parseDouble(args[i+1]);
                i+=2;
            } else if (args[i].equalsIgnoreCase("-loglevel")) {
                int l = Integer.parseInt(args[i+1]);
                logFile.setLogLevel(l);
                i+=2;
            } else if (args[i].equalsIgnoreCase("-dontrunblast")) {
                runBlastCommand = false;
                i++;
            } else if (args[i].equalsIgnoreCase("-dontrunnt")) {
                dontRunNt = true;
                i++;
            } else if (args[i].equalsIgnoreCase("-init")) {
                initMode = true;
                i++;
            } else if (args[i].equalsIgnoreCase("-rawdir")) {
                rawDataDir = new RawDataDirectory(args[i+1]);
                i+=2;                
            } else if (args[i].equalsIgnoreCase("-sampledir")) {
                sampleDirectory = args[i+1];
                i+=2;                
            } else if (args[i].equalsIgnoreCase("-runname")) {
                sampleName = args[i+1];
                i+=2;                
            } else if (args[i].equalsIgnoreCase("-barcodes")) {
                if (args[i+1].length() > 0) {
                    barcodesList = new BarcodesList(args[i+1]);
                }
                i+=2; 
            } else if (args[i].equalsIgnoreCase("-blast")) {                
                blastProcessNames = args[i+1];
                i+=2; 
            } else {
                int iCurrent = i;
                
                if (i == iCurrent) {
                    System.out.println("Unknown parameter: " + args[i]);
                    System.exit(1);
                }
            }            
        }
                 
        if (configFile == null) {
            System.out.println("Error: you must specify a config file");
            System.exit(1);
        }
        
        if (writeConfigMode == true) {
            MARTiConfigFile mcf = new MARTiConfigFile(this);
            mcf.writeConfigFile(configFile);
            return;
        } else if (initMode == true) {
            if (initDir == null) {
                System.out.println("Error: you must specify an init directory in the config file");
                System.exit(1);
            }
        } else {            
            if (rawDataDir == null) {
                System.out.println("Error: you must specify a raw data directory in the config file");
                System.exit(1);
            } 

            if (rawDataDir.checkDirExists() == false) {
                System.exit(1);
            }        

            if (sampleName == null) {
                System.out.println("Error: you must specify a sample name in the config file");
                System.exit(1);
            }
            
            if (this.isClassiftingReads()) {
                if (taxonomyDir == null) {
                    System.out.println("Error: you must specify a TaxonomyDir in the config file if running a classifier.");
                    System.exit(1);
                }
            }

            if (sampleDirectory == null) {
                System.out.println("Error: You must specify a sample");
                System.exit(1);
            }

            if ((schedulerName.equalsIgnoreCase("local") || (schedulerName.equals(""))) || (schedulerName.equals("debug"))) {
                jobScheduler = new SimpleJobScheduler(maxSchedulerJobs, this);
                jobScheduler.setMaxJobs(localSchedulerMaxJobs);
                if (schedulerName.equals("debug")) {
                    jobScheduler.setDontRunCommand();
                }
            } else if (schedulerName.equalsIgnoreCase("slurm")) {
                jobScheduler = new SlurmScheduler(this);
            }
            
            if(resultsFile != null) {
                Path path = Paths.get(resultsFile);
                if(!Files.exists(path.getParent())) {
                    System.out.println("Error: Path to Results file does not exist.");
                    System.out.println(resultsFile);
                    System.exit(1);
                }
            }
            
            System.out.println("Number of cores: "+Runtime.getRuntime().availableProcessors()+"\n");
                
            executor = new ThreadPoolExecutor(numThreads, numThreads, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        }
                 
        String cmdLineArgs="";
        for (int j=0; j<args.length; j++) {
            if (j>0) {
                cmdLineArgs+=" ";
            }
            cmdLineArgs+=args[j];
        }
        logFile.println("Arguments: "+cmdLineArgs);
    }    
    
    public void createSampleDirectory() {
        File s = new File(sampleDirectory);
        if (!s.exists()) {
            System.out.println("Warning: sample directory doesn't exist. Creating.");
            s.mkdir();
            if (!s.exists()) {
                System.out.println("Error: failed to create sample directory.");
                System.exit(1);
            }
        }

        if (!s.isDirectory()) {
            System.out.println("Error: sample doesn't point to a directory");
            System.exit(1);
        }

        sampleDirectory = s.getAbsolutePath();
            
        checkAndMakeDirectory(this.getLogsDir());
        logFile.open(this.getLogsDir() + "/engine.txt");        
    }
            
    public void setReadFormat(int f) {
        readFormat = f;
        //System.out.println("Read format "+f);
    }
        
    /**
     * Get sample name.
     * @return name String
     */
    public String getSampleName() {
        return sampleName;
    }
                
    /**
     * Get a type string (Template, Complement, 2D) from an integer.
     * @param n integer to convert
     * @return type String
     */
    public static String getTypeFromInt(int n) {
        String typeString;
        
        switch(n) {
            case TYPE_TEMPLATE: typeString = "Template"; break;
            case TYPE_COMPLEMENT: typeString = "Complement"; break;
            case TYPE_2D: typeString = "2D"; break;
            default: typeString = "Unknown"; break;
        }
        
        return typeString;
    }
    
    public static String getPassFailFromInt(int n) {
        String typeString;
        
        switch(n) {
            case READTYPE_PASS: typeString = "pass"; break;
            case READTYPE_FAIL: typeString = "fail"; break;
            default: typeString = "Unknown"; break;
        }
        
        return typeString;
    }

    /**
     * Get an error type string (Insertion, Deletion, Substitution) from an integer.
     * @param n error type integer
     * @return type String
     */
    public static String getErrorTypeFromInt(int n) {
        String typeString;
        
        switch(n) {
            case TYPE_INSERTION: typeString = "Insertion"; break;
            case TYPE_DELETION: typeString = "Deletion"; break;
            case TYPE_SUBSTITUTION: typeString = "Substitution"; break;
            default: typeString = "Unknown"; break;
        }
        
        return typeString;
    }
            
    public String getSampleDirectory() {
        return sampleDirectory;
    }

    public String getFastaDir() {
        return sampleDirectory + File.separator + "fasta";
    }

    public String getFastqDir() {
        return sampleDirectory + File.separator + "fastq";
    }    
    
    public String getFast5Dir() {
        // Check for full path
        if ((readsDir.startsWith("/")) || (readsDir.startsWith("~")) || (readsDir.startsWith("."))) {
            return readsDir;
        } else {
            return sampleDirectory + File.separator + readsDir;
        }
    }
    
    public String getFastQConvertDir() {
        return fastQConvertDir;
    }
    
    /**
     * Get FASTA directory.
     * @return directory name as String
     */
    public String getReadDir() {
        String dir;
        
        if (readFormat == FASTQ) {
            dir = getFastqDir();
        } else {
            dir = getFastaDir(); 
        }
                
        return dir;
    } 
    
    public String getExpectedReadFormat() {
        String format;
        
        if (readFormat == FASTQ) {
            format = "FASTQ";
        } else {
            format = "FASTA"; 
        }
        
        return format;
    }
    
    /**
     * Get logs directory.
     * @return directory name as String
     */
    public String getLogsDir() {
        return sampleDirectory + File.separator + "logs";
    } 
    
    public String getLCAParseDirectory() {
        return sampleDirectory + File.separator + "lcaparse";
    }

    public String getMARTiDirectory() {
        return sampleDirectory + File.separator + "marti";
    }
    
    public String getAMRDirectory() {
        return sampleDirectory + File.separator + "amr";
    }
    
    /**
     * Check if processing "pass" reads.
     * @return true to process
     */
    public boolean isProcessingPassReads() {
        return processPassReads;
    }

    /**
     * Check if processing "fail" reads.
     * @return true to process
     */
    public boolean isProcessingFailReads() {
        return processFailReads;
    }
    
    public boolean isProcessingComplementReads() {
        return processComplementReads;
    }
    
    public boolean isProcessingTemplateReads() {
        return processTemplateReads;
    }

    public boolean isProcessing2DReads() {
        return process2DReads;
    }
    
    public boolean isProcessingReadType(int type) {
        boolean r = false;
        
        switch(type) {
            case TYPE_ALL:
                r = true;
                break;
            case TYPE_TEMPLATE:
                r = processTemplateReads;
                break;
            case TYPE_COMPLEMENT:
                r = processComplementReads;
                break;
            case TYPE_2D:
                r = process2DReads;
                break;
        }         
        
        return r;
    }
    
    public int getNumberOfTypes() {
        int t = 0;
        if (processTemplateReads) t++;
        if (processComplementReads) t++;
        if (process2DReads) t++;
        return t;
    }
                   
    /**
     * Get maximum number of reads (used for debugging)
     * @return maximum number of reads
     */
    public int getMaxReads() {
        return maxReads;
    }
    
    public int getReadFormat() {
        return readFormat;
    }
        
    public int getRunMode() {
        return runMode;
    }
            
    public String getSchedulerName() {
        return schedulerName;
    }
    
    public int getNumberOfThreads() {
        return numThreads;
    }
    
    public String getQueue() {
        return jobQueue;
    }
    
    public MARTiLog getLog() {
        return logFile;
    }
    
    public boolean isBarcoded() {
        return barcodesList == null ? false:true;
    }
                                    
    public WatcherLog getWatcherReadLog() {
        return watcherReadLog;
    }

    public WatcherLog getWatcherCardFileLog() {
        return watcherCardFileLog;
    }
    
    public WatcherLog getWatcherCardCommandLog() {
        return watcherCardCommandLog;
    }

    public WatcherLog getWatcherntFileLog() {
        return watcherntFileLog;
    }
    
    public WatcherLog getWatcherntCommandLog() {
        return watcherntCommandLog;
    }    
    
    public boolean clearLogsOnStart() {
        return clearLogsOnStart;
    }
    
    public int getReadsPerMultiFastq() {
        return readsPerMultiFastq;
    }

    public int getReadsPerBlast() {
        return readsPerBlast;
    }
    
    public ThreadPoolExecutor getThreadExecutor() {
        return executor;
    }
    
    public boolean keepRunning() {
        return true;
    }
    
    public boolean isExtractingReads() {
        return extractingReads;
    }
    
    public boolean isConvertingFastQ() {
        return convertingFastQ;
    }
    
    public boolean isAligningRead() {
        return aligningReads;
    }

    public boolean isParsingRead() {
        return parsingReads;
    }    
    
    public boolean isBlastingRead() {
        return blastingReads;
    }
    
    public int getFileWatcherTimeout() {
        return fileWatcherTimeout;
    }

    public void checkAndMakeDirectory(String dir) {
        File f = new File(dir);
        if (f.exists()) {
            if (!f.isDirectory()) {
                System.out.println("Error: " + dir + " is a file, not a directory!");
                System.exit(1);
            }
        } else {
            this.getLog().println("Making directory " + dir);
            f.mkdir();
        }
    }      

    private void checkAndMakeDirectoryWithChildren(String dirname) {
        checkAndMakeDirectory(dirname);
        for (int t=0; t<3; t++) {
            if (this.isProcessingReadType(t)) {
                checkAndMakeDirectory(dirname + File.separator + MARTiEngineOptions.getTypeFromInt(t));
            }
        }        
    }
    
    // Directory structure
    // fast5
    //     - pass
    //         - BC01
    //         - BC02
    //     - fail
    //         - unaligned
    // fasta
    //     - pass
    //         - BC01
    //             - 2D
    //             - Template
    //             - Complement
    //         - BC02
    //         ...
    //     - fail
    public void makeDirectories() {
        checkAndMakeDirectory(this.getLogsDir());
        
        if (this.isExtractingReads()) {
            checkAndMakeDirectory(this.getReadDir());
            
            //if (this.isNewStyleDir()) {
            //    for (int i=READTYPE_PASS; i<=READTYPE_FAIL; i++) {
            //        String pf = NanoOKOptions.getPassFailFromInt(i);
            //        checkAndMakeDirectoryWithChildren(this.getReadDir() + File.separator + pf);
            //        if (this.processSubdirs()) {
            //            File inputDir = new File(this.getFast5Dir());
            //            File[] listOfFiles = inputDir.listFiles();
            //            for (File file : listOfFiles) {
            //                if (file.isDirectory()) {
            //                    checkAndMakeDirectoryWithChildren(this.getReadDir() + File.separator + file.getName());
            //                }
            //            }
            //        }
            //    }
            //}                
        }

        //if (this.isConvertingFastQ()) {
        //    checkAndMakeDirectory(this.getFastaDir());
        //}
        
        checkAndMakeDirectory(this.getFastqDir() + "_chunks");
        checkAndMakeDirectory(this.getFastaDir() + "_chunks");

        checkAndMakeDirectory(getMARTiDirectory());        
        
        if (this.isBlastingRead()) {
            //checkAndMakeDirectory(this.getReadDir() + "_chunks");
            checkAndMakeDirectory(this.getSampleDirectory() + File.separator + "megan");
        }
        
        if (this.classifyingReads) {
            checkAndMakeDirectory(getLCAParseDirectory());
        }
        
        if (runningCARD) {
            checkAndMakeDirectory(getAMRDirectory());
        }
             
        if (this.isBlastingRead()) {
            for (int i=0; i<blastProcesses.size(); i++) {
                BlastProcess bp = blastProcesses.get(i);                
                checkAndMakeDirectory(getSampleDirectory() + File.separator + bp.getBlastTask() + "_" + bp.getBlastName() + File.separator);
                checkAndMakeDirectory(getLogsDir() + File.separator + bp.getBlastTask() + "_" + bp.getBlastName() + File.separator);
            }
        }        
    }
    
    private void processBarcodeId(String tag, String value) {
        int bc = Integer.parseInt(tag.substring(9));
        
        if (bc > 0) {
            if (barcodeIDs.containsKey(bc)) {
                getLog().printlnLogAndScreen("Warning: already seen ID for barcode "+bc);
            } 
            
            barcodeIDs.put(bc, value);
            getLog().println("Got barcode ID: "+value);
        }
    }
    
    public String getSampleIdByBarcode(int bc) {
        String id = sampleName;
        
        if (bc > 0) {
            if (barcodeIDs.containsKey(bc)) {
                id = barcodeIDs.get(bc);
            } else {
                id = sampleName + "_bc"+bc;
            }
        }
        
        return id;
    }
        
    void readProcessFile() {
        BufferedReader br;
        boolean readNextLine = true;
        
        File f = new File(configFile);
        if (!f.exists()) {
            System.out.println("Error: file "+configFile+" doesn't exist...\n");
            System.exit(1);
        }
                
        System.out.println("\nReading process file "+configFile);
        try {
            br = new BufferedReader(new FileReader(configFile));        
            String line = null;

            do {
                if (readNextLine) {
                    line = br.readLine();
                } 
                
                readNextLine = true;
                
                if (line != null) {
                    if (line.length() > 1) {
                        if (!line.startsWith("#")) {
                            String[] tokens = line.split(":");
                            if (tokens[0].compareToIgnoreCase("Extract") == 0) {
                                extractingReads = true;
                                System.out.println("  Extract "+tokens[1]);
                            } else if (tokens[0].compareToIgnoreCase("Fast5Dir") == 0) {
                                readsDir = tokens[1];
                                System.out.println("  Fast5Dir "+tokens[1]);
                            } else if (((tokens[0].compareToIgnoreCase("SampleName") == 0) ||
                                        (tokens[0].compareToIgnoreCase("RunName")) == 0)) { 
                                sampleName = tokens[1];
                            } else if (tokens[0].compareToIgnoreCase("ConvertFastQ") == 0) {
                                if (tokens.length > 1) {
                                    int value = Integer.parseInt(tokens[1]);
                                    if (value == 1) {
                                        convertingFastQ = true;
                                    }
                                } else {
                                    convertingFastQ = true;
                                }
                            } else if (tokens[0].compareToIgnoreCase("RawDataDir") == 0) {
                                if (tokens[1].startsWith("/")) {
                                    rawDataDir = new RawDataDirectory(tokens[1]);
                                } else {
                                    System.out.println("Error: Raw data directory must be an absolute path");
                                    System.exit(1);
                                }
                            } else if (tokens[0].compareToIgnoreCase("ProcessBarcodes") == 0) {
                                if (tokens.length > 1) {
                                    if (tokens[1].length() > 0) {
                                        barcodesList = new BarcodesList(tokens[1]);
                                        barcodesList.listActiveBarcodes();
                                    }
                                }
                            } else if (tokens[0].compareToIgnoreCase("Scheduler") == 0) {
                                schedulerName = tokens[1];
                            } else if (tokens[0].compareToIgnoreCase("InactivityTimeout") == 0) {
                                fileWatcherTimeout = Integer.parseInt(tokens[1]);
                            } else if (tokens[0].compareToIgnoreCase("LocalSchedulerMaxJobs") == 0) {
                                localSchedulerMaxJobs = Integer.parseInt(tokens[1]);
                            } else if (tokens[0].compareToIgnoreCase("SampleDir") == 0) {
                                sampleDirectory = tokens[1];
                                createSampleDirectory();
                            } else if (tokens[0].compareToIgnoreCase("Analysis") == 0) {
                                parsingReads = true;
                                System.out.println("  Analysis "+tokens[1]);
                            } else if (tokens[0].compareToIgnoreCase("BlastProcess") == 0) {
                                BlastProcess bp = new BlastProcess(this);
                                line = bp.readConfigFile(br);
                                readNextLine = false;
                                blastingReads = true;                        
                                
                                if (bp.getBlastName().equalsIgnoreCase("card")) {
                                    runningCARD = true;
                                }
                                
                                // Force VFDB and CARD to be first run BLAST - ensures results always available when nt finished
                                if ((bp.getBlastName().equalsIgnoreCase("vfdb")) ||
                                    (bp.getBlastName().equalsIgnoreCase("card"))) {
                                    blastProcesses.add(0, bp);
                                    vfdbBlastProcess = bp;
                                } else {
                                    blastProcesses.add(bp);
                                }
                                                                
                                if (bp.useForClassifying()) {
                                    if (classifyingBlastName != null) {
                                        System.out.println("Error: you can't have more than one Blast process with useToClassify set");
                                        System.exit(1);
                                    } else {
                                        classifyingBlastName = bp.getBlastName();
                                        System.out.println("Using " + classifyingBlastName + " for classification");
                                    }
                                }
                            } else if (tokens[0].compareToIgnoreCase("ReadsPerBlast") == 0) {
                                readsPerBlast = Integer.parseInt(tokens[1]);
                            } else if (tokens[0].compareToIgnoreCase("ReadsPerMultiFastQ") == 0) {
                                readsPerMultiFastq = Integer.parseInt(tokens[1]);
                                System.out.println("  ReadsPerMultiFastQ "+readsPerMultiFastq);
                            } else if ((tokens[0].compareToIgnoreCase("Megan5Executable") == 0) ||
                                       (tokens[0].compareToIgnoreCase("MeganExecutable") == 0)) {
                                meganCmdLine = tokens[1];
                            } else if ((tokens[0].compareToIgnoreCase("Megan5License") == 0) ||
                                       (tokens[0].compareToIgnoreCase("MeganLicense") == 0)) {
                                meganLicense = tokens[1];
                            } else if (tokens[0].compareToIgnoreCase("MeganMinSupport") == 0) {
                                doingMeganMinSupport = true;
                                meganMinSupport = Integer.parseInt(tokens[1]);
                            } else if (tokens[0].compareToIgnoreCase("MeganPropertiesFile") == 0) {
                                meganPropertiesFile = tokens[1];
                                System.out.println("Got it "+meganPropertiesFile);
                            } else if (tokens[0].compareToIgnoreCase("MeganMinSupportPercent") == 0) {
                                doingMeganMinSupportPercent = true;
                                meganMinSupportPercent = Double.parseDouble(tokens[1]);
                            } else if (tokens[0].compareToIgnoreCase("UseXvfb")==0) {
                                useXvfb = true;
                            } else if (tokens[0].compareToIgnoreCase("StopProcessingAfter")==0) { 
                                stopProcessingAfter = Integer.parseInt(tokens[1]);
                            } else if (tokens[0].compareToIgnoreCase("TimeLimit")==0) { 
                                timeLimit = Integer.parseInt(tokens[1]);
                            } else if (tokens[0].compareToIgnoreCase("TaxonomyDir")==0) { 
                                taxonomyDir = tokens[1];
                            } else if (tokens[0].compareToIgnoreCase("AccessionMap")==0) { 
                                accessionMapFile = tokens[1];
                            } else if (tokens[0].compareToIgnoreCase("LCAMaxHits") == 0) {
                                lcaMaxHits = Integer.parseInt(tokens[1]);
                            } else if (tokens[0].compareToIgnoreCase("LCAScorePercent") == 0) {
                                lcaScorePercent = Double.parseDouble(tokens[1]);
                            } else if (tokens[0].compareToIgnoreCase("LCAMinIdentity") == 0) {
                                lcaMinIdentity = Integer.parseInt(tokens[1]);
                            } else if (tokens[0].compareToIgnoreCase("LCAMinQueryCoverage") == 0) {
                                lcaMinQueryCoverage = Integer.parseInt(tokens[1]);
                            } else if (tokens[0].compareToIgnoreCase("LCAMinCombinedScore") == 0) {
                                lcaMinCombinedScore = Integer.parseInt(tokens[1]);
                            } else if (tokens[0].compareToIgnoreCase("LCAMinLength") == 0) {
                                lcaMinLength = Integer.parseInt(tokens[1]);
                            } else if (tokens[0].compareToIgnoreCase("ResultsFile") == 0) {
                                resultsFile = tokens[1];
                            } else if (tokens[0].compareToIgnoreCase("InitDir") == 0) {
                                initDir = tokens[1];
                            } else if (tokens[0].compareToIgnoreCase("ReadFilterMinQ") == 0) {
                                readFilterMinQ = Integer.parseInt(tokens[1]);
                            } else if (tokens[0].compareToIgnoreCase("ReadFilterMinLength") == 0) {
                                readFilterMinLength = Integer.parseInt(tokens[1]);
                            } else if (tokens[0].startsWith("BarcodeId")) {
                                processBarcodeId(tokens[0], tokens[1]);
                            } else if (tokens[0].compareToIgnoreCase("AutodeleteBlastResults") == 0) {
                                autodeleteBlastFiles = true;
                            } else if (tokens[0].compareToIgnoreCase("AutodeleteFastaChunks") == 0) {
                                autodeleteFastaChunks = true;
                            } else if (tokens[0].compareToIgnoreCase("AutodeleteFastqChunks") == 0) {
                                autodeleteFastqChunks = true;
                            } else if (tokens[0].compareToIgnoreCase("AutodeleteMetaMapsFiles") == 0) {
                                autodeleteMetaMapsFiles = true;
                            } else if (!tokens[0].startsWith("#")) {                                
                                System.out.println("ERROR: Unknown token "+tokens[0]);
                                System.exit(1);
                            }                                                     
                        }
                    }
                }
            } while (line != null);
        } catch (Exception e) {
            System.out.println("readProcessFile Exception:");
            e.printStackTrace();
            System.exit(1);
        }
                
        if (convertingFastQ) {
            // Work out directory from raw read dir
            fastQConvertDir = rawDataDir.getPathname() + "/fastq_pass";
        }        
                
        checkForClassifyingBlast();

        System.out.println("");
    }
    
    public int getStopProcessingAfter() {
        return stopProcessingAfter;
    }
    
    public boolean useXvfb() {
        return useXvfb;
    }
    
    public String getMeganPropertiesFile() {
        return meganPropertiesFile;
    }
    
    public boolean isDoingMeganMinSupport() {
        return doingMeganMinSupport;
    }

    public boolean isDoingMeganMinSupportPercent() {
        return doingMeganMinSupportPercent;
    }
    
    public int getMeganMinSupport() {
        return meganMinSupport;
    }
    
    public double getMeganMinSupportPercent() {
        return meganMinSupportPercent;
    }
    
    public BlastHandler getBlastHandler() {
        return blastHandler;
    }
    
    public ReadClassifier getReadClassifier() {
        return readClassifier;
    }
    
    public ArrayList<BlastProcess> getBlastProcesses() {
        return blastProcesses;
    }
        
    public boolean mergeFastaFiles() {
        return mergeFastaFiles;
    }
            
    public boolean doForce() {
        return force;
    }
        
    public double getMinQ() {
        return minQForPass;
    }
        
    public boolean debugMode() {
        return false;
    }
    
    public JobScheduler getJobScheduler() {
        return jobScheduler;
    }
                
    public RawDataDirectory getRawDataDir() {
        return rawDataDir;
    }
    
    public BarcodesList getBarcodesList() {
        return barcodesList;
    }
    
    public synchronized void stopProcessing() {        
        stopFlag = true;
    }

    public synchronized void writeStopSequencingFlag() {
        String flagPathname = sampleDirectory + File.separator + "stop_sequencing.flag";
        try {
            logFile.println("Writing "+flagPathname);
            PrintWriter pw = new PrintWriter(new FileWriter(flagPathname));
            pw.close();
        } catch (Exception e) {
            System.out.println("writeStopFlag Exception:");
            e.printStackTrace();
            System.exit(1);
        }
        
        stopFlag = true;
    }

    public synchronized void writeStartedFlag() {
        String flagPathname = sampleDirectory + File.separator + "marti_started.flag";
        try {
            logFile.println("Writing "+flagPathname);
            PrintWriter pw = new PrintWriter(new FileWriter(flagPathname));
            pw.close();
        } catch (Exception e) {
            System.out.println("writeStopFlag Exception:");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public synchronized boolean getStopFlag() {
        return stopFlag;
    }
    
    public boolean runBlastCommand() {
        return runBlastCommand;
    }
    
    public String getTaxonomyDirectory() {
        return taxonomyDir;        
    }
    
    public String getAccessionMap() {
        return accessionMapFile;
    }
    
    public int getLCAMaxHits() {
        return lcaMaxHits;
    }

    public double getLCAScorePercent() {
        return lcaScorePercent;
    }
    
    public String getResultsFile() {
        return resultsFile;
    }
    
    public String getInitDir() {
        return initDir;
    }
    
    public boolean isWriteConfigMode() {
        return writeConfigMode;
    }
    
    public boolean isInitMode() {
        return initMode;
    }
    
    public boolean isClassiftingReads() {
        return classifyingReads;
    }
    
    public int getLCAMinIdentity() {
        return lcaMinIdentity;
    }
    
    public int getLCAMinQueryCoverage() {
        return lcaMinQueryCoverage;
    }
    
    public int getLCAMinCombinedScore() {
        return lcaMinCombinedScore;
    }

    public int getLCAMinLength() {
        return lcaMinLength;
    }

    public int getReadFilterMinQ() {
        return readFilterMinQ;
    }

    public int getReadFilterMinLength() {
        return readFilterMinLength;
    }

    public boolean dontRunNt() {
        return dontRunNt;
    }

    public BlastProcess getVFDBBlastProcess() {
        return vfdbBlastProcess;
    }
            
    public MARTiProgress getProgressReport() {
        return progressReport;
    }
                
    public void setHasReachedReadOrTimeLimit() {
        haveReachedReadOrTimeLimit = true;
    }
    
    public boolean reachedReadOrTimeLimit() {
        return haveReachedReadOrTimeLimit;
    }

    public boolean timeUp() {       
        boolean timeUp = false;
        
        if (timeLimit > 0) {
            long timeSince = System.nanoTime() - startTime;
            long secsSinceLast = timeSince / 1000000000;

            if (secsSinceLast > timeLimit) {
                timeUp = true;
                getLog().println("Time limit (" + timeLimit+") reached.");
            }
        }
                
        return timeUp;
    }
    
    public MARTiResults getResults() {
        return martiResults;
    }
    
    public String getMARTiJSONDirectory(int bc) {
        String dirname = this.getMARTiDirectory() + File.separator + this.getSampleIdByBarcode(bc);
        this.checkAndMakeDirectory(dirname);
        return dirname;
    }
    
    public double getWalkoutMaxE() {
        return walkoutMaxE;
    }
    
    public double getWalkoutMinID() {
        return walkoutMinID;
    }
    
    public int getWalkoutMinLength() {
        return walkoutMinLength;
    } 
    
    public boolean runningCARD() {
        return runningCARD;
    }
    
    public void registerCARDDatabase(String dbPath) {
        if (cardDBPath != null) {
            getLog().println("Warning: already got cardDBPath "+cardDBPath);
        }
        cardDBPath = dbPath;
    }
    
    public String getCARDDatabasePath() {
        return cardDBPath;
    }

    public SampleMetaData getSampleMetaData(int bc) {
        SampleMetaData m = null;
        
        if (metaData.containsKey(bc)) {
            m = metaData.get(bc);
        } else {
            m = new SampleMetaData(this, bc);
            m.writeSampleJSON(false);
            metaData.put(bc, m);
        }
        
        return m;
    }
    
    public boolean fixRandom() {
        return fixRandom;
    }
    
    public long getRandomSeed() {
        return randomSeed;
    }

    public void copyFile(String sourcePathname, String destPathname) {
        try {
            Path source = Paths.get(sourcePathname);
            Path dest = Paths.get(destPathname);
            Files.copy(source, dest, REPLACE_EXISTING);
            getLog().println("Copy "+source);
            getLog().println("  as "+dest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public boolean autodeleteBlastFiles() {
        return autodeleteBlastFiles;
    }
    
    public boolean autodeleteFastaChunks() {
        return autodeleteFastaChunks;
    }
    
    public boolean autodeleteFastqChunks() {
        return autodeleteFastqChunks;
    }
    
    public boolean autodeleteMetaMapsFiles() {
        return autodeleteMetaMapsFiles;
    }
    
    public int getBarcodeFromPath(String pathname) {
       int barcode = 0;
       File f = new File(pathname);
       String leafname = f.getName();

       if (isBarcoded()) {
           if (leafname.contains("barcode")) {
               String bcString = leafname.substring(leafname.indexOf("barcode")+7, leafname.indexOf("barcode")+9);
               barcode = Integer.parseInt(bcString);
           } else if (pathname.contains("barcode")) {
               String bcString = pathname.substring(pathname.indexOf("barcode")+7, pathname.indexOf("barcode")+9);
               barcode = Integer.parseInt(bcString);
           } else {
               getLog().printlnLogAndScreen("ERROR: Can't get barcode from pathname "+pathname);
           }
       }

       return barcode;
   }     
    
    public int getMaxJobs() {
        return localSchedulerMaxJobs;
    }    
    
    public String getBlastProcessNames() {
        return blastProcessNames;
    }
    
    public MARTiEngineOptionsFile getOptionsFile() {
        return engineOptionsFile;
    }
    
    public void setTaxonomyDir(String dir) {
        taxonomyDir = dir;
    }
    
    public void checkForClassifyingBlast() {
        if (classifyingBlastName == null) {
            // Check for nt
            for (int i=0; i<blastProcesses.size(); i++) {
                BlastProcess bp = blastProcesses.get(i);
                if (bp.getBlastName().equals("nt")) {
                    System.out.println("No Blast classification process found - using nt");
                    bp.setClassifyThis();
                    classifyingBlastName = "nt";
                }
            }
        }
        
        if (classifyingBlastName == null) {
            System.out.println("Error: couldn't find a BLAST process to classify with!");
            System.exit(1);
        }
    }
    
    public void writeAllSampleJSON(boolean martiComplete) {
        Set<Integer> keys = metaData.keySet();
        
        for (int bc : keys) {        
            SampleMetaData md = metaData.get(bc);
            md.writeSampleJSON(martiComplete);
        }
    }
    
    public String getClassifyingBlastName() {
        return classifyingBlastName;
    }
    
    public boolean inTestMode() {
        return testMode;
    }
}
