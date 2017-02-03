#!/bin/bash
#$ -S /bin/bash
#$ -cwd

#Download Ref Data
mkdir ./../lib_int
cd ./../lib_int
wget http://hgdownload.soe.ucsc.edu/goldenPath/hg38/bigZips/hg38.fa.gz
wget ftp://ftp.ensembl.org/pub/release-87/fasta/homo_sapiens/dna/Homo_sapiens.GRCh38.dna.toplevel.fa.gz
mv Homo_sapiens.GRCh38.dna.toplevel.fa GRCh38.fa

#Unpack
gunzip hg38.fa.gz
gunzip Homo_sapiens.GRCh38.dna.toplevel.fa.gz

#Install samtools
wget http://sourceforge.net/projects/samtools/files/samtools/1.3/samtools-1.3.tar.bz2
tar jxf samtools-1.3.tar.bz2
cd samtools-1.3
./configure
make
make install

#Install bcftools
https://github.com/samtools/bcftools/releases/download/1.3.1/bcftools-1.3.1.tar.bz2
tar jxf bcftools-1.3.1.tar.bz2
cd bcftools-1.3.1
make
sudo make install

#Make Index
samtools faidx hg38.fa
samtools Homo_sapiens.GRCh38.dna.toplevel.fa

#Install netMHCpan3.0
mv ./../main/netMHCpan-3.0a.Linux.tar ./
mv ./../main/netMHCpan-3.0a.Darwin.tar.gz ./
if [ -e netMHCpan-3.0a.Linux.tar ]; then
 tar zxvf netMHCpan-3.0a.Linux.tar
elif [ -e netMHCpan-3.0a.Darwin.tar.gz ]
 tar zxvf nnetMHCpan-3.0a.Darwin.tar.gz
fi
cd netMHCpan-3.0
mkdir tmp
sed -i -e "s/\/usr\/cbs\/packages\/netMHCpan\/3.0\/netMHCpan-3.0/${0}\/netMHCpan-3.0/g" netMHCpan
sed -i -e "s/#setenv/setenv/g" netMHCpan
wget http://www.cbs.dtu.dk/services/NetMHCpan-3.0/data.tar.gz
tar -xvf data.tar.gz
rm data.tar.gz
cd ..

#Install netMHCIIpan3.1
mv ./../main/netMHCIIpan-3.1a.Linux.tar ./
mv ./../main/netMHCIIpan-3.1a.Darwin.tar.gz ./
if [ -e netMHCIIpan-3.1a.Linux.tar ]; then
 tar zxvf netMHCIIpan-3.1a.Linux.tar
elif [ -e netMHCIIpan-3.1a.Darwin.tar.gz ]
 tar zxvf netMHCIIpan-3.1a.Darwin.tar.gz
fi
cd netMHCIIpan-3.1
mkdir tmp
sed -i -e "s/\/usr\/cbs\/packages\/netMHCIIpan\/3.1\/netMHCIIpan-3.1/${0}\/netMHCIIpan-3.1/g" netMHCIIpan
sed -e "15i setenv  TMPDIR  \$\{NMHOME\}/tmp" netMHCIIpan
wget http://www.cbs.dtu.dk/services/NetMHCIIpan-3.1/data.tar.gz
gunzip -c data.tar.gz | tar xvf -
rm data.tar.gz
cd ..

#Make refMrna Files
wget http://hgdownload.soe.ucsc.edu/goldenPath/hg38/bigZips/refMrna.fa.gz
gunzip refMrna.fa.gz
grep ">" refMrna.fa | sed -e "s/>//g" | cut -d' ' -f1 > refMrna.merge.cut1.fa
grep ">" refMrna.fa | sed -e "s/>//g" | cut -d' ' -f2 > refMrna.merge.cut2.fa
grep -v ">" refMrna.fa | sed -e "s/>//g" | cut -d' ' -f2 > refMrna.merge.cut3.fa
paste refMrna.merge.cut1.fa refMrna.merge.cut2.fa refMrna.merge.cut3.fa > refMrna.merge.fa

#Make refFlat Files
wget http://hgdownload.soe.ucsc.edu/goldenPath/hg38/database/refFlat.txt.gz
gunzip refFlat.txt.gz
cut -f2 refFlat.txt > refFlat.cut.txt

#ReWrite CCFP
parent_dir=$(cd $(dirname $0)/..;pwd)
grep -i -e "s/IndicateDirectory/${parent_dir}\/lib_MUT/g" ./../lib_MUT/perl/MUT.pm

cd ./../main
