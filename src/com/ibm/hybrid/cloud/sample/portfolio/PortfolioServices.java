package com.ibm.hybrid.cloud.sample.portfolio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

//JSON-P (JSR 353).  The replaces my old usage of IBM's JSON4J (package com.ibm.json.java)
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.servlet.http.HttpServletRequest;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class PortfolioServices {
	//private static final String QUOTE_SERVICE = "http://localhost:9081/stock-quote";
	//private static final String LOYALTY_SERVICE = "http://localhost:9082/loyalty";
	private static final String QUOTE_SERVICE = "http://stock-quote-service:9081/stock-quote";
	private static final String LOYALTY_SERVICE = "http://loyalty-level-service:9082/loyalty";
	private static final String DELETE_PORTFOLIO_REST_SERVICE = "https://192.168.0.22:9446/trader/deletePortfolio/";
	private static final String CREATE_PORFOLIO_REST_SERVICE = "https://192.168.0.22:9446/trader/addPortfolio?owner=";
	private static final String GET_PORTFOLIO_REST_SERVICE = "https://192.168.0.22:9446/trader/viewPortfolio/";
	private static final String GET_ALL_PORTFOLIO_REST_SERVICE = "https://192.168.0.22:9446/trader/queryPortfolio";
	private static final String ADD_STOCK_TO_PORFOLIO = "https://192.168.0.22:9446/trader/addStock";
	private static final String TEST_PRICE = "100.00";
	
	public static JsonArray getPortfolios() {
		JsonStructure rawPortfolios = null;
		JsonArrayBuilder builder = Json.createArrayBuilder();
		JSONObject inJson = new JSONObject();
		try {
			rawPortfolios = (JsonStructure) invokeREST("GET", GET_ALL_PORTFOLIO_REST_SERVICE);
			String listPortfolioData = rawPortfolios.toString();
			inJson = JSONObject.parse(listPortfolioData) ;
			} catch (IOException e) {
				System.out.println("Exception parsing JSON: "+e);
			}
			JSONArray portfolioArray = cleanPorfolioJson(inJson);
			
			for(int i=0; i < portfolioArray.size(); i++) {
				JsonObjectBuilder portfolio = Json.createObjectBuilder();
				String owner = (String)((JSONObject) portfolioArray.get(i)).get("owner");
				portfolio.add("owner", owner);
				Object total = ((JSONObject) portfolioArray.get(i)).get("total");
				if (total != null && total.getClass().equals(Double.class)) {
				double doubleTotal = (Double)((JSONObject) portfolioArray.get(i)).get("total");
				portfolio.add("total", doubleTotal);
				}
				else if(total.getClass().equals(Long.class)) {
				long longTotal = (Long)((JSONObject) portfolioArray.get(i)).get("total");	
				portfolio.add("total", longTotal);
				}
				String loyalty = (String)((JSONObject) portfolioArray.get(i)).get("loyalty");
				portfolio.add("loyalty", loyalty);

				builder.add(portfolio);
			}
			return builder.build();
	}
	
	private static JSONArray cleanPorfolioJson(JSONObject inJson) {
		
		JSONObject j1 = (JSONObject)inJson.get("ISTPOMAOperationResponse") ;
		JSONObject j2 = (JSONObject)j1.get("ist_portfolio_manager") ;
		JSONArray ja1i = (JSONArray)j2.get("portfolioOutput") ;
		JSONArray ja1o = stripArray(ja1i, "owner", "") ;
		return ja1o;
	}
	
	private static JSONArray stripArray(JSONArray inArray, String key, String val) {
		JSONArray outArray = new JSONArray() ;
		for (int i = 0; i < inArray.size(); i++) {
			JSONObject ja1o = (JSONObject)inArray.get(i) ;
			if (!((String)ja1o.get(key)).equals(val)) {		// cosmetic but ... going thru whole array in case some data below, unlikely
				outArray.add(ja1o) ;
			}
		}
		return outArray ;
	}
	
	private static JsonArray stripArray(JsonArray inArray, String key, String val) {
		JsonArrayBuilder outArray = Json.createArrayBuilder();
		for (int i = 0; i < inArray.size(); i++) {
			JsonObject ja1o = (JsonObject)inArray.get(i) ;
			if (!((String)ja1o.getString(key)).equals(val)) {		// cosmetic but ... going thru whole array in case some data below, unlikely
				outArray.add(ja1o) ;
			}
		}
		return outArray.build() ;
	}
	
	
	
	public static JsonObject individualPorfolio(JsonObject inJson) {
		JsonObject j1 = (JsonObject)inJson.get("ISTPOMAOperationResponse") ;
		JsonObject j2 = (JsonObject)j1.get("ist_portfolio_manager") ;
		JsonObject portfolioInfo = (JsonObject)j2.get("portfolioInfo") ;
		return portfolioInfo;
	}
	
	public static boolean hasLoyaltyChanged(JsonObject inJson) {
		boolean hasLoyalyChanged = false;
		JsonObject j1 = (JsonObject)inJson.get("ISTPOMAOperationResponse") ;
		JsonObject j2 = (JsonObject)j1.get("ist_portfolio_manager") ;
		String oldLoyalty = j2.getJsonString("oldLoyalty").getString();
		JsonObject portfolioInfo = (JsonObject)j2.get("portfolioInfo") ;
		String currentLoyalty = portfolioInfo.getString("loyalty");
		if (oldLoyalty != "" && oldLoyalty != currentLoyalty) {
			hasLoyalyChanged = true;
		}
		return hasLoyalyChanged;
	}
	
	public static JsonArray individualStockInfo(JsonObject inJson) {
		JsonObject j1 = (JsonObject)inJson.get("ISTPOMAOperationResponse") ;
		JsonObject j2 = (JsonObject)j1.get("ist_portfolio_manager") ;
		JsonArray stockInfo = (JsonArray)j2.get("stockInfo") ;
		JsonArray cleanStockInfo = stripArray(stockInfo, "symbol", "") ;
		return cleanStockInfo;
	}

	public static JsonObject getPortfolio(String owner) {
		JsonObject portfolio = null;

		try {
			portfolio = (JsonObject) invokeREST("GET", GET_PORTFOLIO_REST_SERVICE+owner);
		} catch (Throwable t) {
			t.printStackTrace();
		}

		return portfolio;
	}

	public static JsonObject createPortfolio(String owner) {
		JsonObject portfolio = null;

		try {
			portfolio = (JsonObject) invokeREST("POST", CREATE_PORFOLIO_REST_SERVICE+owner);
		} catch (Throwable t) {
			t.printStackTrace();
		}

		return portfolio;
	}
	
	public static JsonObject updatePortfolio(String owner, String symbol, int shares) {
		JsonObject portfolio = null;
		String quote = null;
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String todayDate = dateFormat.format(date);
		try {
			 JsonObject quandlQuote = (JsonObject) getStockQuote(symbol);
			 if(quandlQuote != null) {
				  quote = ((JsonNumber) quandlQuote.get("price")).toString();
			 } else {
				  quote = TEST_PRICE;
			 }
			 String uri = ADD_STOCK_TO_PORFOLIO+"?owner="+owner+"&symbol="+symbol+"&shares="+shares+"&quote="+quote+"&dateQuoted="+todayDate;
			 
			portfolio = (JsonObject) invokeREST("POST", uri);
			
			if(portfolio != null) {
			JsonObject j1 = (JsonObject)portfolio.get("ISTPOMAOperationResponse") ;
			JsonObject j2 = (JsonObject)j1.get("ist_portfolio_manager") ;
			String oldLoyalty = j2.getJsonString("oldLoyalty").getString();
			JsonObject portfolioInfo = (JsonObject)j2.get("portfolioInfo") ;
			String currentLoyalty = portfolioInfo.getString("loyalty");
			if (!oldLoyalty.equals("") && !oldLoyalty.equals(currentLoyalty)) {
				invokeREST("GET", LOYALTY_SERVICE+"?owner="+owner+"&oldLoyalty="+oldLoyalty+"&currentLoyalty="+currentLoyalty);
			  }
		    }
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
	
		return portfolio;
	}
	
	public static JsonObject deletePortfolio(String owner) {
		JsonObject portfolio = null;

		try {
			portfolio = (JsonObject) invokeREST("DELETE", DELETE_PORTFOLIO_REST_SERVICE+owner);
		} catch (Throwable t) {
			t.printStackTrace();
		}

		return portfolio;
	}
	
	public static JsonStructure getStockQuote(String symbol) {
		JsonStructure quote = null;
		try {
		  quote = invokeREST("GET", QUOTE_SERVICE+"/"+symbol);
		} catch (IOException e) {
			System.out.println(e);
		}
		return quote;
	}

	private static JsonStructure invokeREST(String verb, String uri) throws IOException {
 		URL url = new URL(uri);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(verb);
		conn.setRequestProperty("Content-Type", "application/json");
		InputStream stream = conn.getInputStream();

		//JSONObject json = JSONObject.parse(stream); //JSON4J
		JsonStructure json = Json.createReader(stream).read();

		stream.close();

		return json; //I use JsonStructure here so I can return a JsonObject or a JsonArray
	}

	// Something bizarre is happening in both minikube and in CFC, where the http URL
	// is getting changed to an https one, but it still contains port 80 rather than 443.
	// This doesn't happen when running outside of Kube, or when running in Armada.
	// Might be an artifact of the Ingress proxy (my free Armada account doesn't support
	// Ingress, so I have to use a worker's nodePort instead).
	// TODO: Implementing an ugly hack for now; need to revisit (or open bug report on Ingress)
	static String getRedirectWorkaround(HttpServletRequest request) {
		String workaroundURL = "";

		String requestURL = request.getRequestURL().toString();
		//System.out.println(requestURL);
		if (requestURL.startsWith("https:") && requestURL.contains(":80/")) {
			//we got redirected from http to https, but the port number didn't get updated!
			workaroundURL = requestURL.replace(":80/", ":443/");

			//strip off the current servlet path - caller will append new path
			workaroundURL = workaroundURL.replace(request.getServletPath(), "/");

			//System.out.println("Correcting "+requestURL+" to "+workaroundURL);
		}
		return workaroundURL;
	}
	
	public static JsonObject extractFromQuandl(JsonObject obj, String symbol) {
		JsonObject dataset = (JsonObject) obj.get("dataset");
		JsonArray outerArray = (JsonArray) dataset.get("data");
		JsonArray array = (JsonArray) outerArray.get(0);

		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add("symbol", symbol.toUpperCase());
		builder.add("date", array.get(0)); //date is the first element
		builder.add("price", array.get(4)); //day closing value is the fifth element

		return builder.build();
	}
	
	public static String getData(String restURL)
	{
		URL url;
		String output="";
		try 
		{
			url = new URL(restURL);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Accept", "application/json");
			//Permission perm = con.getPermission();
			if (con.getResponseCode() != 200) 
			{
				System.out.println(con.getResponseCode());
			}
			ByteArrayOutputStream tempBuf = new ByteArrayOutputStream();
			InputStream is = con.getInputStream();
			  byte[] readBuf = new byte[4096];
			  int bytesRead = 0;
			  while((bytesRead = is.read(readBuf)) > -1)
			  {
				tempBuf.write(readBuf, 0, bytesRead);
			  }
			  output = tempBuf.toString();

			con.disconnect();
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output;
		
	}
}
