package afs;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CyclicBarrier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import Decoder.BASE64Encoder;

// Extend HttpServlet class
public class Affiliations extends HttpServlet {
 
  /**
	 * 
	 */
	
	private static String UniqueUserId, AccountKey, Dataset, PathtoPhantomJS, TempJavaScriptFile;

  public void init() throws ServletException {
      // Do required initialization
	  UniqueUserId = "dff5611f-c676-4a02-b772-ea2209fa8d1c";
	  AccountKey = "brnd6tC0e9rkOvUNYypQQaG6y26juT/hREoQzf/yjwc";
	  Dataset = "https://api.datamarket.azure.com/MRC/MicrosoftAcademic/v2/";
//	  PathtoPhantomJS = "/home/kostia/vu/BP/project/tmp/phantomjs/phantomjs-1.9.7-linux-x86_64/bin/";
	  TempJavaScriptFile = "/tmp/bp00001.js";
	  PathtoPhantomJS = "/home/kos/phantomjs/phantomjs-1.9.7-linux-x86_64/bin/";
	  
  }

  
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	  response.setContentType("text/html");
      
      StringBuilder str = new StringBuilder();
      Result result = new Result();
      
      ArrayList<Author> authors;
      ArrayList<Paper> papers;
      String[] data;
      PrintWriter out = response.getWriter();
      String[] query = request.getParameter("inputName").split(" ");
		
      if (query.length == 2) {
    	  out.println("Name is ambigue. Use at least two words, i.e. give first and last name.");
    	  out.close();
    	  return;
      }
			
      String authorID = query[0];
		
      for (int i = 1; i < query.length; i++) {
    	  if (query[i].contains("http://")) { break; }
    	  str.append(query[i] + " ");
      }
		
      String queryAuthorName = str.toString();
      str = new StringBuilder();
			
      data = getStuff(Dataset + "Author?$filter=Name%20eq%20%27" + queryAuthorName.replaceAll(" ", "%20") + "%27");
      if (data.length <= 1) {
    	  str.append("No match for this author. Doing usual MAS search to get exact profile name... <br />");
    	  String profileName = getExactProfileName(queryAuthorName);
    	  if (profileName.equals("No exact match for name")) {
    		  out.println("No exact match for name");
    		  out.close();
    		  return;
    	  }
    	  if (profileName.equals("MAS is probably down at the moment. Try later.")) {
    		  out.println("MAS is probably down at the moment. Try later. You can check if http://academic.research.microsoft.com/ is up");
    		  out.close();
    		  return;
    	  }
    	  str.append("Matched: <br />");
    	  str.append(profileName);
			
    	  data = getStuff(Dataset + "Author?$filter=Name%20eq%20%27" + profileName.replaceAll(" ", "%20") + "%27");
			
    	  if (data.length <= 1) {
    		  out.println("No match, try adjusting query.");
    		  out.close();
    		  return;
    	  }
      }
      
      result.setStartText(str.toString());
      str = new StringBuilder();
		
      authors = parseAuthors(data, new ArrayList<Author>());
		
      ArrayList<String> as = new ArrayList<String>();
		
      if (authors.size() > 1) {
    	  str.append("Multiple authors match:");
    	  result.setStartText(str.toString());
    	  str = new StringBuilder();
			
    	  for (int i = 0; i < authors.size(); i++) {
    		  str.append(authors.get(i).getAuthorId() + " " 
						+ authors.get(i).getAuthorName() + " " 
						+ authors.get(i).getPersonalPage() + " ");
				
    		  as.add(authors.get(i).getAuthorId() + " " 
					+ authors.get(i).getAuthorName() + " " 
					+ authors.get(i).getPersonalPage() + " ");
    	  }
      } else {
    	  str.append(authors.get(0).getAuthorId() + " " 
    			  	+ authors.get(0).getAuthorName() + " " 
					+ authors.get(0).getPersonalPage());
    	  as.add(authors.get(0).getAuthorId() + " " 
    			 + authors.get(0).getAuthorName() + " " 
    			 + authors.get(0).getPersonalPage());
      }
			
