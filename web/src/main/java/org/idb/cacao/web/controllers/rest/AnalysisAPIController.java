/*******************************************************************************
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without 
 * restriction, including without limitation the rights to use, copy, modify, merge, publish, 
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or 
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN 
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.controllers.rest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.idb.cacao.api.utils.ParserUtils;
import org.idb.cacao.web.controllers.services.AnalysisService;
import org.idb.cacao.web.dto.AggregatedAccountingFlow;
import org.idb.cacao.web.dto.AnalysisData;
import org.idb.cacao.web.dto.StatementIncomeItem;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller class for all endpoints related to 'analysis' from tax payers
 * 
 * @author Rivelino Patrício
 *
 */
@RestController
@RequestMapping("/api")
@Tag(name="analysis-api-controller", description="Controller class for all endpoints related to 'TaxPayer' analysis interacting by a REST interface.")
public class AnalysisAPIController {

	private static final Logger log = Logger.getLogger(AnalysisAPIController.class.getName());
	
	private static final int VERTICAL = 1;
	private static final int HORIZONTAL = 2;
	private static final int BOTH = 3;

	@Autowired
	private AnalysisService analysisService;
	
	@Autowired
	private MessageSource messageSource;
	
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/analysis/vertical_horizontal_analysis"})
	public ResponseEntity<Object> getVerticalHorizontalAnalysis(@RequestParam("taxpayerId") String taxpayerId,
			@RequestParam("finalDate") String finalDate, @RequestParam("zeroBalance") String zeroBalance, 
			@RequestParam("comparisonPeriods") int comparisonPeriods) {
		
		if ( taxpayerId == null || taxpayerId.isEmpty() ) {
			log.log(Level.WARNING, "Missing parameter 'taxpayerId'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		if ( finalDate == null || finalDate.length() < 15 ) {
			log.log(Level.WARNING, "Missing or invalid parameter 'finalDate'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}

		YearMonth period = ParserUtils.parseDayMonthDayYear(finalDate);
		
		boolean fetchZeroBalance = ( zeroBalance == null ? false : "true".equalsIgnoreCase(zeroBalance) );
		
		List<YearMonth> additionalPeriods = getAdditionalPeriods(period,comparisonPeriods);
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	List<Map<String,Object>> mapOfAccounts = analysisService.getMapOfAccounts(
    			taxpayerId,period,fetchZeroBalance,additionalPeriods);
	
    	return ResponseEntity.ok().body(mapOfAccounts);    	
	}

	/**
	 * Given a month and information about comparison periods, creates and returns a {@link List}
	 * of {@link YearMonth} 	 
	 * @param basePeriod	The base period
	 * @param comparisonPeriods	The compatison periods information
	 * @return	A {@link List} of {@link YearMonth}
	 */
	private List<YearMonth> getAdditionalPeriods(YearMonth basePeriod, int comparisonPeriods) {
		
		List<YearMonth> periods = new LinkedList<>();
		
		if( comparisonPeriods < 10 ) { //Months before
			
			int numMonths = 1;
			switch (comparisonPeriods) {
			case 2:
				numMonths = 3;
				break;
			case 3:
				numMonths = 6;
				break;
			case 4:
				numMonths = 12;
				break;
			default:
				break;
			}			
			
			for ( int i = 1; i <= numMonths; i++ ) {
				YearMonth period = basePeriod.minusMonths(i);
				periods.add(period);	
			}			
			
		}
		
		else if( comparisonPeriods > 10 && comparisonPeriods < 20) { //Years before			
			
			for ( int i = 1; i <= (comparisonPeriods-10); i++ ) {
				YearMonth period = basePeriod.minusYears(i);
				periods.add(period);	
			}	
			
		}
		
		else if( comparisonPeriods > 20 && comparisonPeriods < 30 ) { //Months after
			
			int numMonths = 1;
			switch (comparisonPeriods) {
			case 22:
				numMonths = 3;
				break;
			case 23:
				numMonths = 6;
				break;
			case 24:
				numMonths = 12;
				break;
			default:
				break;
			}			
			
			for ( int i = 1; i <= numMonths; i++ ) {
				YearMonth period = basePeriod.plusMonths(i);
				periods.add(period);	
			}	
			
		}
		
		else if( comparisonPeriods > 30 ) { //Years after
			for ( int i = 1; i <= (comparisonPeriods-30); i++ ) {
				YearMonth period = basePeriod.plusYears(i);
				periods.add(period);	
			}	
		}
		
		return periods;
	}
	
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/analysis/vertical_horizontal_view_columns"})
	public ResponseEntity<Object> getAnalysisViewColumns(@RequestParam("finalDate") String finalDate, 
			@RequestParam("comparisonPeriods") int comparisonPeriods, 
			@RequestParam("analysisType") int analysisType ) {
		
		if ( finalDate == null || finalDate.length() < 15 ) {
			log.log(Level.WARNING, "Missing or invalid parameter 'finalDate'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}

		YearMonth period = ParserUtils.parseDayMonthDayYear(finalDate);

		List<YearMonth> allPeriods = getAdditionalPeriods(period, comparisonPeriods);
		//allPeriods.sort(null);						
		
		String horizontal = " " + messageSource.getMessage("horizontal", null, LocaleContextHolder.getLocale()) + "%";
		String vertical = " " + messageSource.getMessage("vertical", null, LocaleContextHolder.getLocale()) + "%";
		
		allPeriods.add(0,period);
		
		String decimalChar = messageSource.getMessage("decimal.char", null, LocaleContextHolder.getLocale());
		String decimalGroupSeparator = messageSource.getMessage("decimal.grouping.separator", null, LocaleContextHolder.getLocale());
		String monthFormat = messageSource.getMessage("month.format", null, LocaleContextHolder.getLocale());
		DateTimeFormatter simpleFormat = DateTimeFormatter.ofPattern(monthFormat);
		
		List<String[]> columns = new LinkedList<>();
		int i = 0;
		for ( YearMonth p : allPeriods ) {		
			
			String title = StringUtils.capitalize(simpleFormat.format(p));			
			String field = "B" + i;
			String[] data = new String[] { title, field, "right", "false", "money", decimalChar, decimalGroupSeparator, "$", "true", "0" };
			columns.add(data);
			
			if ( analysisType == VERTICAL || analysisType == BOTH ) { //Vertical OR both
				title += vertical;
				field = "V" + i;
				data = new String[] { title, field, "right", "false", "money", decimalChar, decimalGroupSeparator, "%", "true", "2" };
				columns.add(data);	
			}
			
			if ( i > 0 && ( analysisType == HORIZONTAL || analysisType == BOTH ) ) { //Horizontal OR both
				title += horizontal;
				field = "H" + i;
				data = new String[] { title, field, "right", "false", "money", decimalChar, decimalGroupSeparator, "%", "true", "2" };
				columns.add(data);	
			}			
			
			i++;

		}
		
    	return ResponseEntity.ok().body(columns);    	
    	
	}

	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/analysis/general_analysis"})
	public ResponseEntity<Object> getGeneralAnalysis(@RequestParam("qualifier") String qualifier,
			@RequestParam("qualifierValue") String qualifierValue,
			@RequestParam("sourceData") int sourceData,
			@RequestParam("year") String year) {
		
		if ( qualifier == null || qualifier.isEmpty() ) {
			log.log(Level.WARNING, "Missing parameter 'qualifier'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		if ( qualifierValue == null || qualifierValue.isEmpty() ) {
			log.log(Level.WARNING, "Missing parameter 'qualifierValue'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		if ( sourceData == 0 ) {
			log.log(Level.WARNING, "Missing parameter 'sourceData'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		if ( year == null || year.isEmpty() || Integer.valueOf(year) == 0 ) {
			log.log(Level.WARNING, "Missing or invalid parameter 'year'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	AnalysisData analysisData = analysisService.getGeneralAnalysisValues(qualifier, qualifierValue, sourceData, Integer.valueOf(year));
	
    	return ResponseEntity.ok().body(analysisData);    	
	}
	
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/analysis/qualifiers"})
	public ResponseEntity<Object> getQualifierValues(@RequestParam("qualifier") String qualifier) {
		
		if ( qualifier == null || qualifier.isEmpty() ) {
			log.log(Level.WARNING, "Missing parameter 'qualifier'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	List<String> values = analysisService.getQualifierValues(qualifier);
    	
    	return ResponseEntity.ok().body(values);    
		
	}
	
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/analysis/years"})
	public ResponseEntity<Object> getYears(@RequestParam("sourceData") int sourceData) {
		
		if ( sourceData == 0 ) {
			log.log(Level.WARNING, "Missing parameter 'sourceData'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}		
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	List<Integer> values = analysisService.getYears(sourceData);
    	
    	return ResponseEntity.ok().body(values);    
		
	}	
	
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/analysis/accounting_flows"})
	public ResponseEntity<Object> getAccountingFlows(@RequestParam("taxpayerId") String taxpayerId,
			@RequestParam("startDate") @DateTimeFormat(iso = ISO.DATE) LocalDate startDate, 
			@RequestParam("finalDate") @DateTimeFormat(iso = ISO.DATE) LocalDate finalDate) {
		
		if ( taxpayerId == null || taxpayerId.isEmpty() ) {
			log.log(Level.WARNING, "Missing parameter 'taxpayerId'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	List<AggregatedAccountingFlow> values = analysisService.getAccountingFlow(taxpayerId, 
    			Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
    			Date.from(finalDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()));
    	
    	return ResponseEntity.ok().body(values);    
		
	}
	
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/analysis/statement_income_analysis"})
	public ResponseEntity<Object> getStatementIncomeAnalysis(@RequestParam("taxpayerId") String taxpayerId,
			@RequestParam("year") String year ) {
		
		if ( taxpayerId == null || taxpayerId.isEmpty() ) {
			log.log(Level.WARNING, "Missing parameter 'taxpayerId'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		if ( year == null || year.isEmpty() || Integer.valueOf(year) == 0 ) {
			log.log(Level.WARNING, "Missing or invalid parameter 'year'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	List<StatementIncomeItem> statements = analysisService.getStatementIncomeDeclaredAndCalculated(
    			taxpayerId,Integer.valueOf(year));
	
    	return ResponseEntity.ok().body(statements);    	
	}
	
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/analysis/taxpayer_general_view"})
	public ResponseEntity<Object> getTaxpayerGeneralView(
			@RequestParam("searchType") int searchType,
			@RequestParam("taxpayerId") String taxpayerId,
			@RequestParam("year") String year ) {
		
		if ( taxpayerId == null || taxpayerId.isEmpty() ) {
			log.log(Level.WARNING, "Missing parameter 'taxpayerId'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		if ( year == null || year.isEmpty() || Integer.valueOf(year) == 0 ) {
			log.log(Level.WARNING, "Missing or invalid parameter 'year'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		if ( searchType == 0 ) {
			searchType = AnalysisService.SEARCH_SHAREHOLDINGS;
		}
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	List<?> result = analysisService.getTaxpayerData(taxpayerId,Integer.valueOf(year),searchType);
    	
    	if ( result == null || result.isEmpty() )  
    		result = Collections.emptyList();
    	
    	return ResponseEntity.ok().body(result);    	
	}		
}
