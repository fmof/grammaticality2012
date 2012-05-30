args <- commandArgs(trailingOnly = FALSE)
print(args)
data <- read.csv(file=args[6], head=FALSE)
pdf(args[7])
Graph <- barplot(t(data)[-1,][-1,][-2,],beside=T,col=rainbow(2), main="Occurrence (%) w/ r rule expansions, r=31, k=50000", ylab="Occurrence (percentage)", xlab="Rules expanded in fragment", names.arg=seq(from=1,to=length(data),by=1),xpd=F,ylim=c(0,max(data$V3, data$V5)))
#secondyaxis = pmax(data$V2,data$V4)
n=1
everynxvals = Graph[seq(1, length(Graph), n)] + .4
everynypos = (t(data)[-1,][-1,][-2,])[seq(1,length(Graph),n)] 
everynyvals = (t(data)[-1,][-2,][-3,])[seq(1,length(Graph),n)]
text(x=everynxvals, y=everynypos , everynyvals,cex=0.4,pos=3, srt=90)
#text(x=Graph, , cex=0.5, pos=3)
legend("topright", c("CFG","TSG"), fill=rainbow(2))
dev.off()
q()
