package qcri.dafna.explaination;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import au.com.bytecode.opencsv.CSVWriter;
import qcri.dafna.dataModel.data.ConfValueLabel;
import qcri.dafna.dataModel.data.DataSet;
import qcri.dafna.dataModel.data.Globals;
import qcri.dafna.dataModel.data.Source;
import qcri.dafna.dataModel.data.SourceClaim;
import qcri.dafna.dataModel.data.ValueBucket;
import qcri.dafna.dataModel.dataSet.CSVDatasetReader.ConfidenceReader;
import qcri.dafna.dataModel.dataSet.CSVDatasetReader.TrustWorthinessReader;
import qcri.dafna.voter.Voter;
import qcri.dafna.voter.VoterParameters;

public class MetricsGeneratorOld extends Voter  {
	
	private String claimID;
	private String confidenceFilePath;
	private String trustWorthinessFilePath;
	private int nbSS;
	private int nbC;
	private int nbValDI;
	private int nbVal;
	private String outputPath;
	
	public MetricsGeneratorOld(DataSet dataSet, VoterParameters params, String claimID, String confidenceFilePath, String trustWorthinessFilePath, String outputPath)
	{
		super(dataSet, params);
		this.claimID = claimID; // Claim which is to be falsified.
		this.confidenceFilePath = confidenceFilePath; // Results as shown to the user
		this.trustWorthinessFilePath = trustWorthinessFilePath; // Results as shown to the user
		this.nbSS = 0;
		this.nbC = 0;
		this.nbValDI = 0;
		this.nbVal = 0;
		this.outputPath = outputPath;
	}
	
	protected void initParameters() {
		singlePropertyValue = false;
		onlyMaxValueIsTrue = false;
	}
	