      int authorChoice = 0;
		
      if (!authorID.equals("0")) {
    	  for (Author a : authors) {
    		  if (a.getAuthorId().equals(authorID)) {
    			  authorChoice = authors.indexOf(a);
    			  break;
    		  }
    	  }
      }
		
      result.setAuthors(as);
      str = new StringBuilder();
		
      str.append("General Keywords for this author: ");
			
//      for (String s : findKeywordsScholarSearch(authors.get(authorChoice).getAuthorName())) {
//    	  authors.get(authorChoice).addKeyword(s);
//      }
		
      if (authors.get(authorChoice).getKeywords().size() == 0) {
    	  str.append("Not found anything.");
      } else {
    	  str.append("Words found: ");
    	  for (String s : authors.get(authorChoice).getKeywords()) { str.append(s); str.append("# "); }
      }
			
      result.setKeywords(str.toString());
      str = new StringBuilder();
		
      data = getStuff(Dataset + "Paper_Author?$filter=AuthorID%20eq%20" + authors.get(authorChoice).getAuthorId());
      papers = parsePapers(data, new ArrayList<Paper>());
		
      if (papers.size() == 0) {
    	  str.append("No papers found for this author entry. Try selecting other ID's or adjust name searched.");
    	  result.setDataText(str.toString());
			
    	  Gson gson = new Gson(); 
    	  JsonObject msg = new JsonObject();
			
    	  JsonElement r = gson.toJsonTree(result);
    	  msg.addProperty("success", true);
    	  msg.add("results", r);
    	  out.println(msg.toString());
    	  out.close();
			
      } else {
			
    	  for (Paper p : papers) {
    		  data = getStuff(Dataset + "Paper?$filter=ID%20eq%20" + p.paperID);
    		  p.setYear(parseYear(data));
    		  p.setKeywords(parsePaperKeywords(data));
    		  data = getStuff(Dataset + "Paper_Author?$filter=PaperID%20eq%20" + p.getPaperID());
    		  p.setCoAffiliations(parseCoAffiliations(data, new ArrayList<String>(), authors.get(authorChoice).getAuthorId()));
    	  }
				
    	  Collections.sort(papers);
    	  
    	  for (int i = 0; i < papers.size(); i++) {
    		  if (!papers.get(i).getKeywords().isEmpty()) {
    			  result.addKeywordsChange(papers.get(i).getYear() + " " + papers.get(i).getKeywords());
    		  }
    	  }
    	  
    	  ArrayList<WorkPlace> moves = new ArrayList<WorkPlace>();
			
    	  for (int i = 0; i < papers.size(); i++) {
    		  WorkPlace place = new WorkPlace();
    		  int startYear = papers.get(i).getYear();
				
    		  do {
    			  place.addPaper(papers.get(i));
					
    			  for (String s : papers.get(i).getCoAffiliations()) {
    				  if (s.equals(papers.get(i).getAffiliationID())) { continue; }
							
    				  if (place.getWorkedWith().containsKey(s)) {
    					  place.updateWorkedWith(s, place.getWorkedWith().get(s) + 1);
    				  } else {
    					  place.updateWorkedWith(s, 1);
    				  }
    			  }
					
    			  i++;
    		  } while (i < papers.size() && 
    				  papers.get(i - 1).getAffiliationID().equals(papers.get(i).getAffiliationID()));
				
    		  place.setInstitution(papers.get(i - 1).getAffiliationID(), startYear, papers.get(i - 1).getYear());
    		  i--;
				
    		  moves.add(place);
    	  }
				
    	  result.setDataText(str.toString());
    	  Map <String, Institution> cacheAffiliations = new HashMap<String, Institution>();
				
    	  for (WorkPlace w : moves) {
    		  ReturnFormattedWorkPlace wp = new ReturnFormattedWorkPlace();
    		  str.append("Workplace: ");
    		  Institution i = new Institution();
    		  if (!cacheAffiliations.containsKey(w.getAffiliationID())) {
    			  data = getStuff(Dataset + "Affiliation?$filter=ID%20eq%20" + w.getAffiliationID());
    			  i.set(parseAffiliation(data), parseLatitude(data), parseLongitude(data), "0");
    			  cacheAffiliations.put(w.getAffiliationID(), i);
    			  
    			  wp.setWorkPlace(i);
    		  } else {
    			  wp.setWorkPlace(cacheAffiliations.get(w.getAffiliationID()));
    		  }
				
    		  wp.setPeriod(w.getYearFrom() + " " + w.getYearTill());
    		  str.append("Worked with: ");
    		  ArrayList<Institution> ww = new ArrayList<Institution>();
    		  
    		  for (Map.Entry<String, Integer> entry : w.getWorkedWith().entrySet()) {
    			  i = new Institution();
    			  if (!cacheAffiliations.containsKey(entry.getKey())) {
    				  data = getStuff(Dataset + "Affiliation?$filter=ID%20eq%20" + entry.getKey());
    				  i.set(parseAffiliation(data), parseLatitude(data), parseLongitude(data), entry.getValue().toString());
        			  cacheAffiliations.put(entry.getKey(), i);
    				  ww.add(cacheAffiliations.get(entry.getKey()));
    			  } else {
    				  ww.add(cacheAffiliations.get(entry.getKey()));
    			  }
    		  }
				
    		  wp.setWorkedWith(ww);
    		  result.addData(wp);
    	  }

    	  Gson gson = new Gson(); 
		  JsonObject msg = new JsonObject();
			
		  JsonElement r = gson.toJsonTree(result);
		  msg.addProperty("success", true);
		  msg.add("results", r);
		  out.println(msg.toString());
		  out.close();
      }

  }
  
  
