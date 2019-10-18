package org.ihtsdo.icdmapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ICDMappingPrototype {

	class Mapping {
		String sourceID;
		String sourceName;
		String targetID;
		String targetName;
		String mappingMethod;

		public String getSourceID() {
			return sourceID;
		}

		public void setSourceID(String sourceID) {
			this.sourceID = sourceID;
		}

		public String getSourceName() {
			return sourceName;
		}

		public void setSourceName(String sourceName) {
			this.sourceName = sourceName;
		}

		public String getTargetID() {
			return targetID;
		}

		public void setTargetID(String targetID) {
			this.targetID = targetID;
		}

		public String getTargetName() {
			return targetName;
		}

		public void setTargetName(String targetName) {
			this.targetName = targetName;
		}

		public String getMappingMethod() {
			return mappingMethod;
		}

		public void setMappingMethod(String mappingMethod) {
			this.mappingMethod = mappingMethod;
		}

	}

	class ICD11Code {
		String foundationURL;
		String code;
		String kind;
		String title;
		String depth;
		String chapter;

		@Override
		public int hashCode() {
			return code.hashCode();
		}

	}

	// Files
	private final String SCTRF2_Con_File = "sct2_Concept_Snapshot_AU1000036_20190731.txt";
	private final String SCTRF2_Desc_File = "sct2_Description_Snapshot-en-AU_AU1000036_20190731.txt";
	private final String SCTTranstiveClosure_File = "tc-full.txt";
	private final String ICD_File = "ICD_exclude_x_26.txt";
	private final String GB_File = "der2_cRefset_LanguageSnapshot-en_INT_20190731.txt";
	private final String mapSIFile = "der2_iisssccRefset_ExtendedMapFull_INT_20190731.txt";
	private final String map11To10File = "11To10MapToOneCategory.xlsx";

	private static Logger logger = Logger.getLogger(ICDMappingPrototype.class.getName());

	Map<String, Boolean> SCTConceptsAll = new HashMap<String, Boolean>();

	Map<String, String> SCTConceptFSN = new HashMap<String, String>();

	Map<String, Set<String>> SCTConceptDescription = new HashMap<String, Set<String>>();

	Map<String, Set<String>> SCTTC = new HashMap<String, Set<String>>();

	Map<String, ICD11Code> icd11 = new HashMap<String, ICD11Code>();
	
	Map<String, Set<String>> concept_ancestor= new HashMap<String, Set<String>>();

	Set<String> gbEnglishSet = new HashSet<String>();

	Set<String> mappingSourceSet = new HashSet<String>();
	Set<String> mappingTargetSet = new HashSet<String>();

	Set<String> exactMapSourceSet = new HashSet<String>();
	Set<String> nearlyMapSourceSet = new HashSet<String>();
	Set<String> wordMapSourceSet = new HashSet<String>();
	
	Map<String, Set<String>> siMapInput = new HashMap<String, Set<String>>();
	Map<String, String> siMap = new HashMap<String, String>();
	Map<String, Set<String>> icdMap = new HashMap<String, Set<String>>();
	
	Map<String, Set<ICD11Code>> exactMapping = new HashMap<String, Set<ICD11Code>>();

	Map<String, Mapping> mappingResult = new HashMap<String, ICDMappingPrototype.Mapping>();

	private static double SCALE_FACTOR = 0.9;

	public void loadRF2SCTFiles(String resourceFileFolder) throws IOException {
		File SCTConRf2File = new File(resourceFileFolder + "/" +  SCTRF2_Con_File);
		List<String> contents = FileUtils.readLines(SCTConRf2File);
		contents.remove(0);
		for (String s : contents) {
			String[] parts = s.split("[\\t]");
			if (parts[2].equals("0")) {
				SCTConceptsAll.put(parts[0], false);
			} else {
				SCTConceptsAll.put(parts[0], true);
			}
			SCTConceptDescription.put(parts[0], new HashSet<String>());
		}
		logger.info("Total SCT concepts (include inactive) loaded " + SCTConceptsAll.size());

		File gbFile = new File(resourceFileFolder + "/" + GB_File);
		for (String s : FileUtils.readLines(gbFile)) {
			String[] chunks = s.split("\\t");
			String did = chunks[5].trim();
			String refSetId = chunks[4].trim();
			if (refSetId.equals("900000000000508004")) {
				gbEnglishSet.add(did);
			}
		}

		logger.info("GB English Set loaded " + gbEnglishSet.size());

		File SCTDescRf2File = new File(resourceFileFolder + "/" + SCTRF2_Desc_File);
		contents = FileUtils.readLines(SCTDescRf2File);
		contents.remove(0);
		for (String s : contents) {
			String[] parts = s.split("[\\t]");
			if (parts[2].equals("1")) {
				if (parts[6].equals("900000000000003001")) {
					SCTConceptFSN.put(parts[4], parts[7]);
				} else {
					if (gbEnglishSet.contains(parts[0])) {
						SCTConceptDescription.get(parts[4]).add(parts[7]);
					}

				}
			}

		}
		logger.info("Total SCT concepts FSN " + SCTConceptFSN.size());
		logger.info("Total SCT descriptions " + SCTConceptDescription.size());

	}

	public void loadMappingSource(String inputFile) throws IOException {
		for (String line : FileUtils.readLines(new File(inputFile))) {
			String id = line.trim();
			if (SCTConceptsAll.containsKey(id)) {
				mappingSourceSet.add(id);
			}
		}
		logger.info("Total SCT Concepts loaded as mapping source " + mappingSourceSet.size());
	}

	private void loadICD11(String resourceFileFolder) throws IOException {
		try {
			File icdFile = new File(resourceFileFolder + "/" + ICD_File);
			for (String s : FileUtils.readLines(icdFile)) {
				String[] chunks = seperateTab(s);
				ICD11Code icdCode = new ICD11Code();
				icdCode.code = chunks[0];
				icdCode.foundationURL = chunks[2];
				icdCode.kind = chunks[3];
				icdCode.depth = chunks[4];
				icdCode.title = chunks[1].trim();
				icdCode.chapter = chunks[5].trim();
				icd11.put(icdCode.title.toLowerCase(), icdCode);
			}
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		logger.info("Load ICD 11 " + icd11.size());
	}
	
	private void loadTC(String resourceFileFolder) throws IOException {
		try {
			File tcFile =new File(resourceFileFolder + "/" + SCTTranstiveClosure_File);
			for (String s : FileUtils.readLines(tcFile)) {
				String[] chunks = seperateTab(s);
				String cid =  chunks[0];
				String aid =  chunks[1];
				if (!concept_ancestor.containsKey(cid)) {
					concept_ancestor.put(cid, new HashSet<String>());
				}
				concept_ancestor.get(cid).add(aid);
			}
		} catch (IOException e) {
		
			e.printStackTrace();
		}
		logger.info("Load TC Table " + concept_ancestor.size());
	}
	
	private void loadMapping(String resourceFileFolder) {
		
		try {
			
			for (String s : FileUtils.readLines(new File(resourceFileFolder + "/" +mapSIFile))) {
				String[] parts = seperateTab(s);
				if(parts[2].equals("1")&&parts[6].equals("1")&&parts[7].equals("1")&& parts[8].trim().equalsIgnoreCase("TRUE")
						&& seperateWhiteSpace(parts[9])[0].equalsIgnoreCase("ALWAYS") && parts[12].trim().equalsIgnoreCase("447637006")) {
					if(!siMapInput.containsKey(parts[5])) {
						siMapInput.put(parts[5], new HashSet<String> ());
					}
					siMapInput.get(parts[5]).add(parts[10]);
				}
			}
			
			for(String k : siMapInput.keySet()) {
				if(siMapInput.get(k).size() ==1) {
					String map = siMapInput.get(k).iterator().next();
					if(! (map.endsWith("8") || map.endsWith("9"))) {
						siMap.put(k , map );
						//System.out.println(k + "\t"+ map);
					}
					
				}
				
			}
			logger.info("Load SI MAP " + siMap.size());
			

			File icd11to10MapFile = new File(resourceFileFolder + "/" + map11To10File);
			for(List<String> line : readXLSXFile(icd11to10MapFile, "11To10MapToOneCategory", false)) {
				String icd10Code = line.get(4).trim();
				String icdtitle = line.get(3).toLowerCase();
				String icd11Code = line.get(1);
				if(!icdMap.containsKey(icd10Code)) {
					icdMap.put(icd10Code, new HashSet<String>());
				}
				icdMap.get(icd10Code).add(icdtitle);
				if(icd11Code.length() > 0) {
					if(!icd11Code.endsWith("Y") &&!icd11Code.endsWith("Z")  ) {
						//System.out.println(icd11Code + "\t"+ icd10Code );
					}
				}
			}

			logger.info("Load ICD MAP " + icdMap.size());
			
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
	} 

	private void mapping(String outPutFile) throws IOException {
		Map<String, List<List<String>>> allContents = new HashMap<String, List<List<String>>>();
		allContents.put("Exact Map", exactMapping());
		allContents.put("Nearly Map", nearlyMapping());
		allContents.put("Word Map", wordMapping());
		allContents.put("Extented Map", extendMapping());
		allContents.put("Pull through Map", pullthroughMapping());
		toXLSXFileMultipleSheets(outPutFile, allContents);
	}

	private List<List<String>> exactMapping() throws IOException {
		List<List<String>> contents = new ArrayList<List<String>>();
		
		for (String sc : mappingSourceSet) {
			for (String sd : SCTConceptDescription.get(sc)) {
				if (icd11.keySet().contains(sd.toLowerCase())) {
					ICD11Code iCode = icd11.get(sd.toLowerCase());
					if (!exactMapping.containsKey(sc)) {
						exactMapping.put(sc, new HashSet<ICD11Code>());
					}
					exactMapping.get(sc).add(iCode);
				}
			}

		}

		for (String sc : exactMapping.keySet()) {
			exactMapSourceSet.add(sc);
			for (ICD11Code iCode : exactMapping.get(sc)) {
				contents.add(getExcelLine(sc, SCTConceptFSN.get(sc), String.valueOf(SCTConceptsAll.get(sc)), iCode.code,
						iCode.title, iCode.kind, iCode.depth, iCode.chapter, "ExactMatch"));

			}
		}

		logger.info("Exact Map Done - size " + contents.size());
		return contents;
	}

	private List<List<String>> nearlyMapping() throws IOException {
		List<List<String>> contents = new ArrayList<List<String>>();

		int count = 0;
		Map<String, Set<ICD11Code>> conceptMapping = new HashMap<String, Set<ICD11Code>>();
		for (String sc : mappingSourceSet) {
			if (count % 1000 == 0)
				logger.info("Processing " + count++);
			if (!exactMapSourceSet.contains(sc)) {
				for (String sd : SCTConceptDescription.get(sc)) {
					String matchString = sd.toLowerCase();
					for (String s : icd11.keySet()) {
						double similarity = SimilarityCalculation.standardLevenshteinDistance(s, matchString);
						if (similarity > 0.9) {
							if (!conceptMapping.containsKey(sc)) {
								conceptMapping.put(sc, new HashSet<ICD11Code>());
							}
							conceptMapping.get(sc).add(icd11.get(s));
						}
					}
				}
			}
		}

		for (String sc : conceptMapping.keySet()) {
			nearlyMapSourceSet.add(sc);
			for (ICD11Code iCode : conceptMapping.get(sc)) {
				contents.add(getExcelLine(sc, SCTConceptFSN.get(sc), String.valueOf(SCTConceptsAll.get(sc)), iCode.code,
						iCode.title, iCode.kind, iCode.depth, iCode.chapter, "Nearly Match"));

			}
		}
		logger.info("Nearly Map Done - size " + contents.size());
		return contents;
	}

	private List<List<String>> wordMapping() throws IOException {

		List<List<String>> contents = new ArrayList<List<String>>();
		Map<String, Set<String>> sctGroups = new HashMap<String, Set<String>>();
		Map<String, Set<String>> icdGroups = new HashMap<String, Set<String>>();

		Map<String, Set<ICD11Code>> conceptMapping = new HashMap<String, Set<ICD11Code>>();

		for (String sc : mappingSourceSet) {
			for (String sd : SCTConceptDescription.get(sc)) {
				{
					// get prefix
					for (String w : sd.split("[\\W]")) {
						String prefix = SimilarityCalculation.getPrefix(w, 3);
						if (prefix != null) {
							String k = prefix.toLowerCase();
							if (!sctGroups.containsKey(k)) {
								sctGroups.put(k, new HashSet<String>());
							}
							sctGroups.get(k).add(w.toLowerCase());
						}
					}
				}
			}
		}
		logger.info("SCT Groups Size " + sctGroups.size());

		for (ICD11Code icd : icd11.values()) {
			for (String w : icd.title.split("[\\W]")) {
				String prefix = SimilarityCalculation.getPrefix(w, 3);
				if (prefix != null) {
					String k = prefix.toLowerCase();
					if (!icdGroups.containsKey(k)) {
						icdGroups.put(k, new HashSet<String>());
					}
					icdGroups.get(k).add(w.toLowerCase());
				}
			}
		}
		logger.info("ICD Groups Size " + icdGroups.size());

		// Pre compute distance score

		Map<String, Double> scores = new HashMap<>();

		for (String grp : sctGroups.keySet()) {
			for (String sct : sctGroups.get(grp)) {
				if (icdGroups.containsKey(grp)) {
					for (String icd : icdGroups.get(grp)) {
						scores.put(sct + "|" + icd, SimilarityCalculation.standardLevenshteinDistance(sct, icd));
					}
				}
			}
		}

		logger.info("Scores Size" + scores.size());

		int count = 0;
		for (String sc : mappingSourceSet) {
			if (!exactMapSourceSet.contains(sc) && !nearlyMapSourceSet.contains(sc)) {
				if (count % 1000 == 0)
					System.out.println(count++);
				for (String sd : SCTConceptDescription.get(sc)) {
					for (String s : icd11.keySet()) {
						ICD11Code iCode = icd11.get(s);
						double similarity = scoreTerms(scores, sd.toLowerCase(), iCode.title.toLowerCase());
						if (similarity > 0.8) {
							if (!conceptMapping.containsKey(sc)) {
								conceptMapping.put(sc, new HashSet<ICD11Code>());
							}
							conceptMapping.get(sc).add(iCode);
						}
					}
				}
			}
		}
		
		for (String sc : conceptMapping.keySet()) {
			for (ICD11Code iCode : conceptMapping.get(sc)) {
				wordMapSourceSet.add(sc);
				contents.add(getExcelLine(sc, SCTConceptFSN.get(sc), String.valueOf(SCTConceptsAll.get(sc)), iCode.code,
						iCode.title, iCode.kind, iCode.depth, iCode.chapter, "Word Match"));

			}
		}

		logger.info("Word Map Done - size " + contents.size());

		return contents;
	}

	private List<List<String>> extendMapping() throws IOException {
		
		List<List<String>> contents = new ArrayList<List<String>>();
		
		Set<String> descendents = new HashSet<String>();
		
		for(String c : concept_ancestor.keySet()) {
			for(String a : concept_ancestor.get(c)) {
				if(exactMapSourceSet.contains(a)) {
					if(!nearlyMapSourceSet.contains(c) && !wordMapSourceSet.contains(c))
					descendents.add(c);
				}
			}
		}
		
		for(String d: descendents) {
			// get lowest mapped ancestor
			Set<String> mappedAncestor = new HashSet<String>();
			for(String a : concept_ancestor.get(d)) {
				
				if(exactMapSourceSet.contains(a)) {
					mappedAncestor.add(a);
				}
			}
			Set<String> removed  = new HashSet<String>();
			for(String a : mappedAncestor) {
				for(String a_a : concept_ancestor.get(a)) {
					if(mappedAncestor.contains(a_a)) {
						removed.add(a_a);
					}
				}
			}
			mappedAncestor.removeAll(removed);
			for(String m : mappedAncestor) {
				for (ICD11Code iCode : exactMapping.get(m)) { 
					contents.add(getExcelLine(d, SCTConceptFSN.get(d), String.valueOf(SCTConceptsAll.get(d)), iCode.code,
							iCode.title, iCode.kind, iCode.depth, iCode.chapter, "Map Inheritance"));
				}
				
			}
		}
		
		logger.info("Total Exact Map Descendents - size " + descendents.size());
		
		return contents;
	}
	
	private List<List<String>> pullthroughMapping() throws IOException {
	
		List<List<String>> contents = new ArrayList<List<String>>();
		
		for (String sc : mappingSourceSet) {
			if(siMap.containsKey(sc)) {
				String icd10 = siMap.get(sc);
				if(icdMap.containsKey(icd10)) {
					for(String icd :  icdMap.get(icd10)) {
						ICD11Code iCode = icd11.get(icd);
						if(iCode!= null && ! (iCode.code.endsWith("Z") ||iCode.code.endsWith("Y") )) {
							contents.add(getExcelLine(sc,SCTConceptFSN.get(sc), String.valueOf(SCTConceptsAll.get(sc)),
									iCode.code, iCode.title, iCode.kind, iCode.depth, iCode.chapter, "Pull Through"));
						}
					}
				}
				
			}
		}
		
		//logger.info("Total Pull Through Mapping - size ";
		return contents;
	}

	private File getFileFromResources(String fileName) {

		ClassLoader classLoader = getClass().getClassLoader();

		URL resource = classLoader.getResource(fileName);
		if (resource == null) {
			throw new IllegalArgumentException("file is not found!");
		} else {
			return new File(resource.getFile());
		}

	}

	private String[] seperateTab(String s) {
		return s.split("\\t");
	}

	private String[] seperateWhiteSpace(String s) {
		return s.split("[\\W]");
	}
	
	private List<List<String>> readXLSXFile(File file, String sheetName, boolean heading) {
		List<List<String>> lines = new ArrayList<>();
		try {
			// Get the workbook instance for XLS file
			XSSFWorkbook workbook = new XSSFWorkbook(file);
			XSSFSheet sheet = workbook.getSheet(sheetName);

			Iterator<Row> rowIterator = sheet.iterator();
			if (heading) {
				rowIterator.next(); // skip heading
			}
			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				List<String> rowString = new LinkedList<>();

				for (int i = 0; i < row.getLastCellNum(); i++) {
					String cellCountent = "" ;
					if (row.getCell(i) != null && row.getCell(i).getCellType() == 0) {
						cellCountent = String.format("%.0f", row.getCell(i).getNumericCellValue());

					} else if (row.getCell(i) != null && row.getCell(i).getCellType() == 1)  {
						cellCountent = row.getCell(i).getStringCellValue();
					}
					rowString.add(cellCountent);
				}
				lines.add(rowString);
			}

			workbook.close();
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		} catch (InvalidFormatException e) {
			e.printStackTrace();
		}
		return lines;

	}
	
	private double scoreTerms(Map<String, Double> scores, String sctTerm, String icdTerm) {
		final String[] sctWords = sctTerm.split("[\\W]");
		final String[] icdWords = icdTerm.split("[\\W]");

		double score = 0;
		for (String sctWord : sctWords) {
			double wordScore = 0;
			for (String icdWord : icdWords) {
				Double s = scores.get(sctWord + "|" + icdWord);
				// s will be null if the words do not share a 3 char prefix
				if (s != null) {
					wordScore = Math.max(wordScore, s);
				}
			}
			score += wordScore;
		}

		score = score / sctWords.length;
		if (icdWords.length > sctWords.length) {
			score = score * Math.pow(SCALE_FACTOR, icdWords.length - sctWords.length);
		}

		return score;
	}

	public void toXLSXFileMultipleSheets(String fileName, Map<String, List<List<String>>> map) {

		FileUtils.deleteQuietly(new File(fileName));

		try {
			XSSFWorkbook workbook = new XSSFWorkbook();

			for (String sheetName : map.keySet()) {
				XSSFSheet sheet = workbook.createSheet(sheetName);
				System.out.println("Create Sheet : " + sheetName + " with row count " + map.get(sheetName).size());
				int rowNum = 0;
				for (List<String> line : map.get(sheetName)) {
					Row row = sheet.createRow(rowNum++);
					for (int i = 0; i < line.size(); i++) {
						row.createCell(i).setCellValue(line.get(i));
					}
				}
			}

			FileOutputStream outputStream = new FileOutputStream(fileName);
			workbook.write(outputStream);
			workbook.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		logger.info("\nOutput to file : " + fileName);
	}

	public static List<String> getExcelLine(String... strings) {
		List<String> list = new ArrayList<>();
		for (String s : strings) {
			list.add(s);
		}
		return list;
	}

	public void run(String resouceFileFoler, String inputFile, String outPutFile) {

		logger.info("Output file is : " + outPutFile);

		try {
			logger.info("Load SCT");
			loadRF2SCTFiles(resouceFileFoler);
			logger.info("Load ICD");
			loadICD11(resouceFileFoler);
			logger.info("Load Mapping");
			loadMapping(resouceFileFoler);
			logger.info("Load TC");
			loadTC(resouceFileFoler);
			logger.info("Load Mapping Source");
			loadMappingSource(inputFile);
			logger.info("Run Mapping");
			mapping(outPutFile);

		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		ICDMappingPrototype icdMappingPrototype = new ICDMappingPrototype();
		logger.info("ICD Mapping ProtoType");
		icdMappingPrototype.run(args[0], args[1], args[2]);
	}
}