	protected int runVoter(boolean convergence100) {
		HashMap<Integer, ConfValueLabel> conf;
		HashMap<String, Double> trust;
		
		ConfidenceReader confidenceReader = new ConfidenceReader();
		TrustWorthinessReader trustWorthinessReader = new TrustWorthinessReader();
		
		conf = confidenceReader.readConfidenceFile(confidenceFilePath, ',');
		trust = trustWorthinessReader.readTrustFile(trustWorthinessFilePath, ',');
		
		
		// Normalization
		
		HashMap<Integer, Double> confValues = new HashMap<Integer, Double>();
		HashMap<String, Double> trustValues =  new HashMap<String, Double>();
		
		for(int claimIDForConf : conf.keySet()){
			confValues.put(claimIDForConf,conf.get(claimIDForConf).getConfidenceValue());
		}
		
		for(String sourceIDForTrust : trust.keySet()){
			trustValues.put(sourceIDForTrust,trust.get(sourceIDForTrust));
		}
				
		double maxConfValue = Collections.max(confValues.values());
		double minConfValue = Collections.min(confValues.values());
		double maxTrustValue = Collections.max(trustValues.values());
		double minTrustValue = Collections.min(trustValues.values());
		
		for(int claimIDForConf : confValues.keySet()){
			double normalizedValue = (confValues.get(claimIDForConf)-minConfValue)/(maxConfValue - minConfValue);
			confValues.put(claimIDForConf, normalizedValue);
		}
		
		for(String sourceIDForTrust : trustValues.keySet()){
			double normalizedValue = (trustValues.get(sourceIDForTrust)-minTrustValue)/(maxTrustValue - minTrustValue);
			trustValues.put(sourceIDForTrust, normalizedValue);
		}
		
		for(int claimIDForConf: conf.keySet()){
			ConfValueLabel newValue = new ConfValueLabel();
			newValue.setLabel(conf.get(claimIDForConf).getLabel());
			newValue.setBucketId(conf.get(claimIDForConf).getBucketId());
			newValue.setConfidenceValue(confValues.get(claimIDForConf));
			conf.put(claimIDForConf, newValue);
		}
		
		for(String sourceIDForTrust: trust.keySet()){
			trust.put(sourceIDForTrust, trustValues.get(sourceIDForTrust));
		}
		
		//Normalization end
		
		intializeWithResults(conf, trust);
		String targetObjectId = "";
		String targetPropertyName = "";
		String targetDataItemKey = "";
		String targetSource = "";
		ValueBucket targetBucket = null;
		
		double maxTrustWorthySourceDI = -1*Double.MAX_VALUE;
		double minTrustWorthySourceDI = Double.MAX_VALUE;
		
		String originalTrust = "";
		String originalConfidence = "";
		double globalTrustWorthinessComparison = 0.0;
		double localTrustWorthinessComparison = 0.0;
		int	totalNumberOfClaims = 0;
		double globalConfidenceComparison = 0.0;
		double localConfidenceComparison = 0.0;
		
		for (String DIKey : dataSet.getDataItemsBuckets().keySet()){
			for (ValueBucket b : dataSet.getDataItemsBuckets().get(DIKey)) {
					for (SourceClaim claim : b.getClaims()) {
						if(Integer.valueOf(claimID) == claim.getId()){
							targetObjectId = claim.getObjectIdentifier();
							targetPropertyName = claim.getPropertyName();
							targetDataItemKey = DIKey;
							targetSource = claim.getSource().getSourceIdentifier();
							targetBucket = b;
						}
					}
			}
		}
		
		for(SourceClaim claim : targetBucket.getClaims()){
			if(claim.getSource().getTrustworthiness() > maxTrustWorthySourceDI)
				maxTrustWorthySourceDI = claim.getSource().getTrustworthiness();
			if(claim.getSource().getTrustworthiness() < minTrustWorthySourceDI)
				minTrustWorthySourceDI = claim.getSource().getTrustworthiness();
		}
	
		if(originalTrust == ""){
			originalTrust = String.valueOf(trust.get(targetSource));
		}
	
		if(originalConfidence == "" && targetBucket != null){
			originalConfidence = String.valueOf(targetBucket.getConfidence());
		}
		
		nbSS = targetBucket.getNumberOfClaims();
		
		for (ValueBucket b : dataSet.getDataItemsBuckets().get(targetDataItemKey)) {
			totalNumberOfClaims = totalNumberOfClaims + b.getNumberOfClaims();
		}
		
		nbC = totalNumberOfClaims - nbSS;
		
		for(String sourceId : trust.keySet()){
				if(trust.get(sourceId) <= trust.get(targetSource))
					++globalTrustWorthinessComparison;
		}
		
		for (String DIKey : dataSet.getDataItemsBuckets().keySet()){
			for (ValueBucket b : dataSet.getDataItemsBuckets().get(DIKey)) {
				if(b.getConfidence() <= targetBucket.getConfidence())
					++globalConfidenceComparison;
				++nbVal;
			}
		}
		
		String truthLabel = "FALSE";
		
		for (ValueBucket b : dataSet.getDataItemsBuckets().get(targetDataItemKey)) {
			for(SourceClaim claim : b.getClaims()){
				if(trust.get(claim.getSource().getSourceIdentifier()) <= trust.get(targetSource))
					++localTrustWorthinessComparison;
				if(claim.getSource().getSourceIdentifier() == targetSource && claim.isTrueClaimByVoter())
					truthLabel = "TRUE";
			}
			if(b.getConfidence() <= targetBucket.getConfidence())
				++localConfidenceComparison;
		}
		
		nbValDI = dataSet.getDataItemsBuckets().get(targetDataItemKey).size();
		
		try {
			String confidenceResultFile = outputPath + System.getProperty("file.separator") + "Metrics.csv";
			BufferedWriter confidenceWriter;
			confidenceWriter = Files.newBufferedWriter(Paths.get(confidenceResultFile), 
					Globals.FILE_ENCODING, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			CSVWriter csvWriter = new CSVWriter(confidenceWriter, ',');
			writeMetrics(csvWriter,String.valueOf(originalConfidence), String.valueOf(originalTrust), String.valueOf(minTrustWorthySourceDI), String.valueOf(maxTrustWorthySourceDI), String.valueOf(nbSS*100.0/(nbSS+nbC)), String.valueOf(nbC*100.0/(nbSS+nbC)), String.valueOf(nbValDI), String.valueOf(globalConfidenceComparison*100.0/nbVal), String.valueOf(localConfidenceComparison*100.0/nbValDI), String.valueOf(globalTrustWorthinessComparison*100.0/trust.size()), String.valueOf(localTrustWorthinessComparison*100.0/totalNumberOfClaims), String.valueOf(truthLabel));
			confidenceWriter.close();
		}
		catch (IOException e) {
			System.out.println("Cannot write the confidence results");
			e.printStackTrace();
		}
		//System.out.println(originalConfidence+","+originalTrust+","+minTrustWorthySourceDI+","+maxTrustWorthySourceDI+","+nbSS*100.0/(nbSS+nbC)+","+nbC*100.0/(nbSS+nbC)+","+nbValDI+","+globalConfidenceComparison*100.0/nbVal+","+localConfidenceComparison*100.0/nbValDI+","+globalTrustWorthinessComparison*100.0/trust.size()+","+localTrustWorthinessComparison*100.0/totalNumberOfClaims+","+truthLabel);
		//System.out.println(truthLabel+" 1:"+originalConfidence+" 2:"+originalTrust+" 3:"+minTrustWorthySourceDI+" 4:"+maxTrustWorthySourceDI+" 4:"+nbSS*100.0/(nbSS+nbC)+" 5:"+nbC*100.0/(nbSS+nbC)+" 6:"+nbValDI+" 7:"+globalConfidenceComparison*100.0/nbVal+" 8:"+globalTrustWorthinessComparison*100.0/trust.size()+" 9:"+localTrustWorthinessComparison*100.0/totalNumberOfClaims+" 10:"+nbSS+nbC);
		return 1;
	}
		
	public void intializeWithResults(HashMap<Integer, ConfValueLabel> conf, HashMap<String, Double> trust){
		for (List<ValueBucket> bucketsList : dataSet.getDataItemsBuckets().values()) {
			for (ValueBucket b : bucketsList) {
				int claim_id = b.getClaims().get(0).getId();
				double confidence = conf.get(claim_id).getConfidenceValue();
					b.setConfidence(confidence);
					for (SourceClaim claim : b.getClaims()){
						claim.setTrueClaimByVoter(conf.get(claim.getId()).getLabel());
				}
		}
	}
		
		for(Source source : dataSet.getSourcesHash().values()){
			try
			{
				double source_trust = trust.get(source.getSourceIdentifier());
				source.setTrustworthiness(source_trust);
			}
			catch (NullPointerException e)
			{
				//e.printStackTrace();
			}
		}
	}
	
	private static void writeMetrics(CSVWriter writer, String cv, String trust,String minTrust, String maxTrust, String nbSS, String nbC, String nbDI, String cvglobal, String cvlocal, String trustglobal, String trustlocal, String truthLabel) {
		String [] lineComponents = new String[]{cv, trust, minTrust, maxTrust, nbSS, nbC, nbDI, cvglobal, cvlocal, trustglobal, trustlocal, truthLabel};
		writer.writeNext(lineComponents);
	}
}
