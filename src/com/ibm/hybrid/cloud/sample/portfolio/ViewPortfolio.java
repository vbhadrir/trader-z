/*
       Copyright 2017 IBM Corp All Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.ibm.hybrid.cloud.sample.portfolio;

import java.io.IOException;
import java.io.Writer;
import java.math.RoundingMode;
import java.text.NumberFormat;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class ViewPortfolio
 */
@WebServlet(description = "View Portfolio servlet", urlPatterns = { "/viewPortfolio" })
public class ViewPortfolio extends HttpServlet {
	private static final long serialVersionUID = 4815162342L;
	private static final double ERROR = -1;
	private static final String ERROR_STRING = "Error";
	private NumberFormat currency = null;
       
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ViewPortfolio() {
		super();

		currency = NumberFormat.getNumberInstance();
		currency.setMinimumFractionDigits(2);
		currency.setMaximumFractionDigits(2);
		currency.setRoundingMode(RoundingMode.HALF_UP);
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String owner = request.getParameter("owner");

		JsonObject portfolio = PortfolioServices.getPortfolio(owner);
		JsonObject individualPortflio = PortfolioServices.individualPorfolio(portfolio);
		JsonArray individualStockInfo = PortfolioServices.individualStockInfo(portfolio);
		double overallTotal = individualPortflio.getJsonNumber("total").doubleValue();
		String loyaltyLevel = individualPortflio.getString("loyalty");
		
		Writer writer = response.getWriter();
		writer.append("<!DOCTYPE html>");
		writer.append("<html>");
		writer.append("  <head>");
		writer.append("    <title>Stock Portfolio</title>");
		writer.append("    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
		writer.append("  </head>");
		writer.append("  <body>");
		writer.append("    <img src=\"header.jpg\" width=\"534\" height=\"200\"/>");
		writer.append("    <br/>");
		writer.append("    <form method=\"post\"/>");
		writer.append("      Stock Portfolio for "+owner+": <br/>");
		writer.append("      <br/>");
		writer.append("      <table border=\"1\" cellpadding=\"5\">");
		writer.append("        <tr>");
		writer.append("          <th>Symbol</th>");
		writer.append("          <th>Shares</th>");
		writer.append("          <th>Price</th>");
		writer.append("          <th>Date Quoted</th>");
		writer.append("          <th>Total</th>");
		writer.append("        </tr>");
		writer.append(getTableRows(individualStockInfo));
		writer.append("      </table>");
		writer.append("      <br/>");
		writer.append("      Total Portfolio Value: $"+currency.format(overallTotal)+" <br/>");
		writer.append("      <br/>");
		writer.append("      Loyalty Level: "+loyaltyLevel+" <br/>");
		writer.append("      <br/>");
		writer.append("      <input type=\"submit\" name=\"submit\" value=\"OK\" style=\"font-family: sans-serif; font-size: 16px;\">");
		writer.append("    </form>");
		writer.append("    <br/>");
		writer.append("    <a href=\"http://ibm.com/bluemix\">");
		writer.append("      <img src=\"footer.jpg\"/>");
		writer.append("    </a>");
		writer.append("  </body>");
		writer.append("</html>");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//In minikube and CFC, the port number is wrong for the https redirect.
		//This will fix that if needed - otherwise, it just returns an empty string
		//so that we can still use relative paths
		String prefix = PortfolioServices.getRedirectWorkaround(request);

		response.sendRedirect(prefix+"summary"); //send control to the Summary servlet
	}

	private String getTableRows(JsonArray stocks) {
		StringBuffer rows = new StringBuffer();

		for (int i = 0; i < stocks.size(); i++) {
			JsonObject obj = stocks.getJsonObject(i);
			String symbol = obj.getString("symbol");
			int shares = obj.getInt("shares");
			double price = obj.getJsonNumber("price").doubleValue();
			String date = obj.getString("dateQuoted");
			double total = obj.getJsonNumber("total").doubleValue();

			String formattedPrice = "$"+currency.format(price);
			String formattedTotal = "$"+currency.format(total);

			if (price == ERROR) {
				formattedPrice = ERROR_STRING;
				formattedTotal = ERROR_STRING;
			}

			rows.append("        <tr>");
			rows.append("          <td>"+symbol+"</td>");
			rows.append("          <td>"+shares+"</td>");
			rows.append("          <td>"+formattedPrice+"</td>");
			rows.append("          <td>"+date+"</td>");
			rows.append("          <td>"+formattedTotal+"</td>");
			rows.append("        </tr>");
		}

		return rows.toString();
	}
}
