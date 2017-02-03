data<-t(sapply(scan(commandArgs(TRUE)[1], "character", sep="\n"),
        function(x) strsplit(x, "\t")[[1]]))
if(is.na(commandArgs(TRUE)[2])){
   ratio_matrix<-matrix(nrow=nrow(data), ncol=3, NA)
   write.table(cbind(data, ratio_matrix), commandArgs(TRUE)[1],
      row.names=FALSE, col.names=FALSE, quote=FALSE, sep="\t")
   q("no")
} else {
   sep<-strsplit(commandArgs(TRUE)[2],"/")[[1]]
   if(length(grep(sep[length(sep)], list.files(paste(sep[-length(sep)],collapse="/"))))==0){
    ratio_matrix<-matrix(nrow=nrow(data), ncol=3, NA)
    write.table(cbind(data, ratio_matrix), commandArgs(TRUE)[1],
      row.names=FALSE, col.names=FALSE, quote=FALSE, sep="\t")
    q("no")
   }   
}

##Get RNA Data
temp<-scan(commandArgs(TRUE)[2], "character", sep="\n")
if(length(temp)<5){
    ratio_matrix<-matrix(nrow=nrow(data), ncol=3, NA)
    write.table(cbind(data, ratio_matrix), commandArgs(TRUE)[1],
      row.names=FALSE, col.names=FALSE, quote=FALSE, sep="\t")
    q("no")
}
rna<-t(sapply(temp,function(x) strsplit(x, "\t")[[1]]))
rna<-rna[-1,]
rna_pos<-t(sapply(rna[,2], function(x) strsplit(x, ":|-")[[1]]))

hit<-match(sapply(data[,2], function(x) strsplit(x,"_")[[1]][2]), rna[,1])

#data[,chr], data[,m_position]
for(x in which(is.na(hit))){
   hit_pos<-which(data[x,3]==rna_pos[,1])
   abs_hit_pos<-hit[which((as.numeric(data[x,12]) >
      as.numeric(rna_pos[hit,2]) & as.numeric(data[x,12]) < as.numeric(rna_pos[hit,3])))]
   if(length(abs_hit_pos)!=0){
      print(abs_hit_pos[1])
      hit[x]<-abs_hit_pos[1]
   }
}

data<-cbind(data, rna[hit,3])
colnames(data)<-NULL
rownames(data)<-NULL
tail_col<-ncol(data)

##Get Ratio
ratio<-scan(commandArgs(TRUE)[3], "character", sep="\n", nlines=100)
h<-grep(":",ratio)
ratio<-t(sapply(scan(commandArgs(TRUE)[3], "character", sep="\n", skip=length(h)+2, nlines=10000), 
   function(x) strsplit(x, "\t")[[1]]))
if(ncol(ratio)==0){
   ratio_matrix<-matrix(nrow=nrow(data), ncol=3, NA)
   write.table(cbind(data, ratio_matrix), commandArgs(TRUE)[1],
      row.names=FALSE, col.names=FALSE, quote=FALSE, sep="\t")
   q("no")
}

#data[,total depth]
size<-sum(as.numeric(sapply(strsplit(ratio[1,3],"M|D|I")[[1]], function(x) rev(strsplit(x,"N|S|H|P")[[1]])[1])))
ratio_matrix<-NULL
for(i in 1:nrow(data)){
   hit<-which(!is.na(match(ratio[,1], data[i,3])))
   hit<-hit[which(as.numeric(ratio[hit,2]) + size > data[i,12]
      & as.numeric(ratio[hit,2]) -1 < data[i,12])]
   #remove<-grep("N|S|H|P", ratio[hit,3])
   #if(length(remove)>0) hit<-hit[-grep("N|S|H|P", ratio[hit,3])]
   if(length(hit)==0){
      ratio_matrix<-rbind(ratio_matrix, c("0/0.1","0"))
      next
   }
   total<-length(hit) + length(grep("I|D", ratio[hit,3]))
   mut<-length(grep("I|D", ratio[hit,3]))
   r<-paste(mut, total, sep="/")
   ratio_matrix<-rbind(ratio_matrix, c(r, mut/total * as.numeric(data[i,19])))
}

write.table(cbind(data, ratio_matrix), commandArgs(TRUE)[1], 
   row.names=FALSE, col.names=FALSE, quote=FALSE, sep="\t")
