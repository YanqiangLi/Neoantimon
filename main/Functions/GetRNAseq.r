GetRNAseq<-function(output_peptide_txt_file, RNAseq_file = NA, output_file_rna_vcf = NA){
  data<-t(sapply(scan(output_peptide_txt_file, "character", sep="\n"),
          function(x) strsplit(x, "\t")[[1]]))
  if(is.na(RNAseq_file) | !file.exists(RNAseq_file)){
     ratio_matrix<-matrix(nrow=nrow(data), ncol=3, NA)
     write.table(cbind(data, ratio_matrix), commandArgs(TRUE)[1],
        row.names=FALSE, col.names=FALSE, quote=FALSE, sep="\t")
     q("no")
  }
  
  ##Get RNA Data
  temp<-scan(RNAseq_file, "character", sep="\n")
  if(length(temp) < 2){
      ratio_matrix<-matrix(nrow=nrow(data), ncol=3, NA)
      write.table(cbind(data, ratio_matrix), commandArgs(TRUE)[1],
        row.names=FALSE, col.names=FALSE, quote=FALSE, sep="\t")
      q("no")
  }
  rna<-t(sapply(temp, function(x) strsplit(x, "\t")[[1]]))
  rna<-rna[-1,]
  rna_pos<-t(sapply(rna[,2], function(x) strsplit(x, ":|-")[[1]]))
  hit<-match(sapply(data[,2], function(x) strsplit(x,"_")[[1]][2]), rna[,1])
  
  #data[,chr], data[,m_position]
  for(x in which(is.na(hit))){
     hit_pos<-which(data[x,3]==rna_pos[,1])
     abs_hit_pos<-hit[which(as.numeric(data[x,12]) > as.numeric(rna_pos[hit,2]) & as.numeric(data[x,12]) < as.numeric(rna_pos[hit,3]))]
     if(length(abs_hit_pos)!=0){
        print(abs_hit_pos[1])
        hit[x]<-abs_hit_pos[1]
     }
  }
  
  data<-cbind(data, rna[hit,3])
  colnames(data)<-NULL
  rownames(data)<-NULL
  tail_col<-ncol(data)
  
  if(is.na(output_file_rna_vcf) | !file.exists(output_file_rna_vcf)){
    ratio<-t(sapply(scan(output_file_rna_vcf, "character", sep="\n"),
                    function(x) strsplit(x, "\t")[[1]]))
    ratio<-t(sapply(scan(output_file_rna_vcf, "character", sep="\n", skip=which(lapply(ratio, length)>2)[1]), 
                    function(x) strsplit(x, "\t")[[1]]))
    if(ncol(ratio)==0){
      ratio_matrix<-matrix(nrow=nrow(data), ncol=2, NA)
    }else{
      ratio_matrix<-NULL
      for(i in 1:nrow(data)){
        hit<-which(data[i, 3]==ratio[,1] & data[i, 12]==ratio[,2])
        if(length(hit)==1 & !is.na(data[i,13])){
          l<-strsplit(ratio[hit,8], "=|,|;")[[1]]
          hit2<-grep("DP4",l)
          if(length(hit2)==1) count<-as.numeric(l[(hit2+1):(hit2+4)])
          else count<-c(.1, 0, 0, 0)
        } else {
          count<-c(.1, 0, 0, 0)
        }
        ratio_matrix<-rbind(ratio_matrix, 
                            c(paste(c(sum(count[c(3,4)]),sum(count)), collapse="/"), 
                              ifelse(is.na(data[i,tail_col]), 0, as.numeric(data[i, tail_col])) * sum(count[c(3,4)])/sum(count)))
      }
    }
  } else {
    ratio_matrix<-matrix(nrow=nrow(data), ncol=2, NA)
  }
  write.table(cbind(data, ratio_matrix), output_peptide_txt_file, 
   row.names=FALSE, col.names=FALSE, quote=FALSE, sep="\t")
}