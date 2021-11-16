package org.idb.cacao.validator;

import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.validator.fileformats.FileFormatFactory;
import org.idb.cacao.validator.utils.JSONUtils;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.FileReader;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests sample files in JSON format with the JSONParser implemented in VALIDATOR
 *
 * @author Leon Silva
 */
@RunWith(JUnitPlatform.class)
public class JSONParserTests {

    /**
     * Tests wether sample file '20211411 - Pauls Guitar Shop - Chart of Accounts.json' is JSON Valid
     */
    @Test
    void testChartOfAccountsJSONValid() throws Exception {
        Resource sampleFile = new ClassPathResource("/samples/20211411 - Pauls Guitar Shop - Chart of Accounts.json");
        assertTrue(sampleFile.exists());

        BufferedReader br = new BufferedReader(new FileReader(sampleFile.getFile()));

        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            String jsonContent = sb.toString();

            assertTrue(FileFormatFactory.getFileFormat(DocumentFormat.JSON).matchFilename(sampleFile.getFilename()));
            assertTrue(JSONUtils.isJSONValid(jsonContent));
        } finally {
            br.close();
        }

    }


}
