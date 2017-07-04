##
#lib_sample includes all sample files.
##
MainSNVClass1(hmdir = getwd(),
              input_file = "data.txt/sample.txt",
              job_ID = "NO_JOB_ID",
              Chr_Column = 1,
              Mutation_Start_Column = 2,
              Mutation_End_Column = 3,
              Mutation_Ref_Column = 4,
              Mutation_Alt_Column = 5,
              NM_ID_Column = 10,
              Depth_Tumor_Column = 12,
              Depth_Normal_Column = 14,
              file_name_in_HLA_table = "sample",
              HLA_file = "data.txt/hla_table.txt",
              RNAseq_file = "data.txt/RNAseq.txt",
              CNV="data.txt/Copy.txt",
              Purity = 0.8,
              ccfp_dir = "lib/ccfp.jar",
              netMHCpan_dir = "lib/netMHCpan-3.0/netMHCpan",
              refDNA = "lib/GRCh37.fa")

MainMergeClass1(hmdir = getwd(),
                input_dir = "data.txt",
                input_file_prefix = "sample",
                Tumor_RNA_BASED_ON_DNA = TRUE)

MainSNVClass2(hmdir = getwd(),
              input_file = "data.txt/sample.txt",
              job_ID = "NO_JOB_ID",
              Chr_Column = 1,
              Mutation_Start_Column = 2,
              Mutation_End_Column = 3,
              Mutation_Ref_Column = 4,
              Mutation_Alt_Column = 5,
              NM_ID_Column = 10,
              Depth_Tumor_Column = 12,
              Depth_Normal_Column = 14,
              file_name_in_HLA_table = "sample",
              HLA_file = "data.txt/hla_table2.txt",
              RNAseq_file = "data.txt/RNAseq.txt",
              CNV="data.txt/Copy.txt",
              Purity = 0.8,
              ccfp_dir = "lib/ccfp.jar",
              netMHCpan_dir = "lib/netMHCIIpan-3.1/netMHCIIpan",
              refDNA = "lib/GRCh37.fa")

MainMergeClass2(hmdir = getwd(),
                input_dir = "data.txt",
                input_file_prefix = "sample_genomon",
                Tumor_RNA_BASED_ON_DNA = TRUE)

MainINDELClass1(hmdir = getwd(),
                input_file = "data.txt/sample.txt",
                job_ID = "NO_JOB_ID",
                Chr_Column = 1,
                Mutation_Start_Column = 2,
                Mutation_End_Column = 3,
                Mutation_Ref_Column = 4,
                Mutation_Alt_Column = 5,
                NM_ID_Column = 10,
                Depth_Tumor_Column = 12,
                Depth_Normal_Column = 14,
                file_name_in_HLA_table = "sample",
                HLA_file = "data.txt/hla_table.txt",
                RNAseq_file = "data.txt/RNAseq.txt",
                CNV="data.txt/Copy.txt",
                Purity = 0.8,
                ccfp_dir = "lib/ccfp.jar",
                netMHCpan_dir = "lib/netMHCpan-3.0/netMHCpan",
                refDNA = "lib/GRCh37.fa")

MainINDELClass2(hmdir = getwd(),
                input_file = "data.txt/sample.txt",
                job_ID = "NO_JOB_ID",
                Chr_Column = 1,
                Mutation_Start_Column = 2,
                Mutation_End_Column = 3,
                Mutation_Ref_Column = 4,
                Mutation_Alt_Column = 5,
                NM_ID_Column = 10,
                Depth_Tumor_Column = 12,
                Depth_Normal_Column = 14,
                file_name_in_HLA_table = "sample",
                HLA_file = "data.txt/hla_table2.txt",
                RNAseq_file = "data.txt/RNAseq.txt",
                CNV="data.txt/Copy.txt",
                Purity = 0.8,
                ccfp_dir = "lib/ccfp.jar",
                netMHCpan_dir = "lib/netMHCIIpan-3.1/netMHCIIpan",
                refDNA = "lib/GRCh37.fa")
