/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.commons.lang3.StringUtils;
import org.idb.cacao.api.utils.ParserUtils;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * Performs some tests with parser utilities
 * 
 * @author Rivelino Patrício
 * 
 * @since 24/11/2021
 *
 */
@RunWith(JUnitPlatform.class)
public class ParserTests {

	
	/**
	 * Test get month number given a month name
	 */
	@Test
	public void testMonthNameParser() throws Exception {
		
		String[][] allMonths = { {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"},
				{"JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"},
				{"ENE.", "FEB.", "MAR.", "ABR.", "MAY.", "JUN.", "JUL.", "AGO.", "SEPT.", "OCT.", "NOV.", "DIC."},
				{"ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO", "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"},
				{"JANV.", "FÉVR.", "MARS", "AVR.", "MAI", "JUIN", "JUIL.", "AOÛT", "SEPT.", "OCT.", "NOV.", "DÉC."},
				{"JANVIER", "FÉVRIER", "MARS", "AVRIL", "MAI", "JUIN", "JUILLET", "AOÛT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DÉCEMBRE"},
				{"JAN", "FEV", "MAR", "ABR", "MAI", "JUN", "JUL", "AGO", "SET", "OUT", "NOV", "DEZ"},
				{"JANEIRO", "FEVEREIRO", "MARÇO", "ABRIL", "MAIO", "JUNHO", "JULHO", "AGOSTO", "SETEMBRO", "OUTUBRO", "NOVEMBRO", "DEZEMBRO"} };
		
		for ( String[] months : allMonths ) {
			
			int i = 1;
			for ( String month : months ) { 
				Integer monthValue = ParserUtils.parseMonth(month);
				
				assertNotNull(monthValue,"Month can't be null/");
				assertEquals(i, monthValue.intValue(), "Month value is different from expected for month " + month);
				
				month = month.toLowerCase();
				monthValue = ParserUtils.parseMonth(month);
				
				assertNotNull(monthValue,"Month can't be null/");
				assertEquals(i, monthValue.intValue(), "Month value is different from expected for month " + month);
				
				month = StringUtils.capitalize(month);
				monthValue = ParserUtils.parseMonth(month);
				
				assertNotNull(monthValue,"Month can't be null/");
				assertEquals(i, monthValue.intValue(), "Month value is different from expected for month " + month);
				
				i++;
			}
			
		}
		
	}

	/**
	 * Test get year and month given a month name and year
	 */
	@Test
	public void testYearMonthParser() throws Exception {
		
		String[][] allMonths = { { "JAN/2021", "2021-01" },
								{ "FEB/2022", "2022-02" },		
								{ "MARÇO/2015", "2015-03" },
								{ "JANUARY/2021", "2021-01" },
								{ "FEBRUARY/2022", "2022-02" },
								{ "SEPTIEMBRE/2020", "2020-09" }, 
								{ "DÉC./2015", "2015-12" },
								{ "DÉCEMBRE/2019", "2019-12"} };
		
		for ( String[] months : allMonths ) {
			
			String yearMonth = ParserUtils.getYearMonth(months[0]);
			
			assertNotNull(yearMonth,"Month can't be null/");
			assertEquals(months[1], yearMonth, "Year/month value is different from expected for month " + months[0]);
			
		}
		
	}

}