//######## INNER OBJECTS: ########
	
	static class Author {
	    private String name, id, personalPage;
	    private ArrayList<String> keywords = new ArrayList<String>();
	    
	    public void setPersonalPage(String personalPage) { this.personalPage = personalPage; }
	    public void setAuthorName(String name) { this.name = name; }
	    public void setAuthorId(String id) { this.id = id; }
	    public void addKeyword(String word) { this.keywords.add(word); }
	    public String getAuthorId() { return id; }
	    public String getAuthorName() { return name; }
	    public String getPersonalPage() { return personalPage; }
	    public ArrayList<String> getKeywords() { return keywords; }
	}
	
	static class Paper implements Comparable<Paper> {
		private int year;
		private String paperID, affiliationID, keywords;
		private ArrayList<String> coAffiliationsIDs = new ArrayList<String>();
		
		public void setAffiliationID(String affiliationID) { this.affiliationID = affiliationID; }
		public void setPaperID(String paperID) { this.paperID = paperID; }
		public void setCoAffiliations(ArrayList<String> coaffiliations) { 
			for (String s : coaffiliations) {
				coAffiliationsIDs.add(s);
			}
		}
		public void setYear(int year) { this.year = year; }
		public void setKeywords(String s) { this.keywords = s; }
		public String getPaperID() { return paperID; }
		public String getAffiliationID() { return affiliationID; }
		public ArrayList<String> getCoAffiliations() { return coAffiliationsIDs; }
		public int getYear() { return year; }
		public String getKeywords() { return keywords; }
		
		public int compareTo(Paper p) {
			final int BEFORE = -1;
		    final int EQUAL = 0;
		    final int AFTER = 1;
		    
		    if (this.year < p.getYear()) return BEFORE;
		    if (this.year > p.getYear()) return AFTER;
		    
			return EQUAL;
		}
	}
	
	static class WorkPlace {
		private int  yearFrom, yearTill;
		private String affiliationID;
		private Map<String, Integer> workedWith = new HashMap<String, Integer>();
		private ArrayList<Paper> papers = new ArrayList<Paper>();
		
		public void setInstitution(String affiliationID, int yearFrom, int yearTill) {
			this.affiliationID = affiliationID;
			this.yearFrom = yearFrom;
			this.yearTill = yearTill;
		}
		public void updateWorkedWith(String affiliationID, int numberOfPapers) { workedWith.put(affiliationID, numberOfPapers); }
		public void addPaper(Paper p) { papers.add(p); }
		public String getAffiliationID() { return affiliationID; }
		public int getYearFrom() { return yearFrom; }
		public int getYearTill() { return yearTill; }
		public Map<String, Integer> getWorkedWith() { return workedWith; }
		public ArrayList<Paper> getPapers() { return papers; }
	}
	
	static class Result {
		private String speedup, starttext, keywords, datatext;
		private ArrayList<String> authors = new ArrayList<String>();
		private ArrayList<String> keywordsChanges = new ArrayList<String>();
		private ArrayList<ReturnFormattedWorkPlace> data = new ArrayList<ReturnFormattedWorkPlace>();
		
		public void setStartText(String s) { this.starttext = s; }
		public void setAuthors(ArrayList<String> s) { this.authors = s; }
		public void setKeywords(String s) { this.keywords = s; }
		public void addData(ReturnFormattedWorkPlace s) { this.data.add(s); }
		public void addKeywordsChange(String s) { keywordsChanges.add(s); }
		public void setSpeedUp(String s) { this.speedup = s; }
		public void setDataText(String s) { this.datatext = s; }
		public ArrayList<String> getAuthors() { return authors; }
		public String getKeywords() { return keywords; }
		public ArrayList<String> getKeywordsChanges() { return keywordsChanges; }
		public ArrayList<ReturnFormattedWorkPlace> getData() { return data; }
		public String getStartText() { return starttext; }
		public String getSpeedUp() { return speedup; }
		public String getDataText() { return datatext; }
	}
	
	static class Institution {
		private String name, latitude, longitude, connections;
		
		public void set(String name, String latitude, String longitude, String connections) {
			this.name = name;
			this.latitude = latitude;
			this.longitude = longitude;
			this.connections = connections;
		}
		public String getName() { return name; }
		public String getLatitude() { return latitude; }
		public String getLlongitude() { return longitude; }
		public String getConnections() { return connections; }
	}
	
	static class ReturnFormattedWorkPlace {
		private Institution workplace;
		private String period;
		private ArrayList<Institution> workedWith = new ArrayList<Institution>();
		
		public void setWorkPlace(Institution s) { this.workplace = s; }
		public void setPeriod(String s) { this.period = s; }
		public void setWorkedWith(ArrayList<Institution> ww) { this.workedWith = ww; }
		public Institution getWorkPlace() { return workplace; }
		public String getPeriod() { return period; }
		public ArrayList<Institution> getWorkedWith() { return workedWith; }
	}
	
	// ######## QUERY-METHODS: ########

	private String[] getStuff(String queryURL) {
		try {
			
			URL _url = new URL(queryURL);
			URLConnection _urlConn = _url.openConnection();
			BASE64Encoder encoder = new BASE64Encoder();
	        String credential = UniqueUserId + ":" + AccountKey;
	        String credentialBase64 = (encoder.encode(credential.getBytes())).replaceAll("\\s", "");
	        
	        _urlConn.setRequestProperty("accept", "*/*");
	        _urlConn.addRequestProperty("Authorization", "Basic " + credentialBase64);
	        
	        BufferedReader br = new BufferedReader(new InputStreamReader(_urlConn.getInputStream()));
	 
	        String line = null;
	        StringBuilder strBuilder = new StringBuilder();
	        
	        while ((line = br.readLine()) != null) {
	            strBuilder.append(line);
	        }
	        
	        return strBuilder.toString().split("<m:properties>");
	        
	    } catch (MalformedURLException ex) {
	        ex.printStackTrace();
	    } catch (IOException ex) {
	        ex.printStackTrace();
	    } catch (Exception ex) {
	        ex.printStackTrace();
	    }

		String[] error = {"errormsg"};
		
		return error;
	}
	
	
	private String getExactProfileName(String inputName) throws IOException {
		try {
			Document doc = Jsoup.connect("http://academic.research.microsoft.com/Search?query=" + inputName.replaceAll(" ", "%20"))
			.userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/36.0.1985.125 Safari/537.36")
			.timeout(30000)
			.get();
			
			Elements paperItems = doc.getElementsByClass("author-name-tooltip");
			String name[] = inputName.split(" ");
			
			for (Element e : paperItems) {
				int i = 0;
				
				while (e.text().contains(name[i])) {
					i++;
					if (i == name.length) { 
						Document exactName = Jsoup.connect(e.attributes().toString().split("href=")[1].replaceAll("\"", ""))
						.userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/36.0.1985.125 Safari/537.36")
						.timeout(30000)
						.get();
						
						return exactName.title();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return "MAS is probably down at the moment. Try later.";
			
		}
		
		return "No exact match for name";
	}
	
	
	private ArrayList<String> findKeywordsScholarSearch(String inputName) throws IOException {
		ArrayList<String> words = new ArrayList<String>();
		String _url = "http://scholar.google.com";
		
		try {
			
				Document doc = Jsoup.parse(usePhantomJS(_url + "/scholar?q=" + inputName.replaceAll(" ", "+")));
				Elements confused = doc.getElementsByClass("gs_red");
				
				for (Element e : confused) {
					if (e.text().contains("Did you mean:")) {
						for (Element c : doc.getElementsByClass("gs_pda")) {
							inputName = c.text();
							doc = Jsoup.parse(usePhantomJS(_url + "/scholar?q=" + inputName.replaceAll(" ", "+")));
						}
	
						break;
					}
				}
				
				Elements profiles = doc.getElementsByClass("gs_rt2");
				String name[] = inputName.split(" ");
				
				if (profiles.size() > 0) { 
					for (Element e : profiles) {
						for (Element c : e.children()) {
							int i = 0;
							
							while (c.text().toLowerCase().contains(name[i].toLowerCase())) {
								i++;
								
								if (i == name.length) {
									Document wordsHTML = Jsoup.parse(usePhantomJS(_url + c.attributes().toString().split("href=")[1].replaceAll("\"", ""))); 
									Element wordsToParse = wordsHTML.getElementById("cit-int-read");
									
									for (String w : wordsToParse.text().split("-")) { 
										if (w.charAt(0) == ' ') { w = w.substring(1); }
										words.add(w); 
									}
									
									return words;
								}
							}
						}
					}
				}	 
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(0);
			}
		
		return words;
	}
	

	
	// ######## HELP-METHODS: ########
	
	private ArrayList<String> parseCoAffiliations(String[] data, ArrayList<String> array, String authorID) {
		for (int i = 1; i < data.length; i++) {
			if (!GetStringBetween(data[i], "<d:AuthorID m:type=\"Edm.Int32\">", "</d:AuthorID>").equals(authorID)) {
				String s = GetStringBetween(data[i], "<d:AffiliationID m:type=\"Edm.Int32\">", "</d:AffiliationID>");
				if (!s.equals("0")) { array.add(s); }
			}
		}
		
		return array;
	}
	
	private int parseYear(String[] data) {
		return Integer.parseInt(GetStringBetween(data[1], "<d:Year m:type=\"Edm.Int32\">", "</d:Year>"));
	}
	
	private String parsePaperKeywords(String[] data) {
		if (data[1].contains("Keyword")) {
			String[] keywords = GetStringBetween(data[1], "<d:Keyword", "</d:Keyword>").split(">");
			String result = "";
			
			if (keywords.length > 1) { 
				if (keywords[1].contains(":")) {
					result = keywords[1].split(":")[1];
				} else {
					result = keywords[1];
				}
				
				Pattern sup = Pattern.compile(",|;|\\.|\\|");
				String[] tmp = sup.split(result);
				
				StringBuilder str = new StringBuilder();
				
				for (String s : tmp) {
					if (s.contains("CRL")) { continue; }
					
					Pattern pat = Pattern.compile("[\\w]-[\\w]");
					Matcher m = pat.matcher(s);
					while (m.find()) { 
						s = s.substring(0, m.start() + 1) + " " + s.substring(m.end() - 1, s.length());
					}
					
					pat = Pattern.compile("[\\w]-[\\s]");
					m = pat.matcher(s);
					while (m.find()) { 
						s = s.substring(0, m.start() + 1) + s.substring(m.end(), s.length());
					}
					
					if (s.split(" ").length < 8 && s.length() > 2) {
						str.append(s + "; ");
					}
				}
				
				if (result.length() > 5) {
					return str.toString();
				}
			} 
		}
		
		return "";
	}
	
	private String parseAffiliation(String[] data) {
		return GetStringBetween(data[1], "<d:OfficialName>", "</d:OfficialName>");
	}
	
	private String parseLatitude(String[] data) {
		return GetStringBetween(data[1], "<d:Latitude m:type=\"Edm.Double\">", "</d:Latitude>");
	}
	
	private String parseLongitude(String[] data) {
		return GetStringBetween(data[1], "<d:Longitude m:type=\"Edm.Double\">", "</d:Longitude>");
	}
	
	private ArrayList<Paper> parsePapers(String[] data, ArrayList<Paper> array) {
		for (int i = 1; i < data.length; i++) {
           Paper paper = new Paper();
           paper.setPaperID(GetStringBetween(data[i], "<d:PaperID m:type=\"Edm.Int32\">", "</d:PaperID>"));
           paper.setAffiliationID(GetStringBetween(data[i], "<d:AffiliationID m:type=\"Edm.Int32\">", "</d:AffiliationID>"));
           if (!paper.getAffiliationID().equals("0")) {
           	array.add(paper);
           }
       }
		
		return array;
	}
	
	private ArrayList<Author> parseAuthors(String[] data, ArrayList<Author> array) {
		for (int i = 1; i < data.length; i++) {
           Author author = new Author();
           author.setAuthorId(GetStringBetween(data[i], "<d:ID m:type=\"Edm.Int32\">", "</d:ID>"));
           author.setAuthorName(GetStringBetween( data[i], "<d:Name>", "</d:Name>"));
           author.setPersonalPage(GetStringBetween(data[i], "<d:Homepage>", "</d:Homepage>"));
           array.add(author);
       }
		
		return array;
	}
	
	private static String GetStringBetween(String src, String start, String end) {
	    StringBuilder sb = new StringBuilder();
	    int startIdx = src.indexOf(start) + start.length();
	    int endIdx = src.indexOf(end);
	    while (startIdx < endIdx) {
	        sb.append(String.valueOf(src.charAt(startIdx)));
	        startIdx++;
	    }
	    return sb.toString();
	}
	

	private String usePhantomJS(String url) throws IOException, InterruptedException {
		String[] parseHTML = { "var page = require('webpage').create();",
								"page.open('", 
								"', function() {",
								"console.log(page.content);",
								"phantom.exit();",
								"});",
		};
		
		String filename = TempJavaScriptFile;
		
		File f = new File(filename);
		if (f.exists()) {
			Runtime.getRuntime().exec("rm " + filename);
		}
		PrintWriter writer = new PrintWriter(f, "UTF-8");
		
		for (int i = 0; i < parseHTML.length; i++) { 
			if (parseHTML[i].contains("page.open")) {
				String tmp = parseHTML[i] + url + parseHTML[i + 1];
				i++;
				writer.println(tmp);
			} else {
				writer.println(parseHTML[i]);
			}
		}
		writer.close();
		
		Process p = Runtime.getRuntime().exec(PathtoPhantomJS + "phantomjs " + filename);
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line = "";
		String output = "";
	
		while ((line = br.readLine()) != null) {
			output += line + "\n";
		}
		
		Runtime.getRuntime().exec("rm " + filename);
	
		return output;
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException
			{
			    doGet(request, response);
	}
  
  
	public void destroy() {
	      // do nothing.
	}
}
