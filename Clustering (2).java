
import java.util.*;

/**
 * ISTE-612 Lab 5
 * Document clustering
 * Nilesh Jakamputi
 */
public class Clustering {
	
	int numberOfDoc;
	int numClusters;
	int vSize;
	Doc[] docList;
	HashMap<String, Integer> termIdMap;
	HashMap<Integer,double[]>[] clusterDocs;
	Doc[] centroids;
	String[] myDocs;

	ArrayList<Doc>[] clusters;
	public Clustering(int numC)
	{
		numClusters = numC;
		clusters = new ArrayList[numClusters];
		centroids = new Doc[numClusters];
		termIdMap = new HashMap<>();
	}
	
	/**
	 * Load the documents to build the vector representations
	 * @param docs
	 */
	public void preprocess(String[] docs){
         //TO BE COMPLETED
		myDocs = docs;
		numberOfDoc = docs.length;
		docList = new Doc[numberOfDoc];
		int termId = 0;
		
		int docId = 0;
		for(String doc:docs){
			String[] tokens = doc.split(" ");
			Doc docObj = new Doc(docId);
			for(String token: tokens){
				if(!termIdMap.containsKey(token)){
					termIdMap.put(token, termId);
					docObj.tId.add(termId);
					docObj.wtOfterm.add(1.0);
					termId++;
				}
				else{
					Integer tid = termIdMap.get(token);
					int index = docObj.tId.indexOf(tid);
					if (index >0){
						double tw = docObj.wtOfterm.get(index);
						docObj.wtOfterm.add(index, tw+1);
					}
					else{
						docObj.tId.add(termIdMap.get(token));
						docObj.wtOfterm.add(1.0);
					}
				}
			}
			docList[docId] = docObj;
			docId++;
		}
		vSize = termId;
		System.out.println("vSize: " + vSize);
		
		for(Doc doc: docList){
			double docLength = 0;
			double[] vecOfTerm = new double[vSize];
			for(int i=0;i<doc.tId.size();i++){
				double tfidf = (1+Math.log(doc.wtOfterm.get(i)));
				doc.wtOfterm.set(i, tfidf);
				docLength += Math.pow(tfidf, 2);
			}
			docLength = Math.sqrt(docLength);
			
			for(int i=0;i<doc.tId.size();i++){
				double tw = doc.wtOfterm.get(i);
				doc.wtOfterm.set(i, tw/docLength);
				
				vecOfTerm[doc.tId.get(i)] = tw/docLength;

			}
			doc.vecOfTerm = vecOfTerm;
		
		}
				
	}
	
	/**
	 * Cluster the documents
	 * For kmeans clustering, use the first and the seventh documents as the initial centroids
	 */
	public void cluster(){
		//TO BE COMPLETED
		int stop = 0;
		ArrayList<Double> first = new ArrayList<>();
		for(Double item: docList[0].vecOfTerm){
			first.add(item);
		}
		ArrayList<Double> second = new ArrayList<>();
		for(Double item: docList[6].vecOfTerm){
			second.add(item);
		}
		System.out.println(first);
		System.out.println(second);
		int cnter = 1;

		while(stop != 1){
			System.out.println(cnter++);
			ArrayList<Double> fDistance = new ArrayList<Double>();
			ArrayList<Double> sDistance = new ArrayList<Double>();
			double v1 = 0.0;
			double v2 = 0.0;
			for(Doc doc : docList){
				v1 = 0.0;
				v2 = 0.0;
				for (int index = 0; index<first.size(); index++){
					v1+=first.get(index)*doc.vecOfTerm[index];
					v2+=second.get(index)*doc.vecOfTerm[index];
				}
				fDistance.add(v1);
				sDistance.add(v2);
			}

			ArrayList<Integer> c1 = new ArrayList<>();
			ArrayList<Integer> c2 = new ArrayList<>();

			for(int index = 0; index<fDistance.size(); index++){
				if(fDistance.get(index)>=sDistance.get(index)){
					c1.add(index);
				}else{
					c2.add(index);
				}
			}
			ArrayList<Double> firstn = new ArrayList<>();
			ArrayList<Double> secndnew = new ArrayList<>();
			
			int cnt = 1;
			for(int item : c1){
				ArrayList<Double> temp = new ArrayList<>();
				for(Double i: docList[item].vecOfTerm){
					temp.add(i);
				}
				if(cnt==1){
					firstn.addAll(temp);
				}
				for(int index=0; index<temp.size(); index++){
					if(cnt == 1){
						continue;
					}
					firstn.set(index, (firstn.get(index)+temp.get(index)));
				}
				++cnt;
			}

			int fStop = 0;
			for(int index=0; index<firstn.size(); index++){
				firstn.set(index, firstn.get(index)/c1.size());
				if(Double.compare(first.get(index),firstn.get(index)) == 0){
					fStop += 1;
				}
			}

			cnt = 1;
			for(int item : c2){
				ArrayList<Double> temp = new ArrayList<Double>();
				for(Double i: docList[item].vecOfTerm){
					temp.add(i);
				}
				if(cnt==1){
					secndnew.addAll(temp);
				}
				for(int index=0; index<temp.size(); index++){
					if(cnt == 1){
						continue;
					}
					secndnew.set(index, secndnew.get(index)+temp.get(index));
				}
				++cnt;
			}
			int sStop = 0;
			for(int index=0; index<secndnew.size(); index++){
				secndnew.set(index, secndnew.get(index)/c2.size());
				if(Double.compare(second.get(index), secndnew.get(index)) == 0){
					sStop += 1;
				}
			}

			System.out.println("Cluster 1");
			System.out.println(c1);
			System.out.println("Centroid 1");
			System.out.println(firstn);
			System.out.println("Cluster 2");
			System.out.println(c2);
			System.out.println("Centroid 2");
			System.out.println(secndnew);

			if((sStop == secndnew.size()) && (fStop == firstn.size())){
				stop = 1;
			}
			else{
				first = firstn;
				second = secndnew;
			}
		}
	}

	

	
	public static void main(String[] args){
		String[] docs = {"hot chocolate cocoa beans",
				 "cocoa ghana africa",
				 "beans harvest ghana",
				 "cocoa butter",
				 "butter truffles",
				 "sweet chocolates can",
				 "brazil sweet sugar can",
				 "suger can brazil",
				 "sweet cake icing",
				 "cake black forest"
				};
		Clustering c = new Clustering(2);
		c.preprocess(docs);
		System.out.println("Vector space representation:");
		for(int i=0;i<c.docList.length;i++){
			System.out.println(c.docList[i]);
		}
		
		c.cluster();
	}
}

/**
 * 
 * Document class for the vector representation of a document
 */
class Doc{
	int docId;
	ArrayList<Integer> tId;
	ArrayList<Double> wtOfterm;
	double[] vecOfTerm;

	public Doc(){

	}
	public Doc(int id){
		docId = id;
		tId = new ArrayList<Integer>();
		wtOfterm = new ArrayList<Double>();
	}
	public void setvecOfTerm(double[] vec){
		vecOfTerm = vec;
	}

	public String toString()
	{
		StringBuilder docString = new StringBuilder("[");
		for (double v : vecOfTerm) {
			docString.append(v).append(",");
		}
		return docString+"]";
	}
	
}