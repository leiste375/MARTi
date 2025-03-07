.. _configfiles:

Config file format
==================

Each time you run a MARTi analysis on a sequencing run, you need to specify a config file which provides the details of the analysis to be performed.

This config file is generated by the MARTi launcher front-end (Desktop) or GUI (cluster/HPC).

The following table specifies the meaning of the parameters in the file. Keywords in **bold** are mandatory, others are optional.

Sample and global settings
--------------------------

.. csv-table::
   :header: "Keyword", "Example", "Meaning"
   :file: table1.csv
   :delim: tab

Pre-filtering settings
----------------------

.. csv-table::
   :header: "Keyword", "Example", "Meaning"
   :file: table2.csv
   :delim: tab

LCA classification settings
---------------------------

These Lowest Common Ancestor settings apply to BLAST results (see below).

.. csv-table::
   :header: "Keyword", "Example", "Meaning"
   :file: table3.csv
   :delim: tab

BLAST processes
---------------

You can run multiple BLAST processes. Each begins with the Keyword BlastProcess.

.. csv-table::
   :header: "Keyword", "Example", "Meaning"
   :file: table4.csv
   :delim: tab


Centrifuge processes
--------------------

You can run multiple Centrifuge processes. Each begins with the keyword CentrifugeProcess.

.. csv-table::
   :header: "Keyword", "Example", "Meaning"
   :file: table_centrifuge.csv
   :delim: tab

Kraken2 processes
-----------------

You can run multiple Kraken2 processes. Each begins with the keywork Kraken2Process.

.. csv-table::
   :header: "Keyword", "Example", "Meaning"
   :file: table_kraken2.csv
   :delim: tab

Metadata
---------------

Metadata blocks are optional blocks that contain data describing the collection of samples. A metadata block could describe the whole run or a subset of barcodes.

.. csv-table::
   :header: "Keyword", "Example", "Meaning"
   :file: table5.csv
   :delim: tab

Example
-------

Example file::

 SampleName:BAMBI_1D_19092017_MARTi
 RawDataDir:/Users/leggettr/Documents/Datasets/BAMBI_1D_19092017_MARTi
 SampleDir:/Users/leggettr/Documents/Projects/MARTiTest/BAMBI_1D_19092017_MARTi
 ProcessBarcodes:
 BarcodeId1:SampleNameHere
 
 Scheduler:local
 LocalSchedulerMaxJobs:4
 
 InactivityTimeout:10
 StopProcessingAfter:50000000
 
 TaxonomyDir:/Users/leggettr/Documents/Databases/taxonomy_6Jul20
 LCAMaxHits:20
 LCAScorePercent:90
 LCAMinIdentity:60
 LCAMinQueryCoverage:0
 LCAMinCombinedScore:0
 LCAMinLength:50
 
 ConvertFastQ 

 ReadsPerBlast:8000
 
 ReadFilterMinQ:9
 ReadFilterMinLength:500
 
 BlastProcess
     Name:nt
     Program:megablast
     Database:/Users/leggettr/Documents/Databases/nt_30Jan2020_v5/nt
     TaxaFilter:/Users/leggettr/Documents/Datasets/bacteria_viruses.txt
     MaxE:0.001
     MaxTargetSeqs:25
     BlastThreads:4
     UseToClassify
 
 BlastProcess
     Name:card
     Program:blastn
     Database:/Users/leggettr/Documents/Databases/card/nucleotide_fasta_protein_homolog_model.fasta
     MaxE:0.001
     MaxTargetSeqs:100
     BlastThreads:1
 
 Metadata
     Location:52.62170,1.21900
     Date:31/10/23
     Time: 11:41
     Temperature:21.7C
     Humidity:49%
     Keywords:bambi


Different classification processes can be performed in the same MARTi process (but only one classification process can have the "UseToClassify" field). The example below shows a config file that classifies reads using Kraken2, and searches for AMR hits using BLAST and the CARD database. Note that if a BLAST/CARD process is used, a walkout analysis giving the putative host taxa for AMR genes is only performed if a BLAST process is used to classify the reads. ::

 SampleName:BAMBI_1D_19092017_MARTi
 RawDataDir:/Users/leggettr/Documents/Datasets/BAMBI_1D_19092017_MARTi
 SampleDir:/Users/leggettr/Documents/Projects/MARTiTest/BAMBI_1D_19092017_MARTi
 ProcessBarcodes:
 BarcodeId1:SampleNameHere
 
 Scheduler:local
 LocalSchedulerMaxJobs:4
 
 InactivityTimeout:10
 StopProcessingAfter:50000000
 
 TaxonomyDir:/Users/leggettr/Documents/Databases/taxonomy_6Jul20
 LCAMaxHits:20
 LCAScorePercent:90
 LCAMinIdentity:60
 LCAMinQueryCoverage:0
 LCAMinCombinedScore:0
 LCAMinLength:50
 
 ConvertFastQ 

 ReadsPerBlast:8000
 
 ReadFilterMinQ:9
 ReadFilterMinLength:500
 
 Kraken2Process
     Name:refseq_16
     Database:/Users/leggettr/Documents/Databases/kraken2/k2_standard_16gb_20231009/
     Kraken2Threads:4
     UseToClassify
 
 BlastProcess
     Name:card
     Program:blastn
     Database:/Users/leggettr/Documents/Databases/card/nucleotide_fasta_protein_homolog_model.fasta
     MaxE:0.001
     MaxTargetSeqs:100
     BlastThreads:1
 

